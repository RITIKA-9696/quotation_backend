package com.solar.Quotation.dto.responsedto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReferralActivityResponseDto {
    private Long quotationId;
    private String quotationNo, customer, status;
    private BigDecimal quotationAmount, referralPercent, referralAmount;
    private LocalDate date, paidDate;

    public Long getQuotationId() { return quotationId; }
    public void setQuotationId(Long v) { this.quotationId = v; }
    public String getQuotationNo() { return quotationNo; }
    public void setQuotationNo(String v) { this.quotationNo = v; }
    public String getCustomer() { return customer; }
    public void setCustomer(String v) { this.customer = v; }
    public BigDecimal getQuotationAmount() { return quotationAmount; }
    public void setQuotationAmount(BigDecimal v) { this.quotationAmount = v; }
    public BigDecimal getReferralPercent() { return referralPercent; }
    public void setReferralPercent(BigDecimal v) { this.referralPercent = v; }
    public BigDecimal getReferralAmount() { return referralAmount; }
    public void setReferralAmount(BigDecimal v) { this.referralAmount = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate v) { this.date = v; }
    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate v) { this.paidDate = v; }
}