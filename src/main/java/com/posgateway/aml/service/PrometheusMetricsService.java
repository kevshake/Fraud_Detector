package com.posgateway.aml.service;

import com.posgateway.aml.model.CaseStatus;
import com.posgateway.aml.model.RiskLevel;
import com.posgateway.aml.model.SarStatus;
import com.posgateway.aml.repository.ComplianceCaseRepository;
import com.posgateway.aml.repository.SuspiciousActivityReportRepository;
import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.DistributionSummary;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive Prometheus Metrics Service
 * Collects and exposes all metrics for AML/Fraud Detection system
 */
@Service
public class PrometheusMetricsService {

        private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricsService.class);

        private final MeterRegistry meterRegistry;
        private final ComplianceCaseRepository caseRepository;
        private final SuspiciousActivityReportRepository sarRepository;

        // Transaction Metrics
        private final Counter transactionTotalCounter;
        private final Counter transactionProcessedCounter;
        private final Counter transactionBlockedCounter;
        private final Counter transactionAllowedCounter;
        private final Counter transactionHeldCounter;
        private final Counter transactionAlertedCounter;
        private final Timer transactionProcessingTime;
        private final AtomicLong transactionQueueSizeValue = new AtomicLong(0);

        // AML Metrics
        private final Counter amlRiskAssessmentCounter;
        private final Counter amlHighRiskCounter;
        private final Counter amlMediumRiskCounter;
        private final Counter amlLowRiskCounter;
        private final Counter amlVelocityCheckCounter;
        private final Counter amlGeographicRiskCounter;
        private final Counter amlPatternRiskCounter;
        private final Counter amlAmountRiskCounter;
        private final Timer amlAssessmentTime;
        private final AtomicLong amlActiveCasesValue = new AtomicLong(0);

        // Fraud Detection Metrics
        private final Counter fraudAssessmentCounter;
        private final Counter fraudDetectedCounter;
        private final Counter fraudFalsePositiveCounter;
        private final Counter fraudScoreHighCounter;
        private final Counter fraudScoreMediumCounter;
        private final Counter fraudScoreLowCounter;
        private final Counter fraudDeviceRiskCounter;
        private final Counter fraudIpRiskCounter;
        private final Counter fraudBehavioralRiskCounter;
        private final Counter fraudVelocityRiskCounter;
        private final Timer fraudAssessmentTime;
        private final DistributionSummary fraudScoreDistribution;

        // Compliance Metrics
        private final Counter complianceCaseCreatedCounter;
        private final Counter complianceCaseResolvedCounter;
        private final Counter complianceCaseEscalatedCounter;
        @SuppressWarnings("unused")
        private final Counter complianceCaseByStatusCounter;
        @SuppressWarnings("unused")
        private final Counter complianceCaseByPriorityCounter;
        private final Counter sarCreatedCounter;
        private final Counter sarFiledCounter;
        private final Counter sarApprovedCounter;
        private final Counter sarRejectedCounter;
        private final Timer caseResolutionTime;
        private final Timer sarFilingTime;
        private final AtomicLong complianceOpenCasesValue = new AtomicLong(0);
        private final AtomicLong compliancePendingSarsValue = new AtomicLong(0);

        // Performance Metrics
        private final Timer apiResponseTime;
        private final Counter apiRequestCounter;
        private final Counter apiErrorCounter;
        @SuppressWarnings("unused")
        private final Counter apiErrorByTypeCounter;

        // System Health Metrics
        private final Counter cacheHitCounter;
        private final Counter cacheMissCounter;
        private final Timer cacheOperationTime;

        // Model Performance Metrics
        private final Counter modelScoringCounter;
        private final Counter modelScoringSuccessCounter;
        private final Counter modelScoringFailureCounter;
        private final Timer modelScoringTime;
        private final DistributionSummary modelScoreDistribution;

        // Screening Metrics
        private final Counter screeningTotalCounter;
        private final Counter screeningMatchCounter;
        private final Counter screeningNoMatchCounter;
        private final Counter screeningByTypeCounter;
        private final Timer screeningTime;

        @Autowired
        public PrometheusMetricsService(
                        MeterRegistry meterRegistry,
                        ComplianceCaseRepository caseRepository,
                        SuspiciousActivityReportRepository sarRepository) {
                this.meterRegistry = meterRegistry;
                this.caseRepository = caseRepository;
                this.sarRepository = sarRepository;

                // Initialize Transaction Metrics
                this.transactionTotalCounter = Counter.builder("aml.transactions.total")
                                .description("Total number of transactions received")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.transactionProcessedCounter = Counter.builder("aml.transactions.processed")
                                .description("Number of transactions processed")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.transactionBlockedCounter = Counter.builder("aml.transactions.blocked")
                                .description("Number of transactions blocked")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.transactionAllowedCounter = Counter.builder("aml.transactions.allowed")
                                .description("Number of transactions allowed")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.transactionHeldCounter = Counter.builder("aml.transactions.held")
                                .description("Number of transactions held for review")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.transactionAlertedCounter = Counter.builder("aml.transactions.alerted")
                                .description("Number of transactions that generated alerts")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.transactionProcessingTime = Timer.builder("aml.transactions.processing.time")
                                .description("Transaction processing time in milliseconds")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                Gauge
                                .builder("aml.transactions.queue.size", transactionQueueSizeValue, AtomicLong::get)
                                .description("Current transaction queue size")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                // Initialize AML Metrics
                this.amlRiskAssessmentCounter = Counter.builder("aml.risk.assessments.total")
                                .description("Total AML risk assessments performed")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.amlHighRiskCounter = Counter.builder("aml.risk.high")
                                .description("Number of high-risk assessments")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.amlMediumRiskCounter = Counter.builder("aml.risk.medium")
                                .description("Number of medium-risk assessments")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.amlLowRiskCounter = Counter.builder("aml.risk.low")
                                .description("Number of low-risk assessments")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.amlVelocityCheckCounter = Counter.builder("aml.velocity.checks")
                                .description("Number of velocity checks performed")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.amlGeographicRiskCounter = Counter.builder("aml.geographic.risk")
                                .description("Number of geographic risk detections")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.amlPatternRiskCounter = Counter.builder("aml.pattern.risk")
                                .description("Number of pattern risk detections")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.amlAmountRiskCounter = Counter.builder("aml.amount.risk")
                                .description("Number of amount-based risk detections")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.amlAssessmentTime = Timer.builder("aml.assessment.time")
                                .description("AML risk assessment time in milliseconds")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                Gauge.builder("aml.active.cases", amlActiveCasesValue, AtomicLong::get)
                                .description("Number of active AML cases")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                // Initialize Fraud Detection Metrics
                this.fraudAssessmentCounter = Counter.builder("fraud.assessments.total")
                                .description("Total fraud assessments performed")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.fraudDetectedCounter = Counter.builder("fraud.detected")
                                .description("Number of fraud detections")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.fraudFalsePositiveCounter = Counter.builder("fraud.false.positive")
                                .description("Number of false positive fraud detections")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.fraudScoreHighCounter = Counter.builder("fraud.score.high")
                                .description("Number of high fraud scores")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.fraudScoreMediumCounter = Counter.builder("fraud.score.medium")
                                .description("Number of medium fraud scores")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.fraudScoreLowCounter = Counter.builder("fraud.score.low")
                                .description("Number of low fraud scores")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.fraudDeviceRiskCounter = Counter.builder("fraud.device.risk")
                                .description("Number of device risk detections")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.fraudIpRiskCounter = Counter.builder("fraud.ip.risk")
                                .description("Number of IP risk detections")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.fraudBehavioralRiskCounter = Counter.builder("fraud.behavioral.risk")
                                .description("Number of behavioral risk detections")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.fraudVelocityRiskCounter = Counter.builder("fraud.velocity.risk")
                                .description("Number of velocity risk detections")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.fraudAssessmentTime = Timer.builder("fraud.assessment.time")
                                .description("Fraud assessment time in milliseconds")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.fraudScoreDistribution = DistributionSummary.builder("fraud.score.distribution")
                                .description("Fraud score distribution")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                // Initialize Compliance Metrics
                this.complianceCaseCreatedCounter = Counter.builder("compliance.cases.created")
                                .description("Number of compliance cases created")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.complianceCaseResolvedCounter = Counter.builder("compliance.cases.resolved")
                                .description("Number of compliance cases resolved")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.complianceCaseEscalatedCounter = Counter.builder("compliance.cases.escalated")
                                .description("Number of compliance cases escalated")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.complianceCaseByStatusCounter = Counter.builder("compliance.cases.by.status")
                                .description("Compliance cases by status")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.complianceCaseByPriorityCounter = Counter.builder("compliance.cases.by.priority")
                                .description("Compliance cases by priority")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.sarCreatedCounter = Counter.builder("compliance.sars.created")
                                .description("Number of SARs created")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.sarFiledCounter = Counter.builder("compliance.sars.filed")
                                .description("Number of SARs filed")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.sarApprovedCounter = Counter.builder("compliance.sars.approved")
                                .description("Number of SARs approved")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.sarRejectedCounter = Counter.builder("compliance.sars.rejected")
                                .description("Number of SARs rejected")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.caseResolutionTime = Timer.builder("compliance.case.resolution.time")
                                .description("Time to resolve compliance cases in hours")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.sarFilingTime = Timer.builder("compliance.sar.filing.time")
                                .description("Time to file SARs in hours")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                Gauge
                                .builder("compliance.open.cases", complianceOpenCasesValue, AtomicLong::get)
                                .description("Number of open compliance cases")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                Gauge
                                .builder("compliance.pending.sars", compliancePendingSarsValue, AtomicLong::get)
                                .description("Number of pending SARs")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                // Initialize Performance Metrics
                this.apiResponseTime = Timer.builder("api.response.time")
                                .description("API response time in milliseconds")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.apiRequestCounter = Counter.builder("api.requests.total")
                                .description("Total API requests")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.apiErrorCounter = Counter.builder("api.errors.total")
                                .description("Total API errors")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.apiErrorByTypeCounter = Counter.builder("api.errors.by.type")
                                .description("API errors by type")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                Gauge
                                .builder("system.active.connections", () -> 0.0) // Placeholder, as
                                                                                 // activeConnectionsValue is removed
                                .description("Number of active connections")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                Gauge.builder("database.connection.pool.size", () -> 0.0)
                                .description("Database connection pool size")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                Gauge.builder("database.connection.pool.active", () -> 0.0)
                                .description("Active database connections")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                // Initialize System Health Metrics
                Gauge
                                .builder("system.uptime.seconds",
                                                () -> (System.currentTimeMillis() - startTime) / 1000.0)
                                .description("System uptime in seconds")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                Gauge
                                .builder("jvm.memory.used.bytes",
                                                () -> (double) (Runtime.getRuntime().totalMemory()
                                                                - Runtime.getRuntime().freeMemory()))
                                .description("JVM memory used in bytes")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                Gauge
                                .builder("jvm.memory.max.bytes", () -> (double) Runtime.getRuntime().maxMemory())
                                .description("JVM memory max in bytes")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                Gauge.builder("jvm.threads.active", () -> (double) Thread.activeCount())
                                .description("Number of active JVM threads")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.cacheHitCounter = Counter.builder("cache.hits")
                                .description("Cache hits")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.cacheMissCounter = Counter.builder("cache.misses")
                                .description("Cache misses")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.cacheOperationTime = Timer.builder("cache.operation.time")
                                .description("Cache operation time in milliseconds")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                // Initialize Model Performance Metrics
                this.modelScoringCounter = Counter.builder("model.scoring.total")
                                .description("Total model scoring operations")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.modelScoringSuccessCounter = Counter.builder("model.scoring.success")
                                .description("Successful model scoring operations")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.modelScoringFailureCounter = Counter.builder("model.scoring.failure")
                                .description("Failed model scoring operations")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.modelScoringTime = Timer.builder("model.scoring.time")
                                .description("Model scoring time in milliseconds")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.modelScoreDistribution = DistributionSummary.builder("model.score.distribution")
                                .description("Model score distribution")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                // Initialize Screening Metrics
                this.screeningTotalCounter = Counter.builder("screening.total")
                                .description("Total screening operations")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.screeningMatchCounter = Counter.builder("screening.matches")
                                .description("Number of screening matches")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.screeningNoMatchCounter = Counter.builder("screening.no.matches")
                                .description("Number of screening non-matches")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.screeningByTypeCounter = Counter.builder("screening.by.type")
                                .description("Screening operations by type")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);

                this.screeningTime = Timer.builder("screening.time")
                                .description("Screening operation time in milliseconds")
                                .tag("application", "aml-fraud-detector")
                                .register(meterRegistry);
        }

        @PostConstruct
        public void init() {
                logger.info("PrometheusMetricsService initialized with comprehensive metrics collection");
        }

        // Transaction Metrics Methods
        public void incrementTransactionTotal(String merchantId) {
                transactionTotalCounter.increment();
                Counter.builder("aml.transactions.total")
                                .tag("merchant_id", merchantId != null ? merchantId : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        public void incrementTransactionProcessed(String merchantId, String action) {
                transactionProcessedCounter.increment();
                Counter.builder("aml.transactions.processed")
                                .tag("merchant_id", merchantId != null ? merchantId : "unknown")
                                .tag("action", action != null ? action : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        public void incrementTransactionBlocked(String merchantId, String reason) {
                transactionBlockedCounter.increment();
                Counter.builder("aml.transactions.blocked")
                                .tag("merchant_id", merchantId != null ? merchantId : "unknown")
                                .tag("reason", reason != null ? reason : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        public void incrementTransactionAllowed(String merchantId) {
                transactionAllowedCounter.increment();
                Counter.builder("aml.transactions.allowed")
                                .tag("merchant_id", merchantId != null ? merchantId : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        public void incrementTransactionHeld(String merchantId) {
                transactionHeldCounter.increment();
                Counter.builder("aml.transactions.held")
                                .tag("merchant_id", merchantId != null ? merchantId : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        public void incrementTransactionAlerted(String merchantId) {
                transactionAlertedCounter.increment();
                Counter.builder("aml.transactions.alerted")
                                .tag("merchant_id", merchantId != null ? merchantId : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        public void recordTransactionProcessingTime(long timeMs) {
                transactionProcessingTime.record(timeMs, TimeUnit.MILLISECONDS);
        }

        public void setTransactionQueueSize(long size) {
                transactionQueueSizeValue.set(size);
        }

        // AML Metrics Methods
        public void incrementAmlRiskAssessment(RiskLevel riskLevel) {
                amlRiskAssessmentCounter.increment();
                Counter.builder("aml.risk.assessments.total")
                                .tag("risk_level", riskLevel != null ? riskLevel.name() : "UNKNOWN")
                                .register(meterRegistry)
                                .increment();

                if (riskLevel == RiskLevel.HIGH) {
                        amlHighRiskCounter.increment();
                } else if (riskLevel == RiskLevel.MEDIUM) {
                        amlMediumRiskCounter.increment();
                } else if (riskLevel == RiskLevel.LOW) {
                        amlLowRiskCounter.increment();
                }
        }

        public void incrementAmlVelocityCheck() {
                amlVelocityCheckCounter.increment();
        }

        public void incrementAmlGeographicRisk() {
                amlGeographicRiskCounter.increment();
        }

        public void incrementAmlPatternRisk() {
                amlPatternRiskCounter.increment();
        }

        public void incrementAmlAmountRisk() {
                amlAmountRiskCounter.increment();
        }

        public void recordAmlAssessmentTime(long timeMs) {
                amlAssessmentTime.record(timeMs, TimeUnit.MILLISECONDS);
        }

        // Fraud Detection Metrics Methods
        public void incrementFraudAssessment(boolean detected) {
                fraudAssessmentCounter.increment();
                if (detected) {
                        fraudDetectedCounter.increment();
                }
        }

        public void incrementFraudFalsePositive() {
                fraudFalsePositiveCounter.increment();
        }

        public void incrementFraudScore(RiskLevel riskLevel) {
                if (riskLevel == RiskLevel.HIGH) {
                        fraudScoreHighCounter.increment();
                } else if (riskLevel == RiskLevel.MEDIUM) {
                        fraudScoreMediumCounter.increment();
                } else if (riskLevel == RiskLevel.LOW) {
                        fraudScoreLowCounter.increment();
                }
        }

        public void incrementFraudDeviceRisk() {
                fraudDeviceRiskCounter.increment();
        }

        public void incrementFraudIpRisk() {
                fraudIpRiskCounter.increment();
        }

        public void incrementFraudBehavioralRisk() {
                fraudBehavioralRiskCounter.increment();
        }

        public void incrementFraudVelocityRisk() {
                fraudVelocityRiskCounter.increment();
        }

        public void recordFraudAssessmentTime(long timeMs) {
                fraudAssessmentTime.record(timeMs, TimeUnit.MILLISECONDS);
        }

        public void recordFraudScore(double score) {
                fraudScoreDistribution.record(score);
        }

        public void recordModelScore(double score) {
                modelScoreDistribution.record(score);
        }

        // Compliance Metrics Methods
        public void incrementComplianceCaseCreated(CaseStatus status, String priority) {
                complianceCaseCreatedCounter.increment();
                Counter.builder("compliance.cases.created")
                                .tag("status", status != null ? status.name() : "UNKNOWN")
                                .tag("priority", priority != null ? priority : "UNKNOWN")
                                .register(meterRegistry)
                                .increment();
                Counter.builder("compliance.cases.by.status")
                                .tag("status", status != null ? status.name() : "UNKNOWN")
                                .register(meterRegistry)
                                .increment();
        }

        public void incrementComplianceCaseResolved() {
                complianceCaseResolvedCounter.increment();
        }

        public void incrementComplianceCaseEscalated() {
                complianceCaseEscalatedCounter.increment();
        }

        public void incrementComplianceCaseByPriority(String priority) {
                Counter.builder("compliance.cases.by.priority")
                                .tag("priority", priority != null ? priority : "UNKNOWN")
                                .register(meterRegistry)
                                .increment();
        }

        public void incrementSarCreated() {
                sarCreatedCounter.increment();
        }

        public void incrementSarFiled() {
                sarFiledCounter.increment();
        }

        public void incrementSarApproved() {
                sarApprovedCounter.increment();
        }

        public void incrementSarRejected() {
                sarRejectedCounter.increment();
        }

        public void recordCaseResolutionTime(long hours) {
                caseResolutionTime.record(hours, TimeUnit.HOURS);
        }

        public void recordSarFilingTime(long hours) {
                sarFilingTime.record(hours, TimeUnit.HOURS);
        }

        // Performance Metrics Methods
        public void recordApiResponseTime(String endpoint, String method, long timeMs) {
                apiResponseTime.record(timeMs, TimeUnit.MILLISECONDS);
                Timer.builder("api.response.time")
                                .tag("endpoint", endpoint != null ? endpoint : "unknown")
                                .tag("method", method != null ? method : "unknown")
                                .register(meterRegistry)
                                .record(timeMs, TimeUnit.MILLISECONDS);
        }

        public void incrementApiRequest(String endpoint, String method) {
                apiRequestCounter.increment();
                Counter.builder("api.requests.total")
                                .tag("endpoint", endpoint != null ? endpoint : "unknown")
                                .tag("method", method != null ? method : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        public void incrementApiError(String endpoint, String errorType) {
                apiErrorCounter.increment();
                Counter.builder("api.errors.total")
                                .tag("endpoint", endpoint != null ? endpoint : "unknown")
                                .register(meterRegistry)
                                .increment();
                Counter.builder("api.errors.by.type")
                                .tag("error_type", errorType != null ? errorType : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        // System Health Metrics Methods

        public void incrementCacheHit() {
                cacheHitCounter.increment();
        }

        public void incrementCacheMiss() {
                cacheMissCounter.increment();
        }

        public void recordCacheOperationTime(long timeMs) {
                cacheOperationTime.record(timeMs, TimeUnit.MILLISECONDS);
        }

        // Model Performance Metrics Methods
        public void incrementModelScoring(boolean success) {
                modelScoringCounter.increment();
                if (success) {
                        modelScoringSuccessCounter.increment();
                } else {
                        modelScoringFailureCounter.increment();
                }
        }

        public void recordModelScoringTime(long timeMs) {
                modelScoringTime.record(timeMs, TimeUnit.MILLISECONDS);
        }

        // Screening Metrics Methods
        public void incrementScreeningTotal(String type) {
                screeningTotalCounter.increment();
                Counter.builder("screening.total")
                                .tag("type", type != null ? type : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        public void incrementScreeningMatch(String type) {
                screeningMatchCounter.increment();
                Counter.builder("screening.matches")
                                .tag("type", type != null ? type : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        public void incrementScreeningNoMatch(String type) {
                screeningNoMatchCounter.increment();
                Counter.builder("screening.no.matches")
                                .tag("type", type != null ? type : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        public void incrementScreeningByType(String type) {
                screeningByTypeCounter.increment();
                Counter.builder("screening.by.type")
                                .tag("type", type != null ? type : "unknown")
                                .register(meterRegistry)
                                .increment();
        }

        public void recordScreeningTime(long timeMs) {
                screeningTime.record(timeMs, TimeUnit.MILLISECONDS);
        }

        // Scheduled task to update gauge values from database
        @Scheduled(fixedRate = 30000) // Every 30 seconds
        public void updateGaugeMetrics() {
                try {
                        // Update transaction queue size (if you have a queue)
                        // transactionQueueSizeValue.set(queue.size());

                        // Update AML active cases
                        long activeCases = caseRepository.countByStatus(CaseStatus.NEW) +
                                        caseRepository.countByStatus(CaseStatus.ASSIGNED) +
                                        caseRepository.countByStatus(CaseStatus.IN_PROGRESS);
                        amlActiveCasesValue.set(activeCases);

                        // Update compliance open cases
                        long openCases = caseRepository.countByStatus(CaseStatus.NEW) +
                                        caseRepository.countByStatus(CaseStatus.ASSIGNED) +
                                        caseRepository.countByStatus(CaseStatus.IN_PROGRESS);
                        complianceOpenCasesValue.set(openCases);

                        // Update compliance pending SARs
                        long pendingSars = sarRepository.countByStatus(SarStatus.PENDING_REVIEW);
                        compliancePendingSarsValue.set(pendingSars);

                        // Update screening queue size (if you have a queue)
                        // screeningQueueSizeValue.set(screeningQueue.size());

                } catch (Exception e) {
                        logger.error("Error updating gauge metrics", e);
                }
        }

        private static final long startTime = System.currentTimeMillis();
}
