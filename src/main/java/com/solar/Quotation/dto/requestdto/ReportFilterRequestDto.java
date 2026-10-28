package com.solar.Quotation.dto.requestdto;

import java.time.LocalDate;

public class ReportFilterRequestDto {
    private LocalDate from;
    private LocalDate to;
    private String status;
    private Long salesUserId;
    private Long partnerId;
    private int page = 0;
    private int size = 20;
    private String sort = "createdAt,desc";

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate v) { this.from = v; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate v) { this.to = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Long getSalesUserId() { return salesUserId; }
    public void setSalesUserId(Long v) { this.salesUserId = v; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long v) { this.partnerId = v; }
    public int getPage() { return page; }
    public void setPage(int v) { this.page = v; }
    public int getSize() { return size; }
    public void setSize(int v) { this.size = v; }
    public String getSort() { return sort; }
    public void setSort(String v) { this.sort = v; }
}