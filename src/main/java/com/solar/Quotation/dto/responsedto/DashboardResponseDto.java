package com.solar.Quotation.dto.responsedto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardResponseDto {

    // ---- Summary ----
    private Long totalCustomers;
    private Long totalProjects;
    private Long totalQuotations;
    private BigDecimal totalQuotationValue;
    private BigDecimal quotationValueThisMonth;
    private Long pendingApprovals;
    private BigDecimal totalReferralIncome;
    private Long totalPartners;
    private Long activePartners;
    private BigDecimal platformPendingReferralPayouts;
    private List<TopPartner> topPartners;

    // ---- Charts ----
    private Map<String, Long> statusCounts;
    private List<SizeBucket> sizeDistribution;
    private List<MonthlyPoint> monthlyTrend;

    public Long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(Long v) { this.totalCustomers = v; }
    public Long getTotalProjects() { return totalProjects; }
    public void setTotalProjects(Long v) { this.totalProjects = v; }
    public Long getTotalQuotations() { return totalQuotations; }
    public void setTotalQuotations(Long v) { this.totalQuotations = v; }
    public BigDecimal getTotalQuotationValue() { return totalQuotationValue; }
    public void setTotalQuotationValue(BigDecimal v) { this.totalQuotationValue = v; }
    public BigDecimal getQuotationValueThisMonth() { return quotationValueThisMonth; }
    public void setQuotationValueThisMonth(BigDecimal v) { this.quotationValueThisMonth = v; }
    public Long getPendingApprovals() { return pendingApprovals; }
    public void setPendingApprovals(Long v) { this.pendingApprovals = v; }
    public BigDecimal getTotalReferralIncome() { return totalReferralIncome; }
    public void setTotalReferralIncome(BigDecimal v) { this.totalReferralIncome = v; }
    public Long getTotalPartners() { return totalPartners; }
    public void setTotalPartners(Long v) { this.totalPartners = v; }
    public Long getActivePartners() { return activePartners; }
    public void setActivePartners(Long v) { this.activePartners = v; }
    public BigDecimal getPlatformPendingReferralPayouts() { return platformPendingReferralPayouts; }
    public void setPlatformPendingReferralPayouts(BigDecimal v) { this.platformPendingReferralPayouts = v; }
    public List<TopPartner> getTopPartners() { return topPartners; }
    public void setTopPartners(List<TopPartner> v) { this.topPartners = v; }
    public Map<String, Long> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<String, Long> v) { this.statusCounts = v; }
    public List<SizeBucket> getSizeDistribution() { return sizeDistribution; }
    public void setSizeDistribution(List<SizeBucket> v) { this.sizeDistribution = v; }
    public List<MonthlyPoint> getMonthlyTrend() { return monthlyTrend; }
    public void setMonthlyTrend(List<MonthlyPoint> v) { this.monthlyTrend = v; }

    public static class TopPartner {
        private Long partnerId;
        private String partnerName;
        private String partnerCode;
        private Long totalQuotations;
        private BigDecimal totalRevenue;
        public Long getPartnerId() { return partnerId; }
        public void setPartnerId(Long v) { this.partnerId = v; }
        public String getPartnerName() { return partnerName; }
        public void setPartnerName(String v) { this.partnerName = v; }
        public String getPartnerCode() { return partnerCode; }
        public void setPartnerCode(String v) { this.partnerCode = v; }
        public Long getTotalQuotations() { return totalQuotations; }
        public void setTotalQuotations(Long v) { this.totalQuotations = v; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal v) { this.totalRevenue = v; }
    }

    public static class SizeBucket {
        private String label;
        private Long count;
        public SizeBucket() {}
        public SizeBucket(String label, Long count) { this.label = label; this.count = count; }
        public String getLabel() { return label; }
        public void setLabel(String v) { this.label = v; }
        public Long getCount() { return count; }
        public void setCount(Long v) { this.count = v; }
    }

    public static class MonthlyPoint {
        private String month;
        private Long quotations;
        private BigDecimal value;
        public MonthlyPoint() {}
        public MonthlyPoint(String m, Long q, BigDecimal v) { this.month = m; this.quotations = q; this.value = v; }
        public String getMonth() { return month; }
        public void setMonth(String v) { this.month = v; }
        public Long getQuotations() { return quotations; }
        public void setQuotations(Long v) { this.quotations = v; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal v) { this.value = v; }
    }
}