package com.solar.Quotation.util;

import com.solar.Quotation.dto.responsedto.AuditInfoResponseDto;
import com.solar.Quotation.entity.BaseEntity;
import com.solar.Quotation.service.UserNameProvider;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class AuditInfoMapper {

    private final UserNameProvider userNameProvider;

    public AuditInfoMapper(UserNameProvider userNameProvider) {
        this.userNameProvider = userNameProvider;
    }

    public Map<Long, String> namesFor(Iterable<? extends BaseEntity> entities) {
        Set<Long> ids = new HashSet<>();
        for (BaseEntity e : entities) {
            if (e.getCreatedBy() != null) ids.add(e.getCreatedBy());
            if (e.getUpdatedBy() != null) ids.add(e.getUpdatedBy());
        }
        return userNameProvider.names(ids);
    }

    public AuditInfoResponseDto of(BaseEntity e, Map<Long, String> names) {
        return new AuditInfoResponseDto(
                e.getCreatedAt(),
                e.getCreatedBy() == null ? null : names.get(e.getCreatedBy()),
                e.getUpdatedAt(),
                e.getUpdatedBy() == null ? null : names.get(e.getUpdatedBy()));
    }
}