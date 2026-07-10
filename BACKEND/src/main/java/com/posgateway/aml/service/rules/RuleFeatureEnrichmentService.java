package com.posgateway.aml.service.rules;

import com.posgateway.aml.entity.TransactionEntity;
import com.posgateway.aml.entity.chargeback.ChargebackDispute;
import com.posgateway.aml.entity.merchant.Merchant;
import com.posgateway.aml.repository.MerchantRepository;
import com.posgateway.aml.repository.TransactionRepository;
import com.posgateway.aml.repository.chargeback.ChargebackDisputeRepository;
import com.posgateway.aml.repository.MerchantScreeningResultRepository;
import com.posgateway.aml.repository.risk.HighRiskCountryRepository;
import com.posgateway.aml.service.cache.FeatureCacheService;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Populates the feature map consumed by SpEL rules in {@code rule_definitions}.
 * Keys align with expressions seeded in V143.
 */
@Service
public class RuleFeatureEnrichmentService {

    private static final Logger logger = LoggerFactory.getLogger(RuleFeatureEnrichmentService.class);

    private final TransactionRepository transactionRepository;
    private final MerchantRepository merchantRepository;
    private final HighRiskCountryRepository highRiskCountryRepository;
    private final ChargebackDisputeRepository chargebackDisputeRepository;
    private final FeatureCacheService featureCacheService;
    private final MerchantScreeningResultRepository merchantScreeningResultRepository;
    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    public RuleFeatureEnrichmentService(TransactionRepository transactionRepository,
                                        MerchantRepository merchantRepository,
                                        HighRiskCountryRepository highRiskCountryRepository,
                                        ChargebackDisputeRepository chargebackDisputeRepository,
                                        FeatureCacheService featureCacheService,
                                        MerchantScreeningResultRepository merchantScreeningResultRepository) {
        this.transactionRepository = transactionRepository;
        this.merchantRepository = merchantRepository;
        this.highRiskCountryRepository = highRiskCountryRepository;
        this.chargebackDisputeRepository = chargebackDisputeRepository;
        this.featureCacheService = featureCacheService;
        this.merchantScreeningResultRepository = merchantScreeningResultRepository;
    }

    public void enrich(TransactionEntity transaction, Map<String, Object> features) {
        if (transaction == null || features == null) {
            return;
        }

        LocalDateTime now = transaction.getTxnTs() != null ? transaction.getTxnTs() : LocalDateTime.now();
        String merchantId = transaction.getMerchantId();
        String panHash = transaction.getPanHash();
        Long amountCents = transaction.getAmountCents() != null ? transaction.getAmountCents() : 0L;
        double amountUnits = amountCents / 100.0;

        features.put("amount", amountUnits);
        features.put("amount_cents", amountCents);
        features.put("currency", transaction.getCurrency() != null ? transaction.getCurrency() : "USD");
        features.put("merchant_id", merchantId);
        features.put("pan_hash", panHash);
        features.put("direction", normalizeDirection(transaction.getDirection()));
        features.put("country_code", resolveCountryCode(transaction, features));
        features.put("origin_country", features.get("country_code"));
        features.put("destination_country", transaction.getMerchantCountry());
        features.put("channel", transaction.getChannelType() != null ? transaction.getChannelType() : "POS");

        if (transaction.getKrs() != null) features.put("krs_score", transaction.getKrs());
        if (transaction.getTrs() != null) features.put("trs_score", transaction.getTrs());
        if (transaction.getCra() != null) features.put("cra_score", transaction.getCra());

        enrichVelocityAndHistory(transaction, features, now, merchantId, panHash, amountCents);
        enrichAdvancedVelocityRatios(transaction, features, now, panHash, merchantId);
        enrichStructuringSignals(transaction, features, now, panHash, amountCents);
        enrichFanInFanOut(transaction, features, now, panHash, merchantId);
        enrichRoundValueMetrics(features, panHash, now);
        enrichChargebackMetrics(transaction, features, merchantId);
        enrichBlacklistHits(transaction, features);
        enrichMerchantProfile(transaction, features, merchantId);
        enrichMerchantScreeningHits(features, merchantId);
        enrichIpMetrics(transaction, features, now);
        enrichNameSimilarity(features, panHash, merchantId);
    }

