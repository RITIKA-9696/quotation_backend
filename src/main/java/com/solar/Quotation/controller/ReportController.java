package com.solar.Quotation.controller;

import com.solar.Quotation.dto.requestdto.ReportFilterRequestDto;
import com.solar.Quotation.dto.responsedto.ApiResponse;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.dto.responsedto.ReportRowResponseDto;
import com.solar.Quotation.dto.responsedto.ReportSummaryResponseDto;
import com.solar.Quotation.security.RequirePermission;
import com.solar.Quotation.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) { this.service = service; }

    private ReportFilterRequestDto filter(LocalDate from, LocalDate to, String status,
                                          Long salesUserId, Long partnerId,
                                          int page, int size, String sort) {
        ReportFilterRequestDto f = new ReportFilterRequestDto();
        f.setFrom(from); f.setTo(to); f.setStatus(status);
        f.setSalesUserId(salesUserId); f.setPartnerId(partnerId);
        f.setPage(page); f.setSize(size);
        if (sort != null && !sort.isBlank()) f.setSort(sort);
        return f;
    }

    @GetMapping("/summary")
    @RequirePermission("REPORT:VIEW")
    public ResponseEntity<ApiResponse<ReportSummaryResponseDto>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long salesUserId,
            @RequestParam(required = false) Long partnerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                service.getSummary(filter(from, to, status, salesUserId, partnerId, 0, 20, null))));
    }

    @GetMapping("/quotations")
    @RequirePermission("REPORT:VIEW")
    public ResponseEntity<ApiResponse<PageResponse<ReportRowResponseDto.Quotation>>> quotations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long salesUserId,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(service.quotationReport(
                filter(from, to, status, salesUserId, partnerId, page, size, sort), null)));
    }

    @GetMapping("/gst")
    @RequirePermission("REPORT:VIEW")
    public ResponseEntity<ApiResponse<PageResponse<ReportRowResponseDto.Gst>>> gst(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long salesUserId,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(service.gstReport(
                filter(from, to, status, salesUserId, partnerId, page, size, sort), null)));
    }

    @GetMapping("/referral-income")
    @RequirePermission("REPORT:VIEW")
    public ResponseEntity<ApiResponse<PageResponse<ReportRowResponseDto.ReferralIncome>>> referralIncome(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long salesUserId,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(service.referralIncomeReport(
                filter(from, to, status, salesUserId, partnerId, page, size, sort), null)));
    }

    @GetMapping("/top-performers")
    @RequirePermission("REPORT:VIEW")
    public ResponseEntity<ApiResponse<PageResponse<ReportRowResponseDto.Performer>>> topPerformers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long salesUserId,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return ResponseEntity.ok(ApiResponse.ok(service.topPerformers(
                filter(from, to, status, salesUserId, partnerId, page, size, sort), null)));
    }

    @GetMapping("/export")
    @RequirePermission("REPORT:EXPORT")
    public ResponseEntity<byte[]> export(
            @RequestParam String type,
            @RequestParam String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long salesUserId,
            @RequestParam(required = false) Long partnerId) {

        byte[] body = service.export(
                filter(from, to, status, salesUserId, partnerId, 0, 10000, null), type, format);

        boolean excel = "EXCEL".equalsIgnoreCase(format);
        String filename = type.toLowerCase() + "." + (excel ? "xlsx" : "pdf");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(excel
                        ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        : MediaType.APPLICATION_PDF)
                .body(body);
    }
}