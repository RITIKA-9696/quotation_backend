package com.solar.Quotation.dto.responsedto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardResponseDto {

    // ---- Common KPIs ----
    private Long totalCustomers;
    private Long totalProjects;
    private Long totalQuotations;
    private BigDecimal totalQuotationValue;
    private Long acceptedQuotations;
    private Long rejectedQuotations;
    private Long pendingQuotations;
    private Long todayQuotations;
    private Long monthQuotations;
    private BigDecimal monthQuotationValue;

    // ---- Missing from Backend (calculated in frontend if needed) ----
    private Integer winRatePercent;
    private Long pendingApprovals;

    // ---- Super Admin Only ----
    private Long totalPartners;
    private Long activePartners;
    private BigDecimal platformPendingReferralPayouts;
    private List<TopPartner> topPartners;

    // ---- Channel Partner Only ----
    private BigDecimal totalReferralIncome;

    // ---- Charts Data ----
    private Long smallSystems;
    private Long mediumSystems;
    private Long largeSystems;
    private Map<String, Long> statusCounts;

    // YEH ADD KARNA ZAROORI THA (Compilation error fix)
    private List<SizeBucket> sizeDistribution;
    private List<MonthlyPoint> monthlyTrend;

    // ---- Recent Rows ----
    private List<DashboardRecentQuotationResponseDto> recentQuotations;

    // Getters and Setters
    public Long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(Long v) { this.totalCustomers = v; }
    public Long getTotalProjects() { return totalProjects; }
    public void setTotalProjects(Long v) { this.totalProjects = v; }
    public Long getTotalQuotations() { return totalQuotations; }
    public void setTotalQuotations(Long v) { this.totalQuotations = v; }
    public BigDecimal getTotalQuotationValue() { return totalQuotationValue; }
    public void setTotalQuotationValue(BigDecimal v) { this.totalQuotationValue = v; }
    public Long getAcceptedQuotations() { return acceptedQuotations; }
    public void setAcceptedQuotations(Long v) { this.acceptedQuotations = v; }
    public Long getRejectedQuotations() { return rejectedQuotations; }
    public void setRejectedQuotations(Long v) { this.rejectedQuotations = v; }
    public Long getPendingQuotations() { return pendingQuotations; }
    public void setPendingQuotations(Long v) { this.pendingQuotations = v; }
    public Long getTodayQuotations() { return todayQuotations; }
    public void setTodayQuotations(Long v) { this.todayQuotations = v; }
    public Long getMonthQuotations() { return monthQuotations; }
    public void setMonthQuotations(Long v) { this.monthQuotations = v; }
    public BigDecimal getMonthQuotationValue() { return monthQuotationValue; }
    public void setMonthQuotationValue(BigDecimal v) { this.monthQuotationValue = v; }
    public Integer getWinRatePercent() { return winRatePercent; }
    public void setWinRatePercent(Integer v) { this.winRatePercent = v; }
    public Long getPendingApprovals() { return pendingApprovals; }
    public void setPendingApprovals(Long v) { this.pendingApprovals = v; }
    public Long getTotalPartners() { return totalPartners; }
    public void setTotalPartners(Long v) { this.totalPartners = v; }
    public Long getActivePartners() { return activePartners; }
    public void setActivePartners(Long v) { this.activePartners = v; }
    public BigDecimal getPlatformPendingReferralPayouts() { return platformPendingReferralPayouts; }
    public void setPlatformPendingReferralPayouts(BigDecimal v) { this.platformPendingReferralPayouts = v; }
    public List<TopPartner> getTopPartners() { return topPartners; }
    public void setTopPartners(List<TopPartner> v) { this.topPartners = v; }
    public BigDecimal getTotalReferralIncome() { return totalReferralIncome; }
    public void setTotalReferralIncome(BigDecimal v) { this.totalReferralIncome = v; }
    public Long getSmallSystems() { return smallSystems; }
    public void setSmallSystems(Long v) { this.smallSystems = v; }
    public Long getMediumSystems() { return mediumSystems; }
    public void setMediumSystems(Long v) { this.mediumSystems = v; }
    public Long getLargeSystems() { return largeSystems; }
    public void setLargeSystems(Long v) { this.largeSystems = v; }
    public Map<String, Long> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(Map<String, Long> v) { this.statusCounts = v; }
    public List<DashboardRecentQuotationResponseDto> getRecentQuotations() { return recentQuotations; }
    public void setRecentQuotations(List<DashboardRecentQuotationResponseDto> v) { this.recentQuotations = v; }

    // SizeBucket aur MonthlyPoint ke getters/setters
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

    // YEH CLASS MISSING THI, ISLIYE ERROR AA RAHA THA
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

    // YEH CLASS BHI MISSING THI
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