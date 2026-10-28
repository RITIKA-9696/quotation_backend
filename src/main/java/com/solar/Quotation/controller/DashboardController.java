package com.solar.Quotation.controller;

import com.solar.Quotation.dto.responsedto.ApiResponse;
import com.solar.Quotation.dto.responsedto.DashboardResponseDto;
import com.solar.Quotation.security.RequirePermission;
import com.solar.Quotation.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/summary")
    @RequirePermission("DASHBOARD:VIEW")
    public ResponseEntity<ApiResponse<DashboardResponseDto>> summary(
            @RequestParam(required = false) Long partnerId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getSummary(partnerId)));
    }

    @GetMapping("/charts")
    @RequirePermission("DASHBOARD:VIEW")
    public ResponseEntity<ApiResponse<DashboardResponseDto>> charts(
            @RequestParam(required = false) Long partnerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(service.getCharts(partnerId, from, to)));
    }
}