    private void enrichVelocityAndHistory(TransactionEntity transaction, Map<String, Object> features,
                                          LocalDateTime now, String merchantId, String panHash, Long amountCents) {
        LocalDateTime oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        LocalDateTime oneDayAgo = now.minus(24, ChronoUnit.HOURS);
        LocalDateTime sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        LocalDateTime thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        if (panHash != null) {
            Long panCount1h = transactionRepository.countByPanInTimeWindow(panHash, oneHourAgo, now);
            features.putIfAbsent("pan_txn_count_1h", panCount1h != null ? panCount1h : 0L);

            Long panCount24h = transactionRepository.countByPanInTimeWindow(panHash, oneDayAgo, now);
            features.put("pan_txn_count_24h", panCount24h != null ? panCount24h : 0L);

            Long panSum24h = transactionRepository.sumAmountByPanInTimeWindow(panHash, oneDayAgo, now);
            features.put("pan_amount_sum_24h", panSum24h != null ? panSum24h / 100.0 : 0.0);

            Long panCountAll = transactionRepository.countByPanInTimeWindow(
                    panHash, now.minusYears(10), now);
            features.put("is_first_transaction", panCountAll != null && panCountAll <= 1);

            LocalDateTime lastTxn = transactionRepository.findLastTransactionTimeByPan(panHash);
            if (lastTxn != null && transaction.getTxnTs() != null
                    && lastTxn.isBefore(transaction.getTxnTs().minusSeconds(1))) {
                long daysSince = ChronoUnit.DAYS.between(lastTxn, now);
                features.put("days_since_last_activity", Math.max(0, daysSince));
            } else if (panCountAll != null && panCountAll <= 1) {
                features.put("days_since_last_activity", 0L);
            } else {
                features.put("days_since_last_activity", 0L);
            }

            String countryCode = (String) features.get("country_code");
            if (countryCode != null) {
                long priorCountry = countPriorWithCountry(panHash, countryCode, transaction.getTxnId(), thirtyDaysAgo, now);
                features.put("is_new_country", priorCountry == 0);
            }

            String currency = transaction.getCurrency();
            if (currency != null) {
                long priorCurrency = countPriorWithCurrency(panHash, currency, transaction.getTxnId(), thirtyDaysAgo, now);
                features.put("is_new_currency", priorCurrency == 0);
            }

            Long prior7d = transactionRepository.countByPanInTimeWindow(panHash, sevenDaysAgo, now);
            Long prior30d = transactionRepository.countByPanInTimeWindow(panHash, thirtyDaysAgo, now);
            if (prior30d != null && prior30d > 5 && prior7d != null) {
                double baselineDaily = prior30d / 30.0;
                double recentDaily = prior7d / 7.0;
                if (baselineDaily > 0) {
                    features.put("txn_count_deviation_ratio", recentDaily / baselineDaily);
                }
            }

            Long sum7d = transactionRepository.sumAmountByPanInTimeWindow(panHash, sevenDaysAgo, now);
            Long sum30d = transactionRepository.sumAmountByPanInTimeWindow(panHash, thirtyDaysAgo, now);
            if (sum30d != null && sum30d > 0 && sum7d != null) {
                double baselineVol = sum30d / 30.0;
                double recentVol = sum7d / 7.0;
                if (baselineVol > 0) {
                    features.put("volume_deviation_ratio", recentVol / baselineVol);
                }
            }

            Double avg30d = transactionRepository.avgAmountByPanInTimeWindow(panHash, thirtyDaysAgo, now);
            if (avg30d != null && avg30d > 0) {
                double spike = amountCents / avg30d;
                features.put("avg_amount_spike_ratio", spike);
            }
        }

        if (merchantId != null) {
            Long merchantPrior = transactionRepository.countByMerchantInTimeWindow(merchantId, thirtyDaysAgo, now);
            features.putIfAbsent("merchant_txn_count_30d", merchantPrior != null ? merchantPrior : 0L);

            Long merchantCount24h = transactionRepository.countByMerchantInTimeWindow(merchantId, oneDayAgo, now);
            features.put("merchant_txn_count_24h", merchantCount24h != null ? merchantCount24h : 0L);

            Long merchantSum24h = transactionRepository.sumAmountByMerchantInTimeWindow(merchantId, oneDayAgo, now);
            features.put("merchant_amount_sum_24h", merchantSum24h != null ? merchantSum24h / 100.0 : 0.0);
        }
    }

