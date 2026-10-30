package com.solar.Quotation.service;


import com.solar.Quotation.dto.requestdto.CalculationFilterRequestDto;
import com.solar.Quotation.dto.requestdto.CalculationRequestDto;
import com.solar.Quotation.dto.responsedto.CalculationResponseDto;
import com.solar.Quotation.dto.responsedto.CustomerResponseDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.dto.responsedto.ProjectResponseDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CalculatorService {
    CalculationResponseDto preview(CalculationRequestDto req);
    CalculationResponseDto create(CalculationRequestDto req);
    CalculationResponseDto update(Long id, CalculationRequestDto req);
    CalculationResponseDto getById(Long id);
    PageResponse<CalculationResponseDto> list(CalculationFilterRequestDto filter, Pageable pageable);
    void delete(Long id);
    List<CustomerResponseDto> getAllCustomers();
    List<ProjectResponseDto> getAllProjects();
}
