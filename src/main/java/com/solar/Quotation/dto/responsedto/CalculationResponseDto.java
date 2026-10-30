package com.solar.Quotation.dto.responsedto;

import com.solar.Quotation.Enum.ProjectType;

import java.math.BigDecimal;
import java.time.Instant;

public class CalculationResponseDto {

    private Long id;
    private String code;

    // partner — frontend reads rec.partnerName (table + exports) and rec.partner?.name
    private Long partnerId;
    private String partnerName;

    private String customerName;
    private String projectName;
    private BigDecimal capacityKw;
    private ProjectType projectType;

    // costs (flat — matches calc.js res.materialCost, res.installationCost, ...)
    private BigDecimal materialCost;
    private BigDecimal installationCost;
    private BigDecimal transportationCost;
    private BigDecimal civilCost;
    private BigDecimal labourCost;
    private BigDecimal electricalCost;
    private BigDecimal otherCost;
    private BigDecimal discount;

    // result (flat — matches calc.js res.subtotal, res.gstAmount, res.finalCost, ...)
    private BigDecimal subtotal;
    private BigDecimal gstPercent;
    private BigDecimal gstAmount;
    private BigDecimal finalCost;          // was grandTotal
    private BigDecimal costPerKw;
    private BigDecimal estimatedSaving;

    private Instant createdAt;

    // ---- getters / setters ----
    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long v) { this.partnerId = v; }
    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String v) { this.partnerName = v; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String v) { this.projectName = v; }
    public BigDecimal getCapacityKw() { return capacityKw; }
    public void setCapacityKw(BigDecimal v) { this.capacityKw = v; }
    public ProjectType getProjectType() { return projectType; }
    public void setProjectType(ProjectType v) { this.projectType = v; }
    public BigDecimal getMaterialCost() { return materialCost; }
    public void setMaterialCost(BigDecimal v) { this.materialCost = v; }
    public BigDecimal getInstallationCost() { return installationCost; }
    public void setInstallationCost(BigDecimal v) { this.installationCost = v; }
    public BigDecimal getTransportationCost() { return transportationCost; }
    public void setTransportationCost(BigDecimal v) { this.transportationCost = v; }
    public BigDecimal getCivilCost() { return civilCost; }
    public void setCivilCost(BigDecimal v) { this.civilCost = v; }
    public BigDecimal getLabourCost() { return labourCost; }
    public void setLabourCost(BigDecimal v) { this.labourCost = v; }
    public BigDecimal getElectricalCost() { return electricalCost; }
    public void setElectricalCost(BigDecimal v) { this.electricalCost = v; }
    public BigDecimal getOtherCost() { return otherCost; }
    public void setOtherCost(BigDecimal v) { this.otherCost = v; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal v) { this.discount = v; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal v) { this.subtotal = v; }
    public BigDecimal getGstPercent() { return gstPercent; }
    public void setGstPercent(BigDecimal v) { this.gstPercent = v; }
    public BigDecimal getGstAmount() { return gstAmount; }
    public void setGstAmount(BigDecimal v) { this.gstAmount = v; }
    public BigDecimal getFinalCost() { return finalCost; }
    public void setFinalCost(BigDecimal v) { this.finalCost = v; }
    public BigDecimal getCostPerKw() { return costPerKw; }
    public void setCostPerKw(BigDecimal v) { this.costPerKw = v; }
    public BigDecimal getEstimatedSaving() { return estimatedSaving; }
    public void setEstimatedSaving(BigDecimal v) { this.estimatedSaving = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}