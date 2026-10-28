package com.solar.Quotation.service;

import com.solar.Quotation.dto.requestdto.ReportFilterRequestDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.dto.responsedto.ReportRowResponseDto;
import com.solar.Quotation.dto.responsedto.ReportSummaryResponseDto;
import org.springframework.data.domain.Pageable;

public interface ReportService {
    ReportSummaryResponseDto getSummary(ReportFilterRequestDto f);
    PageResponse<ReportRowResponseDto.Quotation> quotationReport(ReportFilterRequestDto f, Pageable p);
    PageResponse<ReportRowResponseDto.Gst> gstReport(ReportFilterRequestDto f, Pageable p);
    PageResponse<ReportRowResponseDto.ReferralIncome> referralIncomeReport(ReportFilterRequestDto f, Pageable p);
    PageResponse<ReportRowResponseDto.Performer> topPerformers(ReportFilterRequestDto f, Pageable p);
    byte[] export(ReportFilterRequestDto f, String type, String format);
}