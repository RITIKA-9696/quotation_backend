package com.solar.Quotation.service.serviceImpl;

import com.solar.Quotation.Enum.AuditAction;
import com.solar.Quotation.Enum.DataScope;
import com.solar.Quotation.Enum.PartnerType;
import com.solar.Quotation.Enum.RecordStatus;
import com.solar.Quotation.dto.requestdto.AdminResetPasswordRequestDto;
import com.solar.Quotation.dto.requestdto.UserFilterRequestDto;
import com.solar.Quotation.dto.requestdto.UserRequestDto;
import com.solar.Quotation.dto.requestdto.UserStatusRequestDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.dto.responsedto.PartnerRefResponseDto;
import com.solar.Quotation.dto.responsedto.RoleRefResponseDto;
import com.solar.Quotation.dto.responsedto.UserResponseDto;
import com.solar.Quotation.dto.responsedto.UserSummaryResponseDto;
import com.solar.Quotation.entity.AppUser;
import com.solar.Quotation.entity.ChannelPartner;
import com.solar.Quotation.entity.Role;
import com.solar.Quotation.exception.BusinessException;
import com.solar.Quotation.exception.ResourceNotFoundException;
import com.solar.Quotation.repository.AppUserRepository;
import com.solar.Quotation.repository.ChannelPartnerRepository;
import com.solar.Quotation.repository.RefreshTokenRepository;
import com.solar.Quotation.repository.RoleRepository;
import com.solar.Quotation.security.TenantContext;
import com.solar.Quotation.security.TenantResolver;
import com.solar.Quotation.service.AuditService;
import com.solar.Quotation.service.PartnerRefProvider;
import com.solar.Quotation.service.UserService;
import com.solar.Quotation.util.AuditInfoMapper;
import com.solar.Quotation.util.CodeGenerator;
import com.solar.Quotation.util.DiffUtil;
import com.solar.Quotation.util.TokenUtil;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "name", "username", "status");
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String PARTNER_ADMIN = "CHANNEL_PARTNER_ADMIN";
    private static final String PARTNER_SALES = "CHANNEL_PARTNER_SALES";
    private static final Pattern PASSWORD_RULE = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,100}$");

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ChannelPartnerRepository partnerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TenantResolver tenantResolver;
    private final PartnerRefProvider partnerRefProvider;
    private final AuditInfoMapper auditInfoMapper;
    private final AuditService auditService;
    private final CodeGenerator codeGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TokenUtil tokenUtil;

    public UserServiceImpl(AppUserRepository userRepository, RoleRepository roleRepository,
                           ChannelPartnerRepository partnerRepository, RefreshTokenRepository refreshTokenRepository,
                           TenantResolver tenantResolver, PartnerRefProvider partnerRefProvider,
                           AuditInfoMapper auditInfoMapper, AuditService auditService, CodeGenerator codeGenerator,
                           PasswordEncoder passwordEncoder, TokenUtil tokenUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.partnerRepository = partnerRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tenantResolver = tenantResolver;
        this.partnerRefProvider = partnerRefProvider;
        this.auditInfoMapper = auditInfoMapper;
        this.auditService = auditService;
        this.codeGenerator = codeGenerator;
        this.passwordEncoder = passwordEncoder;
        this.tokenUtil = tokenUtil;
    }

    // ---------------------------------------------------------------- read

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponseDto> list(UserFilterRequestDto f) {
        Long partnerId = tenantResolver.effectivePartnerId(f.getPartnerId());
        Long roleId = resolveRoleId(f.getRoleCode());
        RecordStatus status = parseStatus(f.getStatus());
        Page<AppUser> page = userRepository.search(partnerId, roleId, status, f.getSearchOrNull(),
                f.toPageable("createdAt,desc", SORT_FIELDS));
        Lookup lookup = lookup(page.getContent());
        return PageResponse.of(page.map(u -> toResponse(u, lookup)));
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponseDto summary() {
        Long partnerId = tenantResolver.effectivePartnerId(null);
        UserSummaryResponseDto s = new UserSummaryResponseDto();
        s.setTotal(userRepository.countFiltered(partnerId, null));
        s.setActive(userRepository.countFiltered(partnerId, RecordStatus.ACTIVE));
        s.setInactive(userRepository.countFiltered(partnerId, RecordStatus.INACTIVE));
        s.setSuperAdmins(userRepository.countByRoleCode(SUPER_ADMIN, partnerId, null));
        s.setPartnerAdmins(userRepository.countByRoleCode(PARTNER_ADMIN, partnerId, null));
        s.setPartnerSales(userRepository.countByRoleCode(PARTNER_SALES, partnerId, null));
        s.setAdmins(s.getSuperAdmins() + s.getPartnerAdmins());
        return s;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto get(Long id) {
        return toSingleResponse(findUser(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleRefResponseDto> roleOptions() {
        boolean superAdmin = TenantContext.get().isSuperAdmin();
        return roleRepository.findByDeletedFalseOrderByIdAsc().stream()
                .filter(r -> r.getStatus() == RecordStatus.ACTIVE)
                .filter(r -> superAdmin || r.getDataScope() != DataScope.ALL)
                .map(this::roleRef)
                .toList();
    }

    // ---------------------------------------------------------------- write

    @Override
    @Transactional
    public UserResponseDto create(UserRequestDto req) {
        Long partnerId = tenantResolver.requirePartnerIdForCreate(req.getPartnerId());
        ChannelPartner partner = partnerRepository.findByIdAndDeletedFalse(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel partner not found"));
        Role role = roleRepository.findByIdAndDeletedFalse(req.getRoleId())
                .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "Role not found"));
        assertRoleAllowed(role, partner);

        String username = req.getUsername().trim();
        String email = req.getEmail().trim();
        assertUnique(username, email);

        boolean generated = req.getPassword() == null || req.getPassword().isBlank();
        String rawPassword = generated ? tokenUtil.generateTemporaryPassword() : validatedPassword(req.getPassword());

        AppUser u = new AppUser();
        u.setChannelPartnerId(partnerId);
        u.setCode(codeGenerator.next("USR", partnerId));
        u.setName(req.getName().trim());
        u.setEmail(email);
        u.setPhone(blankToNull(req.getPhone()));
        u.setUsername(username);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setDepartment(blankToNull(req.getDepartment()));
        u.setPhotoPath(blankToNull(req.getPhotoPath()));
        u.setStatus(parseStatus(req.getStatus()) == null ? RecordStatus.ACTIVE : parseStatus(req.getStatus()));
        u.setRoleId(role.getId());
        u.setMustChangePassword(true);
        AppUser saved = userRepository.save(u);

        auditService.record(AuditAction.CREATE, "USER", "AppUser", saved.getId(), saved.getCode(), partnerId,
                DiffUtil.diff(null, snapshot(saved, role)));

        UserResponseDto out = toSingleResponse(saved);
        if (generated) {
            out.setTemporaryPassword(rawPassword);
        }
        return out;
    }

    @Override
    @Transactional
    public UserResponseDto update(Long id, UserRequestDto req) {
        AppUser u = findUser(id);
        Role oldRole = roleRepository.findById(u.getRoleId()).orElseThrow();
        Role newRole = oldRole;

        if (!u.getRoleId().equals(req.getRoleId())) {
            ChannelPartner partner = partnerRepository.findByIdAndDeletedFalse(u.getChannelPartnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Channel partner not found"));
            newRole = roleRepository.findByIdAndDeletedFalse(req.getRoleId())
                    .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "Role not found"));
            assertRoleAllowed(newRole, partner);
            guardNotSelf(u, "change your own role");
            guardLastSuperAdmin(u, oldRole);
        }

        String email = req.getEmail().trim();
        if (!email.equalsIgnoreCase(u.getEmail())) {
            assertUnique(null, email);
        }

        Map<String, Object> before = snapshot(u, oldRole);
        u.setName(req.getName().trim());
        u.setEmail(email);
        u.setPhone(blankToNull(req.getPhone()));
        u.setDepartment(blankToNull(req.getDepartment()));
        u.setPhotoPath(blankToNull(req.getPhotoPath()));
        u.setRoleId(newRole.getId());
        AppUser saved = userRepository.save(u);

        auditService.record(AuditAction.UPDATE, "USER", "AppUser", saved.getId(), saved.getCode(),
                saved.getChannelPartnerId(), DiffUtil.diff(before, snapshot(saved, newRole)));
        return toSingleResponse(saved);
    }

    @Override
    @Transactional
    public UserResponseDto updateStatus(Long id, UserStatusRequestDto req) {
        AppUser u = findUser(id);
        RecordStatus newStatus = RecordStatus.valueOf(req.getStatus());
        Role role = roleRepository.findById(u.getRoleId()).orElseThrow();

        if (newStatus == RecordStatus.INACTIVE && u.getStatus() == RecordStatus.ACTIVE) {
            guardNotSelf(u, "deactivate your own account");
            guardLastSuperAdmin(u, role);
        }
        Map<String, Object> before = snapshot(u, role);
        u.setStatus(newStatus);
        AppUser saved = userRepository.save(u);
        if (newStatus == RecordStatus.INACTIVE) {
            refreshTokenRepository.revokeAllForUser(saved.getId());
        }
        auditService.record(AuditAction.STATUS_CHANGE, "USER", "AppUser", saved.getId(), saved.getCode(),
                saved.getChannelPartnerId(), DiffUtil.diff(before, snapshot(saved, role)));
        return toSingleResponse(userRepository.findById(saved.getId()).orElse(saved));
    }

    @Override
    @Transactional
    public UserResponseDto resetPassword(Long id, AdminResetPasswordRequestDto req) {
        AppUser u = findUser(id);
        boolean generated = req.getNewPassword() == null || req.getNewPassword().isBlank();
        String rawPassword = generated ? tokenUtil.generateTemporaryPassword() : validatedPassword(req.getNewPassword());

        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setMustChangePassword(true);
        userRepository.saveAndFlush(u);
        refreshTokenRepository.revokeAllForUser(u.getId());      // all devices of that user must log in again

        auditService.record(AuditAction.PASSWORD_RESET, "USER", "AppUser", u.getId(), u.getCode(),
                u.getChannelPartnerId(), null);

        UserResponseDto out = toSingleResponse(userRepository.findById(u.getId()).orElse(u));
        if (generated) {
            out.setTemporaryPassword(rawPassword);
        }
        return out;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AppUser u = findUser(id);
        Role role = roleRepository.findById(u.getRoleId()).orElseThrow();
        guardNotSelf(u, "delete your own account");
        guardLastSuperAdmin(u, role);

        String code = u.getCode();
        Long partnerId = u.getChannelPartnerId();
        Long userId = u.getId();
        // free the username and e-mail so they can be used again (both are unique in the table)
        u.setUsername(u.getUsername() + "~del" + u.getId());
        u.setEmail(u.getEmail() + "~del" + u.getId());
        u.setStatus(RecordStatus.INACTIVE);
        u.setDeleted(true);
        userRepository.saveAndFlush(u);
        refreshTokenRepository.revokeAllForUser(userId);

        auditService.record(AuditAction.DELETE, "USER", "AppUser", userId, code, partnerId, null);
    }

    // ---------------------------------------------------------------- helpers

    private AppUser findUser(Long id) {
        Long partnerId = tenantResolver.effectivePartnerId(null);
        return userRepository.findOne(id, partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /** A role of data scope ALL is a platform role: it can only belong to the PLATFORM partner, and the reverse. */
    private void assertRoleAllowed(Role role, ChannelPartner partner) {
        if (role.getStatus() != RecordStatus.ACTIVE) {
            throw new BusinessException("BUSINESS_RULE", "Role is not active");
        }
        boolean platformRole = role.getDataScope() == DataScope.ALL;
        boolean platformPartner = partner.getPartnerType() == PartnerType.PLATFORM;
        if (platformRole != platformPartner) {
            throw new BusinessException("BUSINESS_RULE", platformRole
                    ? "Super Admin roles can only be given to platform users"
                    : "Partner roles cannot be given to platform users");
        }
    }

    private void assertUnique(String username, String email) {
        if (username != null && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("DUPLICATE", "Username already exists", HttpStatus.CONFLICT);
        }
        if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
            // on update the caller only checks when the e-mail actually changed
            throw new BusinessException("DUPLICATE", "Email already exists", HttpStatus.CONFLICT);
        }
    }

    private void guardNotSelf(AppUser u, String what) {
        if (u.getId().equals(TenantContext.get().getUserId())) {
            throw new BusinessException("BUSINESS_RULE", "You cannot " + what);
        }
    }

    private void guardLastSuperAdmin(AppUser u, Role role) {
        if (SUPER_ADMIN.equals(role.getCode()) && u.getStatus() == RecordStatus.ACTIVE
                && userRepository.countByRoleCode(SUPER_ADMIN, null, RecordStatus.ACTIVE) <= 1) {
            throw new BusinessException("BUSINESS_RULE", "The last active Super Admin cannot be changed this way");
        }
    }

    private String validatedPassword(String raw) {
        if (!PASSWORD_RULE.matcher(raw).matches()) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Password must be at least 8 characters and contain a letter and a digit");
        }
        return raw;
    }

    private Long resolveRoleId(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        return roleRepository.findByCodeAndDeletedFalse(roleCode.trim().toUpperCase(Locale.ROOT))
                .map(Role::getId).orElse(-1L);       // unknown code: matches nobody
    }

    private RecordStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return RecordStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("VALIDATION_ERROR", "Unknown status " + value);
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private RoleRefResponseDto roleRef(Role r) {
        return new RoleRefResponseDto(r.getId(), r.getCode(), r.getName(), r.getDataScope().name());
    }

    private Map<String, Object> snapshot(AppUser u, Role role) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone());
        m.put("username", u.getUsername());
        m.put("department", u.getDepartment());
        m.put("role", role.getCode());
        m.put("status", u.getStatus().name());
        return m;
    }

    /** Batch lookups so a page of users costs three extra queries, not three per row. */
    private static final class Lookup {
        private Map<Long, Role> roles = new HashMap<>();
        private Map<Long, PartnerRefResponseDto> partners = new HashMap<>();
        private Map<Long, String> names = new HashMap<>();
    }

    private Lookup lookup(List<AppUser> users) {
        Lookup lk = new Lookup();
        Set<Long> roleIds = new HashSet<>();
        Set<Long> partnerIds = new HashSet<>();
        for (AppUser u : users) {
            roleIds.add(u.getRoleId());
            partnerIds.add(u.getChannelPartnerId());
        }
        for (Role r : roleRepository.findAllById(roleIds)) {
            lk.roles.put(r.getId(), r);
        }
        lk.partners = partnerRefProvider.refs(partnerIds);
        lk.names = auditInfoMapper.namesFor(users);
        return lk;
    }

    private UserResponseDto toSingleResponse(AppUser u) {
        return toResponse(u, lookup(List.of(u)));
    }

    private UserResponseDto toResponse(AppUser u, Lookup lk) {
        UserResponseDto d = new UserResponseDto();
        d.setId(u.getId());
        d.setCode(u.getCode());
        d.setName(u.getName());
        d.setEmail(u.getEmail());
        d.setPhone(u.getPhone());
        d.setUsername(u.getUsername());
        d.setDepartment(u.getDepartment());
        d.setStatus(u.getStatus().name());
        Role role = lk.roles.get(u.getRoleId());
        d.setRole(role == null ? null : roleRef(role));
        d.setPartner(lk.partners.get(u.getChannelPartnerId()));
        d.setPhotoUrl(u.getPhotoPath());
        d.setLastLoginAt(u.getLastLoginAt());
        d.setMustChangePassword(u.isMustChangePassword());
        d.setAudit(auditInfoMapper.of(u, lk.names));
        return d;
    }
}
