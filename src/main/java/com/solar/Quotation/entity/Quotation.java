package com.solar.Quotation.entity;

import com.solar.Quotation.Enum.DiscountType;
import com.solar.Quotation.Enum.QuotationStatus;
import com.solar.Quotation.Enum.ReferralStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "quotation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"channel_partner_id", "code"}),
        indexes = {
                @Index(name = "idx_quotation_partner_created", columnList = "channel_partner_id,created_at"),
                @Index(name = "idx_quotation_partner_status", columnList = "channel_partner_id,status"),
                @Index(name = "idx_quotation_customer", columnList = "customer_id")
        })
public class Quotation extends TenantBaseEntity {

    @Column(nullable = false, length = 30)
    private String code;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "quote_date", nullable = false)
    private LocalDate quoteDate;

    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuotationStatus status = QuotationStatus.PENDING;

    @Column(name = "system_size_kw", nullable = false, precision = 10, scale = 2)
    private BigDecimal systemSizeKw;

    @Column(length = 50)
    private String mounting;

    // ---- extra costs, discount, GST (inputs) ----
    @Column(name = "installation_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal installationCost = BigDecimal.ZERO;

    @Column(name = "transport_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal transportCost = BigDecimal.ZERO;

    @Column(name = "other_label", length = 80)
    private String otherLabel;

    @Column(name = "other_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal otherCost = BigDecimal.ZERO;

    @Column(name = "gst_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal gstPercent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 10)
    private DiscountType discountType = DiscountType.PERCENT;

    @Column(name = "discount_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountValue = BigDecimal.ZERO;

    // ---- savings inputs ----
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal tariff = BigDecimal.ZERO;

    @Column(name = "escalation_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal escalationPercent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal advance = BigDecimal.ZERO;

    // ---- terms ----
    @Column(length = 4000)
    private String terms;

    @Column(name = "pay_advance_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal payAdvancePct;

    @Column(name = "pay_material_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal payMaterialPct;

    @Column(name = "pay_installation_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal payInstallationPct;

    @Column(name = "pay_balance_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal payBalancePct;

    // ---- bank details printed on this quotation (copied from Settings at creation, editable) ----
    @Column(name = "bank_account_name", length = 150)
    private String bankAccountName;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_number", length = 40)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 20)
    private String bankIfsc;

    @Column(name = "bank_branch", length = 100)
    private String bankBranch;

    // ---- referral ----
    @Column(nullable = false)
    private boolean referred;

    @Column(name = "referred_by", length = 150)
    private String referredBy;

    @Column(name = "referrer_type", length = 30)
    private String referrerType;

    @Column(name = "referral_code", length = 30)
    private String referralCode;

    @Column(name = "referral_percent", precision = 5, scale = 2)
    private BigDecimal referralPercent;

    @Column(name = "referral_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal referralAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "referral_status", length = 20)
    private ReferralStatus referralStatus;

    // ---- computed totals (server side) ----
    @Column(name = "items_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal itemsTotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "sgst_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(name = "cgst_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(name = "gst_amount", nullable = false, precision = 14, scale = 2)   // ← NEW
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "balance_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    @Column(name = "subsidy_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal subsidyAmount = BigDecimal.ZERO;

    @Column(name = "net_investment", nullable = false, precision = 14, scale = 2)
    private BigDecimal netInvestment = BigDecimal.ZERO;

    // --- referral payout (added for the referral module) ---
    @Column(name = "referral_paid_date")
    private LocalDate referralPaidDate;

    @Column(name = "referral_payment_ref", length = 60)
    private String referralPaymentRef;

    @Column(name = "referral_payment_remarks", length = 300)
    private String referralPaymentRemarks;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public LocalDate getQuoteDate() { return quoteDate; }
    public void setQuoteDate(LocalDate quoteDate) { this.quoteDate = quoteDate; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public QuotationStatus getStatus() { return status; }
    public void setStatus(QuotationStatus status) { this.status = status; }
    public BigDecimal getSystemSizeKw() { return systemSizeKw; }
    public void setSystemSizeKw(BigDecimal systemSizeKw) { this.systemSizeKw = systemSizeKw; }
    public String getMounting() { return mounting; }
    public void setMounting(String mounting) { this.mounting = mounting; }
    public BigDecimal getInstallationCost() { return installationCost; }
    public void setInstallationCost(BigDecimal installationCost) { this.installationCost = installationCost; }
    public BigDecimal getTransportCost() { return transportCost; }
    public void setTransportCost(BigDecimal transportCost) { this.transportCost = transportCost; }
    public String getOtherLabel() { return otherLabel; }
    public void setOtherLabel(String otherLabel) { this.otherLabel = otherLabel; }
    public BigDecimal getOtherCost() { return otherCost; }
    public void setOtherCost(BigDecimal otherCost) { this.otherCost = otherCost; }
    public BigDecimal getGstPercent() { return gstPercent; }
    public void setGstPercent(BigDecimal gstPercent) { this.gstPercent = gstPercent; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
    public BigDecimal getTariff() { return tariff; }
    public void setTariff(BigDecimal tariff) { this.tariff = tariff; }
    public BigDecimal getEscalationPercent() { return escalationPercent; }
    public void setEscalationPercent(BigDecimal escalationPercent) { this.escalationPercent = escalationPercent; }
    public BigDecimal getAdvance() { return advance; }
    public void setAdvance(BigDecimal advance) { this.advance = advance; }
    public String getTerms() { return terms; }
    public void setTerms(String terms) { this.terms = terms; }
    public BigDecimal getPayAdvancePct() { return payAdvancePct; }
    public void setPayAdvancePct(BigDecimal payAdvancePct) { this.payAdvancePct = payAdvancePct; }
    public BigDecimal getPayMaterialPct() { return payMaterialPct; }
    public void setPayMaterialPct(BigDecimal payMaterialPct) { this.payMaterialPct = payMaterialPct; }
    public BigDecimal getPayInstallationPct() { return payInstallationPct; }
    public void setPayInstallationPct(BigDecimal payInstallationPct) { this.payInstallationPct = payInstallationPct; }
    public BigDecimal getPayBalancePct() { return payBalancePct; }
    public void setPayBalancePct(BigDecimal payBalancePct) { this.payBalancePct = payBalancePct; }
    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }
    public String getBankBranch() { return bankBranch; }
    public void setBankBranch(String bankBranch) { this.bankBranch = bankBranch; }
    public boolean isReferred() { return referred; }
    public void setReferred(boolean referred) { this.referred = referred; }
    public String getReferredBy() { return referredBy; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }
    public String getReferrerType() { return referrerType; }
    public void setReferrerType(String referrerType) { this.referrerType = referrerType; }
    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }
    public BigDecimal getReferralPercent() { return referralPercent; }
    public void setReferralPercent(BigDecimal referralPercent) { this.referralPercent = referralPercent; }
    public BigDecimal getReferralAmount() { return referralAmount; }
    public void setReferralAmount(BigDecimal referralAmount) { this.referralAmount = referralAmount; }
    public ReferralStatus getReferralStatus() { return referralStatus; }
    public void setReferralStatus(ReferralStatus referralStatus) { this.referralStatus = referralStatus; }
    public BigDecimal getItemsTotal() { return itemsTotal; }
    public void setItemsTotal(BigDecimal itemsTotal) { this.itemsTotal = itemsTotal; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(BigDecimal taxableAmount) { this.taxableAmount = taxableAmount; }
    public BigDecimal getSgstAmount() { return sgstAmount; }
    public void setSgstAmount(BigDecimal sgstAmount) { this.sgstAmount = sgstAmount; }
    public BigDecimal getCgstAmount() { return cgstAmount; }
    public void setCgstAmount(BigDecimal cgstAmount) { this.cgstAmount = cgstAmount; }
    public BigDecimal getGstAmount() { return gstAmount; }                    // ← NEW
    public void setGstAmount(BigDecimal gstAmount) { this.gstAmount = gstAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getBalanceAmount() { return balanceAmount; }
    public void setBalanceAmount(BigDecimal balanceAmount) { this.balanceAmount = balanceAmount; }
    public BigDecimal getSubsidyAmount() { return subsidyAmount; }
    public void setSubsidyAmount(BigDecimal subsidyAmount) { this.subsidyAmount = subsidyAmount; }
    public BigDecimal getNetInvestment() { return netInvestment; }
    public void setNetInvestment(BigDecimal netInvestment) { this.netInvestment = netInvestment; }
    // --- getters/setters ---
    public LocalDate getReferralPaidDate() { return referralPaidDate; }
    public void setReferralPaidDate(LocalDate referralPaidDate) { this.referralPaidDate = referralPaidDate; }

    public String getReferralPaymentRef() { return referralPaymentRef; }
    public void setReferralPaymentRef(String referralPaymentRef) { this.referralPaymentRef = referralPaymentRef; }

    public String getReferralPaymentRemarks() { return referralPaymentRemarks; }
    public void setReferralPaymentRemarks(String referralPaymentRemarks) { this.referralPaymentRemarks = referralPaymentRemarks; }
}