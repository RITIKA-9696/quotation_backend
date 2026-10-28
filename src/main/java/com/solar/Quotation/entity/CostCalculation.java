package com.solar.Quotation.entity;

import com.solar.Quotation.Enum.ProjectType;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "cost_calculation",
        indexes = {
                @Index(name = "idx_costcalc_partner", columnList = "channel_partner_id"),
                @Index(name = "idx_costcalc_code", columnList = "code", unique = true)
        }
)
public class CostCalculation extends TenantBaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "project_name", length = 150)
    private String projectName;

    @Column(name = "capacity_kw", nullable = false, precision = 12, scale = 2)
    private BigDecimal capacityKw;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", nullable = false, length = 20)
    private ProjectType projectType;

    @Column(name = "material_cost", precision = 15, scale = 2)       private BigDecimal materialCost = BigDecimal.ZERO;
    @Column(name = "installation_cost", precision = 15, scale = 2)   private BigDecimal installationCost = BigDecimal.ZERO;
    @Column(name = "transportation_cost", precision = 15, scale = 2) private BigDecimal transportationCost = BigDecimal.ZERO;
    @Column(name = "civil_cost", precision = 15, scale = 2)          private BigDecimal civilCost = BigDecimal.ZERO;
    @Column(name = "labour_cost", precision = 15, scale = 2)         private BigDecimal labourCost = BigDecimal.ZERO;
    @Column(name = "electrical_cost", precision = 15, scale = 2)     private BigDecimal electricalCost = BigDecimal.ZERO;
    @Column(name = "other_cost", precision = 15, scale = 2)          private BigDecimal otherCost = BigDecimal.ZERO;
    @Column(name = "discount", precision = 15, scale = 2)            private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 15, scale = 2)            private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "gst_percent", precision = 5, scale = 2)          private BigDecimal gstPercent = BigDecimal.ZERO;
    @Column(name = "gst_amount", precision = 15, scale = 2)          private BigDecimal gstAmount = BigDecimal.ZERO;
    @Column(name = "grand_total", precision = 15, scale = 2)         private BigDecimal grandTotal = BigDecimal.ZERO;
    @Column(name = "cost_per_kw", precision = 15, scale = 2)         private BigDecimal costPerKw = BigDecimal.ZERO;
    @Column(name = "estimated_saving", precision = 15, scale = 2)    private BigDecimal estimatedSaving = BigDecimal.ZERO;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public BigDecimal getCapacityKw() { return capacityKw; }
    public void setCapacityKw(BigDecimal capacityKw) { this.capacityKw = capacityKw; }
    public ProjectType getProjectType() { return projectType; }
    public void setProjectType(ProjectType projectType) { this.projectType = projectType; }
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
    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal v) { this.grandTotal = v; }
    public BigDecimal getCostPerKw() { return costPerKw; }
    public void setCostPerKw(BigDecimal v) { this.costPerKw = v; }
    public BigDecimal getEstimatedSaving() { return estimatedSaving; }
    public void setEstimatedSaving(BigDecimal v) { this.estimatedSaving = v; }
}