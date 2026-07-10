package com.posgateway.aml.service.limits;

import com.posgateway.aml.entity.TransactionEntity;
import com.posgateway.aml.entity.limits.MerchantTransactionLimit;
import com.posgateway.aml.repository.TransactionRepository;
import com.posgateway.aml.repository.limits.MerchantTransactionLimitRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enforces per-merchant transaction limits configured in {@code merchant_transaction_limits}.
 */
@Service
public class TransactionLimitEnforcementService {

    private final MerchantTransactionLimitRepository merchantLimitRepository;
    private final TransactionRepository transactionRepository;

    public TransactionLimitEnforcementService(MerchantTransactionLimitRepository merchantLimitRepository,
                                              TransactionRepository transactionRepository) {
        this.merchantLimitRepository = merchantLimitRepository;
        this.transactionRepository = transactionRepository;
    }

    public Optional<LimitBreach> checkLimits(TransactionEntity transaction) {
        if (transaction == null || transaction.getMerchantId() == null || transaction.getAmountCents() == null) {
            return Optional.empty();
        }
        Long merchantId;
        try {
            merchantId = Long.parseLong(transaction.getMerchantId());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        MerchantTransactionLimit limits = merchantLimitRepository.findByMerchant_MerchantId(merchantId).orElse(null);
        if (limits == null || !"ACTIVE".equalsIgnoreCase(limits.getStatus())) {
            return Optional.empty();
        }

        BigDecimal txnAmount = BigDecimal.valueOf(transaction.getAmountCents())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        List<String> reasons = new ArrayList<>();

        if (limits.getPerTransactionLimit() != null
                && txnAmount.compareTo(limits.getPerTransactionLimit()) > 0) {
            reasons.add(String.format("Per-transaction limit exceeded: %s > %s",
                    txnAmount, limits.getPerTransactionLimit()));
        }

        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        Long dailyCents = transactionRepository.sumAmountByMerchantInTimeWindow(
                transaction.getMerchantId(), startOfDay, LocalDateTime.now());
        BigDecimal dailyVolume = BigDecimal.valueOf(dailyCents != null ? dailyCents : 0L)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (limits.getDailyLimit() != null
                && dailyVolume.compareTo(limits.getDailyLimit()) > 0) {
            reasons.add(String.format("Daily limit exceeded: %s > %s",
                    dailyVolume, limits.getDailyLimit()));
        }

        if (reasons.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new LimitBreach("BLOCK", reasons));
    }

    public record LimitBreach(String action, List<String> reasons) {}
}