    private void enrichAdvancedVelocityRatios(TransactionEntity transaction, Map<String, Object> features,
                                                LocalDateTime now, String panHash, String merchantId) {
        if (panHash == null) {
            return;
        }
        LocalDateTime oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        LocalDateTime oneDayAgo = now.minus(24, ChronoUnit.HOURS);
        LocalDateTime twoDaysAgo = now.minus(48, ChronoUnit.HOURS);
        LocalDateTime sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);

        Long sum1h = transactionRepository.sumAmountByPanInTimeWindow(panHash, oneHourAgo, now);
        Long sum24h = transactionRepository.sumAmountByPanInTimeWindow(panHash, oneDayAgo, now);
        if (sum24h != null && sum24h > 0 && sum1h != null) {
            double prior23h = (sum24h - sum1h) / 23.0;
            if (prior23h > 0) {
                features.put("volume_ratio_t1_t2", (sum1h / 100.0) / prior23h);
            }
        }

        Long count1h = transactionRepository.countByPanInTimeWindow(panHash, oneHourAgo, now);
        Long count24h = transactionRepository.countByPanInTimeWindow(panHash, oneDayAgo, now);
        if (count24h != null && count24h > 0 && count1h != null) {
            double dailyAvg = count24h / 24.0;
            if (dailyAvg > 0) {
                features.put("txn_count_window_ratio", count1h / dailyAvg);
            }
            features.put("daily_txn_count_ratio", count24h / Math.max(1.0, dailyAvg * 24));
        }

        Long countYesterday = transactionRepository.countByPanInTimeWindow(panHash, twoDaysAgo, oneDayAgo);
        Long countToday = transactionRepository.countByPanInTimeWindow(panHash, oneDayAgo, now);
        if (countYesterday != null && countYesterday > 0 && countToday != null) {
            features.put("daily_txn_count_ratio", (double) countToday / countYesterday);
        }

        Double avg7d = transactionRepository.avgAmountByPanInTimeWindow(panHash, sevenDaysAgo, now);
        Double avg1d = transactionRepository.avgAmountByPanInTimeWindow(panHash, oneDayAgo, now);
        if (avg7d != null && avg7d > 0 && avg1d != null) {
            features.put("avg_amount_window_ratio", avg1d / avg7d);
            features.put("daily_avg_amount_ratio", avg1d / avg7d);
        }

        Long inbound = transactionRepository.countInboundByPanInWindow(panHash, sevenDaysAgo, now);
        Long outbound = transactionRepository.countOutboundByPanInWindow(panHash, sevenDaysAgo, now);
        if (inbound != null && inbound > 0 && outbound != null) {
            features.put("send_receive_ratio", (double) outbound / inbound);
        }
        Long inAmt = transactionRepository.sumInboundAmountByPanInWindow(panHash, oneDayAgo, now);
        Long outAmt = transactionRepository.sumOutboundAmountByPanInWindow(panHash, oneDayAgo, now);
        if (inAmt != null && inAmt > 0 && outAmt != null) {
            features.put("spend_receive_spike_ratio", (double) outAmt / inAmt);
        }

