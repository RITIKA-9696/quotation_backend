package com.solar.Quotation.dto.responsedto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReportRowResponseDto {

    public static class Quotation {
        private String quoteNo, customer, salesPerson, status, partnerName;
        private BigDecimal systemSizeKw, amount;
        private LocalDate date;

        public String getQuoteNo() { return quoteNo; }
        public void setQuoteNo(String v) { this.quoteNo = v; }
        public String getCustomer() { return customer; }
        public void setCustomer(String v) { this.customer = v; }
        public BigDecimal getSystemSizeKw() { return systemSizeKw; }
        public void setSystemSizeKw(BigDecimal v) { this.systemSizeKw = v; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal v) { this.amount = v; }
        public String getSalesPerson() { return salesPerson; }
        public void setSalesPerson(String v) { this.salesPerson = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate v) { this.date = v; }
        public String getPartnerName() { return partnerName; }
        public void setPartnerName(String v) { this.partnerName = v; }
    }

    public static class Gst {
        private String quoteNo, customer;
        private BigDecimal taxableAmount, gstPercent, cgstAmount, sgstAmount, gstAmount, total;
        private LocalDate date;
        public String getQuoteNo() { return quoteNo; }
        public void setQuoteNo(String v) { this.quoteNo = v; }
        public String getCustomer() { return customer; }
        public void setCustomer(String v) { this.customer = v; }
        public BigDecimal getTaxableAmount() { return taxableAmount; }
        public void setTaxableAmount(BigDecimal v) { this.taxableAmount = v; }
        public BigDecimal getGstPercent() { return gstPercent; }
        public void setGstPercent(BigDecimal v) { this.gstPercent = v; }
        public BigDecimal getCgstAmount() { return cgstAmount; }
        public void setCgstAmount(BigDecimal v) { this.cgstAmount = v; }
        public BigDecimal getSgstAmount() { return sgstAmount; }
        public void setSgstAmount(BigDecimal v) { this.sgstAmount = v; }
        public BigDecimal getGstAmount() { return gstAmount; }
        public void setGstAmount(BigDecimal v) { this.gstAmount = v; }
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal v) { this.total = v; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate v) { this.date = v; }
    }

    public static class ReferralIncome {
        private String referrerName, referredCustomer, status, partnerName;
        private BigDecimal quotationAmount, referralPercent, referralAmount;
        public String getReferrerName() { return referrerName; }
        public void setReferrerName(String v) { this.referrerName = v; }
        public String getReferredCustomer() { return referredCustomer; }
        public void setReferredCustomer(String v) { this.referredCustomer = v; }
        public BigDecimal getQuotationAmount() { return quotationAmount; }
        public void setQuotationAmount(BigDecimal v) { this.quotationAmount = v; }
        public BigDecimal getReferralPercent() { return referralPercent; }
        public void setReferralPercent(BigDecimal v) { this.referralPercent = v; }
        public BigDecimal getReferralAmount() { return referralAmount; }
        public void setReferralAmount(BigDecimal v) { this.referralAmount = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
        public String getPartnerName() { return partnerName; }
        public void setPartnerName(String v) { this.partnerName = v; }
    }

    public static class Performer {
        private Long userId, quotes, accepted;
        private String name;
        private BigDecimal ratePercent;
        public Long getUserId() { return userId; }
        public void setUserId(Long v) { this.userId = v; }
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public Long getQuotes() { return quotes; }
        public void setQuotes(Long v) { this.quotes = v; }
        public Long getAccepted() { return accepted; }
        public void setAccepted(Long v) { this.accepted = v; }
        public BigDecimal getRatePercent() { return ratePercent; }
        public void setRatePercent(BigDecimal v) { this.ratePercent = v; }
    }
}