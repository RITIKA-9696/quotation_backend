package com.solar.Quotation.dto.requestdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class ReferralMarkPaidRequestDto {
    @NotNull private LocalDate paidDate;
    @NotBlank private String paymentRef;
    private String remarks;

    public LocalDate getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDate v) { this.paidDate = v; }
    public String getPaymentRef() { return paymentRef; }
    public void setPaymentRef(String v) { this.paymentRef = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { this.remarks = v; }
}