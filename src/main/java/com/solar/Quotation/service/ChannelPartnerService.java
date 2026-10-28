package com.solar.Quotation.service;

import com.solar.Quotation.dto.requestdto.*;
import com.solar.Quotation.dto.responsedto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChannelPartnerService {

    PageResponse<ChannelPartnerResponseDto> list(ChannelPartnerFilterRequestDto filter);


    List<PartnerOptionResponseDto> options();

    ChannelPartnerResponseDto get(Long id);

    /** Creates the partner and its first CHANNEL_PARTNER_ADMIN user in one transaction. */
    ChannelPartnerResponseDto create(ChannelPartnerRequestDto req);

    ChannelPartnerResponseDto update(Long id, ChannelPartnerRequestDto req);

    ChannelPartnerResponseDto updateStatus(Long id, ChannelPartnerStatusRequestDto req);

    ChannelPartnerStatsResponseDto stats(Long id);

    PageResponse<UserResponseDto> users(Long id, UserFilterRequestDto filter);

    com.solar.Quotation.dto.responsedto.PageResponse<com.solar.Quotation.dto.responsedto.ReferralActivityResponseDto>
    getReferralActivity(Long partnerId, String status, org.springframework.data.domain.Pageable pageable);

    void markReferralPaid(Long partnerId, Long quotationId,
                          com.solar.Quotation.dto.requestdto.ReferralMarkPaidRequestDto request);
}