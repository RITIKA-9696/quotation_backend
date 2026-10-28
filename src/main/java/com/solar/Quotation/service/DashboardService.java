package com.solar.Quotation.service;

import com.solar.Quotation.dto.responsedto.DashboardResponseDto;
import java.time.LocalDate;

public interface DashboardService {
    DashboardResponseDto getSummary(Long partnerIdFilter);
    DashboardResponseDto getCharts(Long partnerIdFilter, LocalDate from, LocalDate to);
}