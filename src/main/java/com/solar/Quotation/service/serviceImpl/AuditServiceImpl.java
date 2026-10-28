package com.solar.Quotation.service.serviceImpl;
import com.solar.Quotation.Enum.AuditAction;
import com.solar.Quotation.dto.requestdto.AuditFilterRequestDto;
import com.solar.Quotation.dto.responsedto.AuditLogResponseDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.dto.responsedto.PartnerRefResponseDto;
import com.solar.Quotation.entity.AuditLog;
import com.solar.Quotation.exception.BusinessException;
import com.solar.Quotation.repository.AuditLogRepository;
import com.solar.Quotation.security.CurrentUser;
import com.solar.Quotation.security.TenantContext;
import com.solar.Quotation.security.TenantResolver;
import com.solar.Quotation.service.AuditService;
import com.solar.Quotation.service.PartnerRefProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "module", "action", "actorName");
    private static final LocalDateTime MIN_TS = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime MAX_TS = LocalDateTime.of(2100, 1, 1, 0, 0);

    private final AuditLogRepository repository;
    private final TenantResolver tenantResolver;
    private final PartnerRefProvider partnerRefProvider;
    private final ObjectMapper objectMapper;

    public AuditServiceImpl(AuditLogRepository repository, TenantResolver tenantResolver,
                            PartnerRefProvider partnerRefProvider, ObjectMapper objectMapper) {
        this.repository = repository;
        this.tenantResolver = tenantResolver;
        this.partnerRefProvider = partnerRefProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void record(AuditAction action, String module, String entityType, Long entityId,
                       String entityLabel, Long partnerId, List<Map<String, String>> changes) {
        recordAs(TenantContext.get(), action, module, entityType, entityId, entityLabel, partnerId, changes);
    }

    @Override
    @Transactional
    public void recordAs(CurrentUser actor, AuditAction action, String module, String entityType, Long entityId,
                         String entityLabel, Long partnerId, List<Map<String, String>> changes) {
        AuditLog a = new AuditLog();
        a.setChannelPartnerId(partnerId);
        if (actor != null) {
            a.setActorUserId(actor.getUserId());
            a.setActorName(actor.getName());
            a.setActorRole(actor.getRoleCode());
            a.setActorPartnerId(actor.getPartnerId());
            a.setCreatedBy(actor.getUserId());
        } else {
            a.setActorName("SYSTEM");
        }
        a.setModule(module);
        a.setEntityType(entityType);
        a.setEntityId(entityId);
        a.setEntityLabel(entityLabel);
        a.setAction(action);
        a.setChangesJson(toJson(changes));
        HttpServletRequest req = currentRequest();
        if (req != null) {
            a.setIpAddress(clientIp(req));
            String ua = req.getHeader("User-Agent");
            a.setUserAgent(ua == null ? null : ua.substring(0, Math.min(ua.length(), 255)));
        }
        repository.save(a);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponseDto> search(AuditFilterRequestDto f) {
        Long partnerId = tenantResolver.effectivePartnerId(f.getPartnerId());
        Pageable pageable = f.toPageable("createdAt,desc", SORT_FIELDS);
        Page<AuditLog> page = runSearch(f, partnerId, pageable);
        Map<Long, PartnerRefResponseDto> partners = partnerRefProvider.refs(
                new HashSet<>(page.getContent().stream().map(AuditLog::getChannelPartnerId).toList()));
        return PageResponse.of(page.map(a -> toResponse(a, true, partners)));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(AuditFilterRequestDto f) {
        Long partnerId = tenantResolver.effectivePartnerId(f.getPartnerId());
        Pageable pageable = PageRequest.of(0, 5000, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AuditLog> rows = runSearch(f, partnerId, pageable).getContent();
        Map<Long, PartnerRefResponseDto> partners = partnerRefProvider.refs(
                new HashSet<>(rows.stream().map(AuditLog::getChannelPartnerId).toList()));
        StringBuilder sb = new StringBuilder("Time (UTC),Partner,Actor,Role,Module,Action,Entity,Label,IP\n");
        for (AuditLog a : rows) {
            PartnerRefResponseDto p = partners.get(a.getChannelPartnerId());
            sb.append(csv(String.valueOf(a.getCreatedAt()))).append(',')
                    .append(csv(p == null ? "" : p.getName())).append(',')
                    .append(csv(a.getActorName())).append(',')
                    .append(csv(a.getActorRole())).append(',')
                    .append(csv(a.getModule())).append(',')
                    .append(csv(String.valueOf(a.getAction()))).append(',')
                    .append(csv(a.getEntityType())).append(',')
                    .append(csv(a.getEntityLabel())).append(',')
                    .append(csv(a.getIpAddress())).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> entityHistory(String entityType, Long entityId) {
        Long partnerId = tenantResolver.effectivePartnerId(null);
        boolean full = TenantContext.get().isSuperAdmin();
        List<AuditLog> rows = repository.findHistory(partnerId, entityType, entityId, PageRequest.of(0, 200));
        Map<Long, PartnerRefResponseDto> partners = partnerRefProvider.refs(
                new HashSet<>(rows.stream().map(AuditLog::getChannelPartnerId).toList()));
        return rows.stream().map(a -> toResponse(a, full, partners)).toList();
    }

    // ---------------- helpers ----------------

    private Page<AuditLog> runSearch(AuditFilterRequestDto f, Long partnerId, Pageable pageable) {
        AuditAction action = null;
        if (f.getAction() != null && !f.getAction().isBlank()) {
            try {
                action = AuditAction.valueOf(f.getAction().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("VALIDATION_ERROR", "Unknown action " + f.getAction());
            }
        }
        LocalDateTime from = f.getFrom() == null ? MIN_TS : f.getFrom().atStartOfDay();
        LocalDateTime to = f.getTo() == null ? MAX_TS : f.getTo().plusDays(1).atStartOfDay();
        String module = blankToNull(f.getModule());
        String entityType = blankToNull(f.getEntityType());
        return repository.search(partnerId, f.getUserId(), module == null ? null : module.toUpperCase(),
                action, entityType, f.getEntityId(), from, to, f.getSearchOrNull(), pageable);
    }

    private AuditLogResponseDto toResponse(AuditLog a, boolean full, Map<Long, PartnerRefResponseDto> partners) {
        AuditLogResponseDto d = new AuditLogResponseDto();
        d.setId(a.getId());
        d.setCreatedAt(a.getCreatedAt());
        d.setActorName(a.getActorName());
        d.setAction(a.getAction().name());
        d.setModule(a.getModule());
        d.setEntityType(a.getEntityType());
        d.setEntityId(a.getEntityId());
        d.setEntityLabel(a.getEntityLabel());
        d.setPartner(partners.get(a.getChannelPartnerId()));
        if (full) {
            d.setActorUserId(a.getActorUserId());
            d.setActorRole(a.getActorRole());
            d.setActorPartnerId(a.getActorPartnerId());
            d.setIpAddress(a.getIpAddress());
            d.setUserAgent(a.getUserAgent());
            d.setChanges(parse(a.getChangesJson()));
        }
        return d;
    }

    private String toJson(List<Map<String, String>> changes) {
        if (changes == null || changes.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (Exception ex) {
            log.warn("Could not serialise audit changes", ex);
            return null;
        }
    }

    private Object parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

    private String csv(String v) {
        if (v == null) return "";
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
