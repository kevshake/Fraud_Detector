package com.posgateway.aml.model;


/**
 * Merchant Metrics for Fraud and Chargeback Monitoring
 * Aggregated counters for risk simulation (VFMP, HECM)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantMetrics {
    private long totalTx;
    private long fraudTx;
    private long chargebackCount;
    private long fraudAmount; // in cents
    private long chargebackAmount; // in cents

    public static MerchantMetricsBuilder builder() {
        return new MerchantMetricsBuilder();
    }

    public static class MerchantMetricsBuilder {
        private long totalTx;
        private long fraudTx;
        private long chargebackCount;
        private long fraudAmount;
        private long chargebackAmount;

        MerchantMetricsBuilder() {
        }

        public MerchantMetricsBuilder totalTx(long totalTx) {
            this.totalTx = totalTx;
            return this;
        }

        public MerchantMetricsBuilder fraudTx(long fraudTx) {
            this.fraudTx = fraudTx;
            return this;
        }

        public MerchantMetricsBuilder chargebackCount(long chargebackCount) {
            this.chargebackCount = chargebackCount;
            return this;
        }

        public MerchantMetricsBuilder fraudAmount(long fraudAmount) {
            this.fraudAmount = fraudAmount;
            return this;
        }

        public MerchantMetricsBuilder chargebackAmount(long chargebackAmount) {
            this.chargebackAmount = chargebackAmount;
            return this;
        }

        public MerchantMetrics build() {
            return new MerchantMetrics(totalTx, fraudTx, chargebackCount, fraudAmount, chargebackAmount);
        }
    }

    public double getFraudRate() {
        if (totalTx == 0)
            return 0.0;
        return (double) fraudTx / totalTx;
    }

    public double getChargebackRatio() {
        if (totalTx == 0)
            return 0.0;
        return (double) chargebackCount / totalTx;
    }
}
