package com.solar.Quotation.dto.requestdto;

import com.solar.Quotation.Enum.ProjectType;

public class CalculationFilterRequestDto {
    private String search;
    private ProjectType projectType;
    private Long partnerId;
    private int page = 0;
    private int size = 20;

    public String getSearch() { return search; }
    public void setSearch(String v) { this.search = v; }
    public ProjectType getProjectType() { return projectType; }
    public void setProjectType(ProjectType v) { this.projectType = v; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long v) { this.partnerId = v; }
    public int getPage() { return page; }
    public void setPage(int v) { this.page = v; }
    public int getSize() { return size; }
    public void setSize(int v) { this.size = v; }
}