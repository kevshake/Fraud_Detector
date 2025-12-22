package com.posgateway.aml.service.reporting;

import com.posgateway.aml.entity.TransactionEntity;
import com.posgateway.aml.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Regulatory Reporting Service
 * Generates regulatory reports (CTR, LCTR, IFTR)
 */
@Service
public class RegulatoryReportingService {

    private static final Logger logger = LoggerFactory.getLogger(RegulatoryReportingService.class);

    private final TransactionRepository transactionRepository;

    @Value("${regulatory.ctr.threshold:10000}")
    private BigDecimal ctrThreshold;

    @Value("${regulatory.lctr.threshold:100000}")
    private BigDecimal lctrThreshold;

    @Autowired
    public RegulatoryReportingService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Generate Currency Transaction Report (CTR)
     */
    public CurrencyTransactionReport generateCtr(LocalDateTime startDate, LocalDateTime endDate) {
        // Find transactions at or above CTR threshold
        List<TransactionEntity> transactions = findTransactionsAboveThreshold(ctrThreshold, startDate, endDate);

        CurrencyTransactionReport report = new CurrencyTransactionReport();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setThreshold(ctrThreshold);
        report.setTransactionCount(transactions.size());
        report.setTotalAmount(calculateTotalAmount(transactions));
        report.setTransactions(transactions);

        logger.info("Generated CTR: {} transactions, total amount: {}", 
                transactions.size(), report.getTotalAmount());
        return report;
    }

    /**
     * Generate Large Cash Transaction Report (LCTR)
     */
    public LargeCashTransactionReport generateLctr(LocalDateTime startDate, LocalDateTime endDate) {
        List<TransactionEntity> transactions = findTransactionsAboveThreshold(lctrThreshold, startDate, endDate);

        LargeCashTransactionReport report = new LargeCashTransactionReport();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setThreshold(lctrThreshold);
        report.setTransactionCount(transactions.size());
        report.setTotalAmount(calculateTotalAmount(transactions));
        report.setTransactions(transactions);

        logger.info("Generated LCTR: {} transactions, total amount: {}", 
                transactions.size(), report.getTotalAmount());
        return report;
    }

    /**
     * Generate International Funds Transfer Report (IFTR)
     */
    public InternationalFundsTransferReport generateIftr(LocalDateTime startDate, LocalDateTime endDate) {
        // TODO: Implement based on transaction currency and destination country
        InternationalFundsTransferReport report = new InternationalFundsTransferReport();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setTransactionCount(0);
        report.setTotalAmount(BigDecimal.ZERO);
        report.setTransactions(new ArrayList<>());
        return report;
    }

    /**
     * Find transactions above threshold
     */
    private List<TransactionEntity> findTransactionsAboveThreshold(BigDecimal threshold, 
                                                                   LocalDateTime startDate, 
                                                                   LocalDateTime endDate) {
        // Get all transactions in date range and filter by amount
        // Note: This is a simplified implementation - in production, use database query
        List<TransactionEntity> allTransactions = transactionRepository.findAll();
        BigDecimal thresholdCents = threshold.multiply(new BigDecimal("100"));

        return allTransactions.stream()
                .filter(tx -> tx.getTxnTs() != null &&
                        !tx.getTxnTs().isBefore(startDate) &&
                        !tx.getTxnTs().isAfter(endDate) &&
                        tx.getAmountCents() != null &&
                        tx.getAmountCents() >= thresholdCents.longValue())
                .toList();
    }

    /**
     * Calculate total amount from transactions
     */
    private BigDecimal calculateTotalAmount(List<TransactionEntity> transactions) {
        return transactions.stream()
                .map(tx -> tx.getAmountCents() != null ? 
                        BigDecimal.valueOf(tx.getAmountCents()).divide(new BigDecimal("100")) : 
                        BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Currency Transaction Report DTO
     */
    public static class CurrencyTransactionReport {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private BigDecimal threshold;
        private int transactionCount;
        private BigDecimal totalAmount;
        private List<TransactionEntity> transactions;

        // Getters and Setters
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public BigDecimal getThreshold() { return threshold; }
        public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
        public int getTransactionCount() { return transactionCount; }
        public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public List<TransactionEntity> getTransactions() { return transactions; }
        public void setTransactions(List<TransactionEntity> transactions) { this.transactions = transactions; }
    }

    /**
     * Large Cash Transaction Report DTO
     */
    public static class LargeCashTransactionReport {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private BigDecimal threshold;
        private int transactionCount;
        private BigDecimal totalAmount;
        private List<TransactionEntity> transactions;

        // Getters and Setters
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public BigDecimal getThreshold() { return threshold; }
        public void setThreshold(BigDecimal threshold) { this.threshold = threshold; }
        public int getTransactionCount() { return transactionCount; }
        public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public List<TransactionEntity> getTransactions() { return transactions; }
        public void setTransactions(List<TransactionEntity> transactions) { this.transactions = transactions; }
    }

    /**
     * International Funds Transfer Report DTO
     */
    public static class InternationalFundsTransferReport {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private int transactionCount;
        private BigDecimal totalAmount;
        private List<TransactionEntity> transactions;

        // Getters and Setters
        public LocalDateTime getStartDate() { return startDate; }
        public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
        public int getTransactionCount() { return transactionCount; }
        public void setTransactionCount(int transactionCount) { this.transactionCount = transactionCount; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public List<TransactionEntity> getTransactions() { return transactions; }
        public void setTransactions(List<TransactionEntity> transactions) { this.transactions = transactions; }
    }
}

