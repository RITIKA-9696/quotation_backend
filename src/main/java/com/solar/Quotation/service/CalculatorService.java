package com.solar.Quotation.service;

import com.solar.Quotation.dto.requestdto.CalculationFilterRequestDto;
import com.solar.Quotation.dto.requestdto.CalculationRequestDto;
import com.solar.Quotation.dto.responsedto.CalculationResponseDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CalculatorService {
    CalculationResponseDto preview(CalculationRequestDto req);
    CalculationResponseDto create(CalculationRequestDto req);
    CalculationResponseDto update(Long id, CalculationRequestDto req);
    CalculationResponseDto getById(Long id);
    PageResponse<CalculationResponseDto> list(CalculationFilterRequestDto filter, Pageable pageable);
    void delete(Long id);
}