        if (merchantId != null && panHash != null) {
            Long samePartyVol = transactionRepository.sumAmountByMerchantAndPanInWindow(
                    merchantId, panHash, sevenDaysAgo, now);
            if (samePartyVol != null) {
                features.put("same_parties_volume", samePartyVol / 100.0);
            }
        }

        List<TransactionEntity> recent = transactionRepository.findRecentByPanHash(panHash,
                org.springframework.data.domain.PageRequest.of(0, 100));
        long circular = recent.stream()
                .filter(t -> t.getTxnTs() != null && !t.getTxnTs().isBefore(sevenDaysAgo))
                .map(TransactionEntity::getMerchantId)
                .filter(m -> m != null)
                .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
                .values().stream()
                .filter(c -> c >= 2)
                .count();
        features.put("circular_trading_count", circular);

        long refundCount = recent.stream()
                .filter(t -> t.getTxnTs() != null && !t.getTxnTs().isBefore(sevenDaysAgo))
                .filter(t -> t.getDirection() != null
                        && (t.getDirection().toUpperCase().contains("IN")
                        || t.getDirection().toUpperCase().contains("CREDIT")
                        || t.getDirection().toUpperCase().contains("REFUND")))
                .count();
        long windowSize = recent.stream()
                .filter(t -> t.getTxnTs() != null && !t.getTxnTs().isBefore(sevenDaysAgo))
                .count();
        if (windowSize > 0) {
            features.put("refund_share_pct", (refundCount * 100.0) / windowSize);
        }
    }

    private void enrichStructuringSignals(TransactionEntity transaction, Map<String, Object> features,
                                          LocalDateTime now, String panHash, Long amountCents) {
        if (panHash == null || amountCents == null) {
            return;
        }
        double amount = amountCents / 100.0;
        double threshold = 9000.0;
        boolean justBelow = amount >= threshold * 0.9 && amount < threshold;
        features.put("structuring_amount_band", justBelow);

        if (justBelow) {
            LocalDateTime dayAgo = now.minus(24, ChronoUnit.HOURS);
            Long repeats = transactionRepository.countByAccountAndAmountRangeAndPeriod(
                    panHash,
                    java.math.BigDecimal.valueOf(threshold * 0.9),
                    java.math.BigDecimal.valueOf(threshold),
                    dayAgo, now);
            features.put("structuring_repeat_count_24h", repeats != null ? repeats : 0L);
            features.put("is_structuring_suspected", repeats != null && repeats >= 3);
        } else {
            features.put("structuring_repeat_count_24h", 0L);
            features.put("is_structuring_suspected", false);
        }
    }

    private void enrichFanInFanOut(TransactionEntity transaction, Map<String, Object> features,
                                   LocalDateTime now, String panHash, String merchantId) {
        if (panHash == null) {
            return;
        }
        LocalDateTime sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);

        Long uniqueMerchants = transactionRepository.countDistinctMerchantsByPanInWindow(panHash, sevenDaysAgo, now);
        features.put("unique_senders", uniqueMerchants != null ? uniqueMerchants : 0L);
        features.put("unique_counterparties", uniqueMerchants != null ? uniqueMerchants : 0L);

        Long distinctBanks = transactionRepository.countDistinctCardBrandsByPanInWindow(panHash, sevenDaysAgo, now);
        features.put("distinct_banks", distinctBanks != null ? distinctBanks : 0L);

        Long distinctDevices = transactionRepository.countDistinctDevicesByPanInWindow(panHash, sevenDaysAgo, now);
        features.put("distinct_payment_ids", distinctDevices != null ? distinctDevices : 0L);

        String deviceFp = transaction.getDeviceFingerprint();
        if (deviceFp != null) {
            List<String> merchants = transactionRepository.findMerchantIdsByDeviceFingerprint(deviceFp);
            features.put("distinct_users_per_payment_id", merchants != null ? merchants.size() : 0);
        }

        Set<String> merchantNames = new HashSet<>();
        if (merchantId != null) {
            try {
                Long mid = Long.parseLong(merchantId);
                merchantRepository.findByMerchantId(mid)
                        .ifPresent(m -> merchantNames.add(normalizeName(m.getLegalName())));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        List<TransactionEntity> recent = transactionRepository.findRecentByPanHash(panHash,
                org.springframework.data.domain.PageRequest.of(0, 30));
        for (TransactionEntity t : recent) {
            if (t.getMerchantId() != null) {
                try {
                    merchantRepository.findByMerchantId(Long.parseLong(t.getMerchantId()))
                            .ifPresent(m -> merchantNames.add(normalizeName(m.getLegalName())));
                } catch (NumberFormatException ignored) {
                    // skip non-numeric merchant ids
                }
            }
        }
        features.put("distinct_account_holder_names", merchantNames.size());
        features.put("address_changes", Math.max(0, merchantNames.size() - 1));
        features.put("bank_name_changes", Math.max(0, (distinctBanks != null ? distinctBanks.intValue() : 1) - 1));
    }

    private void enrichMerchantScreeningHits(Map<String, Object> features, String merchantId) {
        if (merchantId == null) {
            return;
        }
        try {
            Long mid = Long.parseLong(merchantId);
            merchantScreeningResultRepository.findLatestByMerchantId(mid).ifPresent(msr -> {
                boolean hit = msr.getMatchCount() != null && msr.getMatchCount() > 0
                        || "MATCH".equalsIgnoreCase(msr.getScreeningStatus())
                        || "POTENTIAL_MATCH".equalsIgnoreCase(msr.getScreeningStatus());
                if (hit) {
                    features.put("entity_screening_hit", true);
                    features.put("counterparty_screening_hit", true);
                    String type = msr.getScreeningType() != null ? msr.getScreeningType().toUpperCase() : "";
                    if (type.contains("SANCTION")) {
                        features.put("sanctions_hit", true);
                    }
                    if (type.contains("PEP")) {
                        features.put("pep_hit", true);
                    }
                    if (type.contains("ADVERSE")) {
                        features.put("adverse_media_hit", true);
                    }
                    if (type.contains("BANK")) {
                        features.put("bank_name_screening_hit", true);
                    }
                }
            });
        } catch (NumberFormatException e) {
            logger.debug("Non-numeric merchantId for screening enrichment: {}", merchantId);
        }
    }

    private void enrichNameSimilarity(Map<String, Object> features, String panHash, String merchantId) {
        if (panHash == null) {
            return;
        }
        List<String> names = new ArrayList<>();
        List<TransactionEntity> recent = transactionRepository.findRecentByPanHash(panHash,
                org.springframework.data.domain.PageRequest.of(0, 20));
        for (TransactionEntity t : recent) {
            if (t.getMerchantId() == null) {
                continue;
            }
            try {
                merchantRepository.findByMerchantId(Long.parseLong(t.getMerchantId()))
                        .map(Merchant::getLegalName)
                        .filter(n -> n != null && !n.isBlank())
                        .ifPresent(names::add);
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        boolean exceeded = false;
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                int dist = levenshteinDistance.apply(normalizeName(names.get(i)), normalizeName(names.get(j)));
                if (dist > 0 && dist <= 2) {
                    exceeded = true;
                    break;
                }
            }
            if (exceeded) {
                break;
            }
        }
        features.put("account_name_levenshtein_exceeded", exceeded);
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name.trim().toUpperCase().replaceAll("\\s+", " ");
    }

    private long countPriorWithCountry(String panHash, String countryCode, Long excludeTxnId,
                                       LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findRecentByPanHash(panHash,
                        org.springframework.data.domain.PageRequest.of(0, 200))
                .stream()
                .filter(t -> excludeTxnId == null || !excludeTxnId.equals(t.getTxnId()))
                .filter(t -> t.getTxnTs() != null && !t.getTxnTs().isBefore(from) && !t.getTxnTs().isAfter(to))
                .filter(t -> countryCode.equals(t.getMerchantCountry()))
                .count();
    }

    private long countPriorWithCurrency(String panHash, String currency, Long excludeTxnId,
                                        LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findRecentByPanHash(panHash,
                        org.springframework.data.domain.PageRequest.of(0, 200))
                .stream()
                .filter(t -> excludeTxnId == null || !excludeTxnId.equals(t.getTxnId()))
                .filter(t -> t.getTxnTs() != null && !t.getTxnTs().isBefore(from) && !t.getTxnTs().isAfter(to))
                .filter(t -> currency.equals(t.getCurrency()))
                .count();
    }

    private void enrichRoundValueMetrics(Map<String, Object> features, String panHash, LocalDateTime now) {
        if (panHash == null) {
            return;
        }
        LocalDateTime sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        List<TransactionEntity> recent = transactionRepository.findRecentByPanHash(panHash,
                org.springframework.data.domain.PageRequest.of(0, 50));
        List<TransactionEntity> window = recent.stream()
                .filter(t -> t.getTxnTs() != null && !t.getTxnTs().isBefore(sevenDaysAgo))
                .toList();
        if (window.isEmpty()) {
            return;
        }
        long roundCount = window.stream().filter(this::isRoundAmount).count();
        double share = (roundCount * 100.0) / window.size();
        features.put("round_value_share_pct", share);
        features.put("round_value_txn_count", roundCount);

        Long amountCents = window.get(0).getAmountCents();
        if (amountCents != null) {
            String cents = String.valueOf(Math.abs(amountCents % 100));
            features.put("amount_ending_pattern", cents.endsWith("99") || cents.equals("0"));
        }
    }

    private boolean isRoundAmount(TransactionEntity t) {
        Long cents = t.getAmountCents();
        return cents != null && cents % 100 == 0;
    }

    private void enrichChargebackMetrics(TransactionEntity transaction, Map<String, Object> features, String merchantId) {
        if (merchantId == null) {
            return;
        }
        try {
            Long mid = Long.parseLong(merchantId);
            List<ChargebackDispute> disputes = chargebackDisputeRepository.findByMerchantIdOrderByCreatedAtDesc(mid);
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
            long cb30d = disputes.stream()
                    .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().isAfter(thirtyDaysAgo))
                    .count();
            features.put("merchant_chargeback_count_30d", cb30d);

            Long txnCount30d = transactionRepository.countByMerchantInTimeWindow(
                    merchantId, thirtyDaysAgo, LocalDateTime.now());
            if (txnCount30d != null && txnCount30d > 0) {
                features.put("merchant_chargeback_ratio", (double) cb30d / txnCount30d);
            }

            Optional<ChargebackDispute> linked = disputes.stream()
                    .filter(d -> transaction.getTxnId() != null
                            && d.getPspTransactionId() != null
                            && d.getPspTransactionId().equals(String.valueOf(transaction.getTxnId())))
                    .findFirst();
            linked.ifPresent(d -> {
                features.put("is_chargeback", true);
                features.put("dispute_reason_code", d.getReasonCode());
                features.put("dispute_reason_category", d.getReasonCategory());
                features.put("rdr_status", d.getRdrStatus());
                features.put("rdr_prevention_match", "accepted".equalsIgnoreCase(d.getRdrStatus()));
            });
        } catch (NumberFormatException e) {
            logger.debug("Non-numeric merchantId for chargeback enrichment: {}", merchantId);
        }
    }

    private void enrichBlacklistHits(TransactionEntity transaction, Map<String, Object> features) {
        String panHash = transaction.getPanHash();
        String deviceFp = transaction.getDeviceFingerprint();
        String walletId = transaction.getDeviceFingerprint();
        String cardCountry = transaction.getMerchantCountry();
        String reference = transaction.getIsoMsg();

        features.put("variable_blacklist_hit",
                (panHash != null && featureCacheService.isBlacklisted("pan", panHash))
                        || (deviceFp != null && featureCacheService.isBlacklisted("device", deviceFp)));
        features.put("payment_details_blacklist",
                deviceFp != null && featureCacheService.isBlacklisted("payment_details", deviceFp));
        features.put("wallet_blacklist_hit",
                walletId != null && featureCacheService.isBlacklisted("wallet", walletId));
        features.put("card_issuer_country_blacklist",
                cardCountry != null && featureCacheService.isBlacklisted("card_country", cardCountry));
        features.put("reference_keyword_blacklist", containsBlacklistedKeyword(reference));
        features.put("anonymous_payment_screening_hit", false);
    }

    private boolean containsBlacklistedKeyword(String reference) {
        if (reference == null || reference.isBlank()) {
            return false;
        }
        String lower = reference.toLowerCase();
        for (String keyword : List.of("casino", "crypto", "gambling", "offshore", "shell")) {
            if (featureCacheService.isBlacklisted("reference_keyword", keyword) && lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private void enrichMerchantProfile(TransactionEntity transaction, Map<String, Object> features, String merchantId) {
        if (merchantId == null) {
            return;
        }
        try {
            Long mid = Long.parseLong(merchantId);
            merchantRepository.findById(mid).ifPresent(merchant -> enrichFromMerchant(merchant, features));
        } catch (NumberFormatException e) {
            merchantRepository.findByMerchantId(Long.valueOf(merchantId))
                    .ifPresent(merchant -> enrichFromMerchant(merchant, features));
        }
    }

    private void enrichFromMerchant(Merchant merchant, Map<String, Object> features) {
        String country = merchant.getCountry();
        if (country != null) {
            boolean highRisk = highRiskCountryRepository.findByCountryCode(country.toUpperCase()).isPresent();
            features.put("onboarding_high_risk_country", highRisk);
            features.put("ip_high_risk_country", highRisk);
        }
        boolean sanctionsHit = merchant.getStatus() != null
                && merchant.getStatus().toUpperCase().contains("SANCTION");
        features.put("sanctions_hit", sanctionsHit);
        features.put("pep_hit", merchant.isPep());
        features.put("entity_screening_hit", sanctionsHit || merchant.isPep());
        features.put("bank_name_screening_hit", false);
        features.put("counterparty_screening_hit", false);
        features.put("anonymous_payment_screening_hit", false);
        features.put("adverse_media_hit", false);
    }

    private void enrichIpMetrics(TransactionEntity transaction, Map<String, Object> features, LocalDateTime now) {
        String ip = transaction.getIpAddress();
        if (ip == null || ip.isBlank()) {
            return;
        }
        LocalDateTime sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        List<String> merchants = transactionRepository.findMerchantIdsByIpAddress(ip);
        features.put("distinct_users_per_ip", merchants != null ? merchants.size() : 0);

        Long ipTxnCount = transactionRepository.countByIpAddressSince(ip, sevenDaysAgo, now);
        features.put("payment_id_use_count", ipTxnCount != null ? ipTxnCount : 0L);

        Set<String> countries = merchants.stream()
                .map(this::resolveMerchantCountry)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.toSet());
        features.put("distinct_countries", countries.size());
        features.put("ip_outside_expected_location", countries.size() > 2);
        features.put("ip_change_count", countries.size());
    }

    private String resolveCountryCode(TransactionEntity transaction, Map<String, Object> features) {
        if (transaction.getMerchantCountry() != null) {
            return transaction.getMerchantCountry();
        }
        Object fromFeatures = features.get("country_code");
        if (fromFeatures != null) {
            return fromFeatures.toString();
        }
        return "UNK";
    }

    private String resolveMerchantCountry(String merchantIdStr) {
        try {
            Long mid = Long.parseLong(merchantIdStr);
            return merchantRepository.findByMerchantId(mid).map(Merchant::getCountry).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return "OUTBOUND";
        }
        String upper = direction.toUpperCase();
        if (upper.startsWith("IN") || "CREDIT".equals(upper)) {
            return "INBOUND";
        }
        return "OUTBOUND";
    }
}
