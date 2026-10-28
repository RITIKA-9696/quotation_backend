package com.solar.Quotation.controller;

import com.solar.Quotation.dto.requestdto.CalculationFilterRequestDto;
import com.solar.Quotation.dto.requestdto.CalculationRequestDto;
import com.solar.Quotation.dto.responsedto.ApiResponse;
import com.solar.Quotation.dto.responsedto.CalculationResponseDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.security.RequirePermission;
import com.solar.Quotation.service.CalculatorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/calculations")
public class CalculatorController {

    private final CalculatorService service;

    public CalculatorController(CalculatorService service) { this.service = service; }

    @PostMapping("/preview")
    @RequirePermission("CALCULATOR:VIEW")
    public ResponseEntity<ApiResponse<CalculationResponseDto>> preview(
            @Valid @RequestBody CalculationRequestDto req) {
        return ResponseEntity.ok(ApiResponse.ok(service.preview(req)));
    }

    @GetMapping
    @RequirePermission("CALCULATOR:VIEW")
    public ResponseEntity<ApiResponse<PageResponse<CalculationResponseDto>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String projectType,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        CalculationFilterRequestDto f = new CalculationFilterRequestDto();
        f.setSearch(search);
        f.setPartnerId(partnerId);
        if (projectType != null && !projectType.isBlank()) {
            f.setProjectType(com.solar.Quotation.Enum.ProjectType.valueOf(
                    projectType.trim().toUpperCase(Locale.ROOT)));
        }
        return ResponseEntity.ok(ApiResponse.ok(service.list(
                f, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }

    @GetMapping("/{id}")
    @RequirePermission("CALCULATOR:VIEW")
    public ResponseEntity<ApiResponse<CalculationResponseDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    @PostMapping
    @RequirePermission("CALCULATOR:CREATE")
    public ResponseEntity<ApiResponse<CalculationResponseDto>> create(
            @Valid @RequestBody CalculationRequestDto req) {
        return ResponseEntity.ok(ApiResponse.ok(service.create(req), "Calculation created"));
    }

    @PutMapping("/{id}")
    @RequirePermission("CALCULATOR:EDIT")
    public ResponseEntity<ApiResponse<CalculationResponseDto>> update(
            @PathVariable Long id, @Valid @RequestBody CalculationRequestDto req) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req), "Calculation updated"));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("CALCULATOR:DELETE")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Calculation deleted"));
    }
}