package com.solar.Quotation.dto.responsedto;

import java.math.BigDecimal;

public class ReportSummaryResponseDto {
    private Long totalQuotations, accepted, pending, rejected;
    private BigDecimal totalValue, totalGst, totalReferralPayable;

    public Long getTotalQuotations() { return totalQuotations; }
    public void setTotalQuotations(Long v) { this.totalQuotations = v; }
    public Long getAccepted() { return accepted; }
    public void setAccepted(Long v) { this.accepted = v; }
    public Long getPending() { return pending; }
    public void setPending(Long v) { this.pending = v; }
    public Long getRejected() { return rejected; }
    public void setRejected(Long v) { this.rejected = v; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal v) { this.totalValue = v; }
    public BigDecimal getTotalGst() { return totalGst; }
    public void setTotalGst(BigDecimal v) { this.totalGst = v; }
    public BigDecimal getTotalReferralPayable() { return totalReferralPayable; }
    public void setTotalReferralPayable(BigDecimal v) { this.totalReferralPayable = v; }
}