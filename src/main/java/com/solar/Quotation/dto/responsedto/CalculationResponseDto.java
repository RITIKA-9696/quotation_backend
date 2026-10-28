package com.solar.Quotation.dto.responsedto;

import com.solar.Quotation.Enum.ProjectType;

import java.math.BigDecimal;
import java.time.Instant;

public class CalculationResponseDto {
    private Long id;
    private String code;
    private PartnerRef partner;
    private String customerName;
    private String projectName;
    private BigDecimal capacityKw;
    private ProjectType projectType;
    private Costs costs;
    private Result result;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public PartnerRef getPartner() { return partner; }
    public void setPartner(PartnerRef v) { this.partner = v; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String v) { this.projectName = v; }
    public BigDecimal getCapacityKw() { return capacityKw; }
    public void setCapacityKw(BigDecimal v) { this.capacityKw = v; }
    public ProjectType getProjectType() { return projectType; }
    public void setProjectType(ProjectType v) { this.projectType = v; }
    public Costs getCosts() { return costs; }
    public void setCosts(Costs v) { this.costs = v; }
    public Result getResult() { return result; }
    public void setResult(Result v) { this.result = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }

    public static class PartnerRef {
        private Long id;
        private String name;
        public Long getId() { return id; }
        public void setId(Long v) { this.id = v; }
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
    }

    public static class Costs {
        private BigDecimal material, installation, transportation, civil, labour, electrical, other, discount;
        public BigDecimal getMaterial() { return material; }
        public void setMaterial(BigDecimal v) { this.material = v; }
        public BigDecimal getInstallation() { return installation; }
        public void setInstallation(BigDecimal v) { this.installation = v; }
        public BigDecimal getTransportation() { return transportation; }
        public void setTransportation(BigDecimal v) { this.transportation = v; }
        public BigDecimal getCivil() { return civil; }
        public void setCivil(BigDecimal v) { this.civil = v; }
        public BigDecimal getLabour() { return labour; }
        public void setLabour(BigDecimal v) { this.labour = v; }
        public BigDecimal getElectrical() { return electrical; }
        public void setElectrical(BigDecimal v) { this.electrical = v; }
        public BigDecimal getOther() { return other; }
        public void setOther(BigDecimal v) { this.other = v; }
        public BigDecimal getDiscount() { return discount; }
        public void setDiscount(BigDecimal v) { this.discount = v; }
    }

    public static class Result {
        private BigDecimal subtotal, gstPercent, gstAmount, grandTotal, costPerKw, estimatedSaving;
        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal v) { this.subtotal = v; }
        public BigDecimal getGstPercent() { return gstPercent; }
        public void setGstPercent(BigDecimal v) { this.gstPercent = v; }
        public BigDecimal getGstAmount() { return gstAmount; }
        public void setGstAmount(BigDecimal v) { this.gstAmount = v; }
        public BigDecimal getGrandTotal() { return grandTotal; }
        public void setGrandTotal(BigDecimal v) { this.grandTotal = v; }
        public BigDecimal getCostPerKw() { return costPerKw; }
        public void setCostPerKw(BigDecimal v) { this.costPerKw = v; }
        public BigDecimal getEstimatedSaving() { return estimatedSaving; }
        public void setEstimatedSaving(BigDecimal v) { this.estimatedSaving = v; }
    }
}