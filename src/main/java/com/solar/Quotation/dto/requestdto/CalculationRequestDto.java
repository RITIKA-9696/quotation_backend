package com.solar.Quotation.dto.requestdto;

import com.solar.Quotation.Enum.ProjectType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class CalculationRequestDto {

    private Long partnerId;

    @NotBlank @Size(max = 150)
    private String customerName;

    @Size(max = 150)
    private String projectName;

    @NotNull @DecimalMin("0.01")
    private BigDecimal capacityKw;

    @NotNull
    private ProjectType projectType;

    @DecimalMin("0") private BigDecimal materialCost;
    @DecimalMin("0") private BigDecimal installationCost;
    @DecimalMin("0") private BigDecimal transportationCost;
    @DecimalMin("0") private BigDecimal civilCost;
    @DecimalMin("0") private BigDecimal labourCost;
    @DecimalMin("0") private BigDecimal electricalCost;
    @DecimalMin("0") private BigDecimal otherCost;
    @DecimalMin("0") private BigDecimal discount;

    @NotNull @DecimalMin("0") @DecimalMax("100")
    private BigDecimal gstPercent;

    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long v) { this.partnerId = v; }
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
    public BigDecimal getGstPercent() { return gstPercent; }
    public void setGstPercent(BigDecimal v) { this.gstPercent = v; }
}