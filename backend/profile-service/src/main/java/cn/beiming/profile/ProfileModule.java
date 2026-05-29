package cn.beiming.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.HttpRequestMethodNotSupportedException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/profile")
class ProfileController {
    private final ProfileStore store;
    private final ProfileAuthContextProvider auth;

    ProfileController(ProfileStore store, ProfileAuthContextProvider auth) {
        this.store = store;
        this.auth = auth;
    }

    @GetMapping("/members")
    ResponseEntity<Map<String, Object>> publicMembers(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String groupId,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String sort) {
        return ok(store.publicMembers(page, pageSize, keyword, groupId, status, sort));
    }

    @GetMapping("/members/{memberId}")
    ResponseEntity<Map<String, Object>> publicMember(@PathVariable String memberId) {
        return ok(store.publicMember(memberId));
    }

    @GetMapping("/me")
    ResponseEntity<Map<String, Object>> me(HttpServletRequest request) {
        AuthUser current = auth.requireCurrent(request);
        return ok(store.currentUserProfile(current.userId));
    }

    @PatchMapping("/me")
    ResponseEntity<Map<String, Object>> patchMe(HttpServletRequest request,
                                                @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(request);
        return ok(store.updateSelf(current, bodyOrEmpty(body)));
    }

    @GetMapping("/admin/members")
    ResponseEntity<Map<String, Object>> adminMembers(HttpServletRequest request,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int pageSize,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String groupId,
                                                     @RequestParam(required = false) String status,
                                                     @RequestParam(required = false) String visibility,
                                                     @RequestParam(required = false) String sort) {
        AuthUser current = auth.requireCurrent(request);
        requireReader(current);
        return ok(store.adminMembers(page, pageSize, keyword, groupId, status, visibility, sort));
    }

    @GetMapping("/admin/members/{memberId}")
    ResponseEntity<Map<String, Object>> adminMember(HttpServletRequest request,
                                                    @PathVariable String memberId) {
        AuthUser current = auth.requireCurrent(request);
        requireReader(current);
        return ok(store.adminMember(memberId));
    }

    @PostMapping("/admin/members/activate")
    ResponseEntity<Map<String, Object>> activateMember(HttpServletRequest request,
                                                       @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(request);
        requireWriter(current);
        return created(store.activateMember(current, auth, bodyOrEmpty(body)));
    }

    @PatchMapping("/admin/members/{memberId}")
    ResponseEntity<Map<String, Object>> updateMember(HttpServletRequest request,
                                                     @PathVariable String memberId,
                                                     @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(request);
        requireWriter(current);
        return ok(store.updateMember(current, memberId, bodyOrEmpty(body)));
    }

    @PatchMapping("/admin/members/{memberId}/status")
    ResponseEntity<Map<String, Object>> updateStatus(HttpServletRequest request,
                                                     @PathVariable String memberId,
                                                     @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(request);
        requireWriter(current);
        return ok(store.updateStatus(current, memberId, bodyOrEmpty(body)));
    }

    @GetMapping("/admin/groups")
    ResponseEntity<Map<String, Object>> groups(HttpServletRequest request,
                                               @RequestParam(defaultValue = "false") boolean includeArchived) {
        AuthUser current = auth.requireCurrent(request);
        requireReader(current);
        return ok(mapOf("items", store.groups(includeArchived)));
    }

    @PostMapping("/admin/groups")
    ResponseEntity<Map<String, Object>> createGroup(HttpServletRequest request,
                                                    @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(request);
        requireWriter(current);
        return created(store.createGroup(current, bodyOrEmpty(body)));
    }

    @PatchMapping("/admin/groups/{groupId}")
    ResponseEntity<Map<String, Object>> updateGroup(HttpServletRequest request,
                                                    @PathVariable String groupId,
                                                    @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(request);
        requireWriter(current);
        return ok(store.updateGroup(current, groupId, bodyOrEmpty(body)));
    }

    @PatchMapping("/admin/groups/{groupId}/archive")
    ResponseEntity<Map<String, Object>> archiveGroup(HttpServletRequest request,
                                                     @PathVariable String groupId,
                                                     @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(request);
        requireWriter(current);
        return ok(store.archiveGroup(current, groupId, bodyOrEmpty(body)));
    }

    @PutMapping("/admin/members/{memberId}/milestones")
    ResponseEntity<Map<String, Object>> replaceMilestones(HttpServletRequest request,
                                                          @PathVariable String memberId,
                                                          @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(request);
        requireWriter(current);
        return ok(store.replaceMilestones(current, memberId, bodyOrEmpty(body)));
    }

    @PutMapping("/admin/members/{memberId}/work-snapshots")
    ResponseEntity<Map<String, Object>> replaceWorks(HttpServletRequest request,
                                                     @PathVariable String memberId,
                                                     @RequestBody(required = false) Map<String, Object> body) {
        AuthUser current = auth.requireCurrent(request);
        requireWriter(current);
        return ok(store.replaceWorks(current, memberId, bodyOrEmpty(body)));
    }

    @GetMapping("/admin/members/{memberId}/audit-logs")
    ResponseEntity<Map<String, Object>> auditLogs(HttpServletRequest request,
                                                  @PathVariable String memberId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize) {
        AuthUser current = auth.requireCurrent(request);
        if (!current.roles.contains("ADMIN") && !current.roles.contains("OWNER")) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role insufficient");
        }
        return ok(store.auditLogs(memberId, page, pageSize));
    }

    private void requireReader(AuthUser current) {
        if (!current.roles.contains("HELPER") && !current.roles.contains("ADMIN") && !current.roles.contains("OWNER")) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role insufficient");
        }
    }

    private void requireWriter(AuthUser current) {
        if (!current.roles.contains("ADMIN") && !current.roles.contains("OWNER")) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role insufficient");
        }
    }

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        return ResponseEntity.ok(envelope(0, "success", data));
    }

    private ResponseEntity<Map<String, Object>> created(Object data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(0, "success", data));
    }

    static Map<String, Object> envelope(int code, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        return body;
    }

    static Map<String, Object> bodyOrEmpty(Map<String, Object> body) {
        return body == null ? Map.of() : body;
    }

    static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }
}

@Service
class ProfileAuthContextProvider {
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9_.:-]{1,128}");
    private static final Pattern MINECRAFT_UUID_PATTERN = Pattern.compile("[a-f0-9]{32}");
    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "HELPER", "USER");
    private static final Set<String> VALID_PERMISSIONS = Set.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE", "VM_OPERATE", "FILE_MANAGE", "TERMINAL_ACCESS", "HIGH_RISK_APPROVE");
    private final Map<String, AuthUser> usersByToken = new ConcurrentHashMap<>();
    private final Map<String, AuthUser> targetsByUserId = new ConcurrentHashMap<>();
    private final Set<String> missingTargets = ConcurrentHashMap.newKeySet();
    private boolean failCurrentUnavailable;
    private boolean failCurrentTimeout;
    private boolean failCurrentIncompatible;
    private boolean failTargetUnavailable;
    private boolean failTargetTimeout;
    private boolean failTargetIncompatible;
    private int writeCallCount;

    synchronized void reset() {
        usersByToken.clear();
        targetsByUserId.clear();
        missingTargets.clear();
        failCurrentUnavailable = false;
        failCurrentTimeout = false;
        failCurrentIncompatible = false;
        failTargetUnavailable = false;
        failTargetTimeout = false;
        failTargetIncompatible = false;
        writeCallCount = 0;
        seedToken("owner-token", "owner", "Owner", List.of("OWNER"), "ACTIVE", null);
        seedToken("admin-token", "admin", "Admin", List.of("ADMIN"), "ACTIVE", null);
        seedToken("helper-token", "helper", "Helper", List.of("HELPER"), "ACTIVE", null);
        seedToken("user-token", "user", "User", List.of("USER"), "ACTIVE", null);
        seedToken("active-member-token", "active_member", "Active Member", List.of("USER"), "ACTIVE", mc("ActiveMc", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        seedToken("no-profile-token", "user_without_profile", "No Profile", List.of("USER"), "ACTIVE", null);
        seedToken("archived-member-token", "archived_member", "Archived Member", List.of("USER"), "ACTIVE", null);
        seedTarget("target_user", "Target User", "PENDING_PROFILE", mc("TargetMc", "dddddddddddddddddddddddddddddddd"));
        seedTarget("another_target", "Another Target", "PENDING_PROFILE", mc("AnotherMc", "eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee"));
        seedTarget("owner_target", "Owner Target", "ACTIVE", mc("OwnerMc", "ffffffffffffffffffffffffffffffff"));
        seedTarget("group_target", "Group Target", "ACTIVE", mc("GroupTargetMc", "11111111111111111111111111111111"));
        seedTarget("mc_conflict_user", "Conflict User", "ACTIVE", mc("ActiveMc", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        seedTarget("race_user", "Race User", "ACTIVE", mc("RaceMc", "22222222222222222222222222222222"));
        seedTarget("audit_fail_target", "Audit Fail", "ACTIVE", mc("AuditFailMc", "33333333333333333333333333333333"));
        seedTarget("unavailable_target", "Unavailable Target", "ACTIVE", mc("UnavailableMc", "44444444444444444444444444444444"));
        seedTarget("timeout_target", "Timeout Target", "ACTIVE", mc("TimeoutMc", "55555555555555555555555555555555"));
        seedTarget("bad_target", "Bad Target", "ACTIVE", mc("BadMc", "66666666666666666666666666666666"));
        seedTarget("helper_target", "Helper Target", "ACTIVE", mc("HelperMc", "77777777777777777777777777777777"));
    }

    private void seedToken(String token, String userId, String displayName, List<String> roles, String status, MinecraftBinding binding) {
        usersByToken.put(token, new AuthUser(userId, displayName, new LinkedHashSet<>(roles), new LinkedHashSet<>(), status, binding));
    }

    private void seedTarget(String userId, String displayName, String status, MinecraftBinding binding) {
        targetsByUserId.put(userId, new AuthUser(userId, displayName, new LinkedHashSet<>(List.of("USER")), new LinkedHashSet<>(), status, binding));
    }

    AuthUser requireCurrent(HttpServletRequest request) {
        String gatewayRequestId = request.getHeader("X-Gateway-Internal-Request-Id");
        if (gatewayRequestId != null && !gatewayRequestId.isBlank()) {
            return requireGatewayContext(request, gatewayRequestId);
        }
        return requireCurrent(request.getHeader("Authorization"));
    }

    AuthUser requireCurrent(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw new ApiException(41000, HttpStatus.UNAUTHORIZED, "unauthenticated");
        }
        if (!authorization.startsWith("Bearer ")) {
            throw new ApiException(41003, HttpStatus.UNAUTHORIZED, "invalid token format");
        }
        if (failCurrentUnavailable) {
            failCurrentUnavailable = false;
            throw new ApiException(46200, HttpStatus.BAD_GATEWAY, "auth unavailable");
        }
        if (failCurrentTimeout) {
            failCurrentTimeout = false;
            throw new ApiException(46201, HttpStatus.GATEWAY_TIMEOUT, "auth timeout");
        }
        if (failCurrentIncompatible) {
            failCurrentIncompatible = false;
            throw new ApiException(46202, HttpStatus.BAD_GATEWAY, "auth incompatible");
        }
        AuthUser user = usersByToken.get(authorization.substring("Bearer ".length()));
        if (user == null) {
            throw new ApiException(41001, HttpStatus.UNAUTHORIZED, "invalid session");
        }
        if (user.userId == null || user.userId.isBlank()) {
            throw new ApiException(46202, HttpStatus.BAD_GATEWAY, "auth incompatible");
        }
        if ("DISABLED".equals(user.status) || "BANNED".equals(user.status)) {
            throw new ApiException(41001, HttpStatus.UNAUTHORIZED, "invalid session");
        }
        return user.copy();
    }

    private AuthUser requireGatewayContext(HttpServletRequest request, String gatewayRequestId) {
        if (!REQUEST_ID_PATTERN.matcher(gatewayRequestId).matches()) {
            throw incompatibleGatewayContext();
        }
        String userId = request.getHeader("X-Beiming-Actor-User-Id");
        if (userId == null || userId.isBlank()) {
            throw incompatibleGatewayContext();
        }
        LinkedHashSet<String> roles = parseCsvHeader(request.getHeader("X-Beiming-Actor-Roles"), VALID_ROLES);
        LinkedHashSet<String> permissions = parseCsvHeader(request.getHeader("X-Beiming-Actor-Permissions"), VALID_PERMISSIONS);
        String minecraftId = blankToNull(request.getHeader("X-Beiming-Actor-Minecraft-Id"));
        String minecraftUuid = blankToNull(request.getHeader("X-Beiming-Actor-Minecraft-Uuid"));
        MinecraftBinding binding = null;
        if (minecraftId != null || minecraftUuid != null) {
            if (minecraftId == null || minecraftUuid == null || !MINECRAFT_UUID_PATTERN.matcher(minecraftUuid).matches()) {
                throw incompatibleGatewayContext();
            }
            binding = mc(minecraftId, minecraftUuid);
        }
        return new AuthUser(userId.trim(), userId.trim(), roles, permissions, "ACTIVE", binding);
    }

    private LinkedHashSet<String> parseCsvHeader(String value, Set<String> allowed) {
        LinkedHashSet<String> parsed = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return parsed;
        }
        for (String part : value.split(",")) {
            String item = part.trim();
            if (item.isEmpty()) {
                continue;
            }
            if (!allowed.contains(item)) {
                throw incompatibleGatewayContext();
            }
            parsed.add(item);
        }
        return parsed;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ApiException incompatibleGatewayContext() {
        return new ApiException(46202, HttpStatus.BAD_GATEWAY, "auth incompatible");
    }

    AuthUser targetUser(String userId) {
        if (failTargetUnavailable) {
            failTargetUnavailable = false;
            throw new ApiException(46200, HttpStatus.BAD_GATEWAY, "auth unavailable");
        }
        if (failTargetTimeout) {
            failTargetTimeout = false;
            throw new ApiException(46201, HttpStatus.GATEWAY_TIMEOUT, "auth timeout");
        }
        if (failTargetIncompatible) {
            failTargetIncompatible = false;
            throw new ApiException(46202, HttpStatus.BAD_GATEWAY, "auth incompatible");
        }
        if (missingTargets.contains(userId)) {
            throw new ApiException(43204, HttpStatus.NOT_FOUND, "auth user not found");
        }
        AuthUser seeded = targetsByUserId.computeIfAbsent(userId, this::defaultTarget);
        if (seeded.displayName == null || seeded.roles == null) {
            throw new ApiException(46202, HttpStatus.BAD_GATEWAY, "auth incompatible");
        }
        return seeded.copy();
    }

    private AuthUser defaultTarget(String userId) {
        String clean = userId.replace("_", " ");
        String display = java.util.Arrays.stream(clean.split(" "))
                .filter(part -> !part.isBlank())
                .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1))
                .reduce((left, right) -> left + " " + right)
                .orElse(userId);
        String hex = UUID.nameUUIDFromBytes(userId.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().replace("-", "").substring(0, 32);
        return new AuthUser(userId, display, new LinkedHashSet<>(List.of("USER")), new LinkedHashSet<>(), "ACTIVE", mc(display.replace(" ", ""), hex));
    }

    void failNextCurrentUnavailable() {
        failCurrentUnavailable = true;
    }

    void failNextCurrentTimeout() {
        failCurrentTimeout = true;
    }

    void failNextCurrentIncompatible() {
        failCurrentIncompatible = true;
    }

    void failNextTargetUnavailable() {
        failTargetUnavailable = true;
    }

    void failNextTargetTimeout() {
        failTargetTimeout = true;
    }

    void failNextTargetIncompatible() {
        failTargetIncompatible = true;
    }

    void setTokenRoles(String token, List<String> roles) {
        AuthUser user = usersByToken.get(token);
        usersByToken.put(token, new AuthUser(user.userId, user.displayName, new LinkedHashSet<>(roles), user.permissions, user.status, user.minecraftBinding));
    }

    void setTargetMissing(String userId) {
        missingTargets.add(userId);
    }

    void setTargetStatus(String userId, String status) {
        AuthUser current = targetsByUserId.computeIfAbsent(userId, this::defaultTarget);
        targetsByUserId.put(userId, new AuthUser(current.userId, current.displayName, current.roles, current.permissions, status, current.minecraftBinding));
    }

    int writeCallCount() {
        return writeCallCount;
    }

    static MinecraftBinding mc(String minecraftId, String minecraftUuid) {
        return new MinecraftBinding(minecraftId, minecraftUuid, Instant.parse("2026-05-21T00:00:00Z"), "MANUAL_VERIFICATION");
    }
}

@Service
class ProfileStore {
    private static final Set<String> PUBLIC_STATUSES = Set.of("ACTIVE", "INACTIVE", "SUSPENDED");
    private static final Set<String> ALL_STATUSES = Set.of("PENDING_ACTIVATION", "ACTIVE", "INACTIVE", "SUSPENDED", "REMOVED", "ARCHIVED");
    private static final Set<String> MILESTONE_TYPES = Set.of("JOINED", "PROJECT", "EVENT", "AWARD", "MANAGEMENT", "OTHER");
    private static final Set<String> WORK_TYPES = Set.of("BUILD", "REDSTONE", "FARM", "ARTICLE", "IMAGE", "VIDEO", "OTHER");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, MemberProfile> profilesById = new ConcurrentHashMap<>();
    private final Map<String, String> memberIdByUserId = new ConcurrentHashMap<>();
    private final Map<String, MemberGroup> groupsById = new ConcurrentHashMap<>();
    private final List<ProfileAudit> audits = new ArrayList<>();
    private final Map<String, IdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    private boolean failNextPublicRead;
    private boolean failNextAudit;

    synchronized void reset() {
        profilesById.clear();
        memberIdByUserId.clear();
        groupsById.clear();
        audits.clear();
        idempotency.clear();
        failNextPublicRead = false;
        failNextAudit = false;
    }

    synchronized void seedTestData(ProfileAuthContextProvider auth) {
        MemberGroup builder = seedGroup("grp_builder", "Builder", "Build team", "#123ABC", 1, false);
        MemberGroup redstone = seedGroup("grp_redstone", "Redstone", "Redstone team", "#AA0000", 2, false);
        seedGroup("grp_archived", "Old Group", "Archived", "#CCCCCC", 99, true);
        seedProfile("active_member", "Active Member", "ACTIVE", "PUBLIC", builder.id, "ActiveMc", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "Active bio", "internal");
        seedProfile("private_member", "Private Member", "ACTIVE", "PRIVATE", null, "PrivateMc", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "Private bio", "private note");
        seedProfile("removed_member", "Removed Member", "REMOVED", "PUBLIC", null, "RemovedMc", "88888888888888888888888888888888", "Removed bio", "removed note");
        seedProfile("archived_member", "Archived Member", "ARCHIVED", "PUBLIC", null, "ArchivedMc", "99999999999999999999999999999999", "Archived bio", "archived note");
        seedProfile("pending_member", "Pending Member", "PENDING_ACTIVATION", "PUBLIC", null, "PendingMc", "12121212121212121212121212121212", "Pending bio", "pending note");
        seedProfile("member_with_group", "Grouped Member", "ACTIVE", "PUBLIC", redstone.id, "GroupMc", "abababababababababababababababab", "Grouped bio", "group note");
        MemberProfile milestoneProfile = seedProfile("member_with_milestones", "Milestone Member", "ACTIVE", "PUBLIC", builder.id, "MileMc", "cdcdcdcdcdcdcdcdcdcdcdcdcdcdcdcd", "Milestone bio", "milestone note");
        milestoneProfile.milestones.add(new MemberMilestone("milestone_seed_1", "PROJECT", "Seed Project", "Seed", Instant.parse("2026-05-20T00:00:00Z"), true, 1, now(), now()));
        milestoneProfile.works.add(new MemberWork("work_seed_1", "BUILD", "Seed Work", "Seed work", "/covers/seed.png", "content", "content-seed", true, 1, now(), now()));
        MemberProfile workProfile = seedProfile("member_with_work_snapshots", "Work Member", "ACTIVE", "PUBLIC", builder.id, "WorkMc", "efefefefefefefefefefefefefefefef", "Work bio", "work note");
        workProfile.works.add(new MemberWork("work_seed_2", "BUILD", "Old Work", "Old", "/covers/old.png", "content", "content-old", true, 1, now(), now()));
    }

    private MemberGroup seedGroup(String id, String name, String description, String color, int sortOrder, boolean archived) {
        MemberGroup group = new MemberGroup(id, name, description, color, sortOrder, archived, now(), now(), archived ? now() : null);
        groupsById.put(id, group);
        return group;
    }

    private MemberProfile seedProfile(String userId, String displayName, String status, String visibility, String groupId, String minecraftId, String minecraftUuid, String bio, String adminNote) {
        MemberProfile profile = new MemberProfile("mem_" + userId, userId, displayName, "ACTIVE", new LinkedHashSet<>(List.of("USER")), null, minecraftId, minecraftUuid, null, groupId, status, visibility, Instant.parse("2026-05-01T00:00:00Z"), bio, adminNote, now(), now(), "ARCHIVED".equals(status) ? now() : null);
        profilesById.put(profile.memberId, profile);
        memberIdByUserId.put(userId, profile.memberId);
        return profile;
    }

    synchronized Map<String, Object> publicMembers(int page, int pageSize, String keyword, String groupId, String status, String sort) {
        if (failNextPublicRead) {
            failNextPublicRead = false;
            throw new ApiException(51200, HttpStatus.INTERNAL_SERVER_ERROR, "profile internal error");
        }
        validatePage(page, pageSize);
        if (status != null && !PUBLIC_STATUSES.contains(status)) {
            throw ApiException.badRequest("status");
        }
        List<MemberProfile> rows = profilesById.values().stream()
                .filter(profile -> "PUBLIC".equals(profile.visibility))
                .filter(profile -> PUBLIC_STATUSES.contains(profile.status))
                .filter(profile -> status == null || status.equals(profile.status))
                .filter(profile -> groupId == null || Objects.equals(groupId, profile.groupId))
                .filter(profile -> keyword == null || matches(profile, keyword))
                .sorted(publicComparator(sort))
                .toList();
        return page(rows.stream().map(this::publicSummary).toList(), page, pageSize);
    }

    synchronized Map<String, Object> publicMember(String memberId) {
        MemberProfile profile = profile(memberId);
        if (!"PUBLIC".equals(profile.visibility) || !PUBLIC_STATUSES.contains(profile.status)) {
            throw new ApiException(43213, HttpStatus.CONFLICT, "profile not public");
        }
        return publicDetail(profile);
    }

    synchronized Map<String, Object> currentUserProfile(String userId) {
        return currentView(profileByUserId(userId));
    }

    synchronized Map<String, Object> updateSelf(AuthUser actor, Map<String, Object> body) {
        MemberProfile profile = profileByUserId(actor.userId);
        if ("REMOVED".equals(profile.status) || "ARCHIVED".equals(profile.status)) {
            throw new ApiException(43212, HttpStatus.CONFLICT, "invalid status");
        }
        for (String field : List.of("status", "groupId", "memberGroupId", "joinedAt", "adminNote", "userId", "minecraftId", "minecraftUuid", "displayNameSnapshot")) {
            if (body.containsKey(field)) {
                throw ApiException.badRequest(field);
            }
        }
        String reason = requiredString(body, "reason");
        String avatarUrl = optionalString(body, "avatarUrl");
        String skinUrl = optionalString(body, "skinUrl");
        String bio = optionalString(body, "bio");
        String visibility = optionalString(body, "visibility");
        validateUrlNullable(avatarUrl, "avatarUrl");
        validateUrlNullable(skinUrl, "skinUrl");
        validateLengthNullable(bio, 1000, "bio");
        if (visibility != null && !Set.of("PUBLIC", "PRIVATE").contains(visibility)) {
            throw ApiException.badRequest("visibility");
        }
        MemberProfile next = profile.copy();
        if (body.containsKey("avatarUrl")) next.avatarUrl = avatarUrl;
        if (body.containsKey("skinUrl")) next.skinUrl = skinUrl;
        if (body.containsKey("bio")) next.bio = bio;
        if (visibility != null) next.visibility = visibility;
        next.updatedAt = now();
        audit("PROFILE_SELF_UPDATED", actor, profile.memberId, reason, state(profile), state(next));
        profilesById.put(profile.memberId, next);
        return currentView(next);
    }

    synchronized Map<String, Object> adminMembers(int page, int pageSize, String keyword, String groupId, String status, String visibility, String sort) {
        validatePage(page, pageSize);
        if (status != null && !ALL_STATUSES.contains(status)) {
            throw ApiException.badRequest("status");
        }
        if (visibility != null && !Set.of("PUBLIC", "PRIVATE").contains(visibility)) {
            throw ApiException.badRequest("visibility");
        }
        List<MemberProfile> rows = profilesById.values().stream()
                .filter(profile -> status == null || status.equals(profile.status))
                .filter(profile -> visibility == null || visibility.equals(profile.visibility))
                .filter(profile -> groupId == null || Objects.equals(groupId, profile.groupId))
                .filter(profile -> keyword == null || matches(profile, keyword) || contains(profile.adminNote, keyword) || contains(profile.userId, keyword))
                .sorted(adminComparator(sort))
                .toList();
        return page(rows.stream().map(this::adminProfile).toList(), page, pageSize);
    }

    synchronized Map<String, Object> adminMember(String memberId) {
        return adminProfile(profile(memberId));
    }

    synchronized Map<String, Object> activateMember(AuthUser actor, ProfileAuthContextProvider auth, Map<String, Object> body) {
        String userId = requiredString(body, "userId");
        String reason = requiredString(body, "reason");
        String idempotencyKey = optionalString(body, "idempotencyKey");
        String idemKey = actor.userId + ":activate:" + idempotencyKey;
        String signature = signature(body);
        if (idempotencyKey != null && idempotency.containsKey(idemKey)) {
            IdempotencyRecord record = idempotency.get(idemKey);
            if (!record.signature.equals(signature)) {
                throw new ApiException(43002, HttpStatus.CONFLICT, "idempotency conflict");
            }
            return record.payload;
        }
        if (memberIdByUserId.containsKey(userId)) {
            throw new ApiException(43210, HttpStatus.CONFLICT, "profile exists");
        }
        String groupId = optionalString(body, "groupId");
        if (groupId != null && !groupsById.containsKey(groupId)) {
            throw new ApiException(43201, HttpStatus.NOT_FOUND, "group not found");
        }
        AuthUser target = auth.targetUser(userId);
        if (!Set.of("PENDING_PROFILE", "ACTIVE").contains(target.status)) {
            throw new ApiException(43215, HttpStatus.CONFLICT, "auth user status invalid");
        }
        String minecraftId = target.minecraftBinding == null ? null : target.minecraftBinding.minecraftId;
        String minecraftUuid = target.minecraftBinding == null ? null : target.minecraftBinding.minecraftUuid;
        ensureMinecraftUnique(null, minecraftId, minecraftUuid);
        MemberProfile profile = new MemberProfile("mem_" + UUID.randomUUID(), userId, target.displayName, target.status, target.roles, optionalString(body, "avatarUrl"), minecraftId, minecraftUuid, optionalString(body, "skinUrl"), groupId, "ACTIVE", optionalStringOrDefault(body, "visibility", "PUBLIC"), optionalInstant(body, "joinedAt", now()), optionalString(body, "bio"), null, now(), now(), null);
        validateUrlNullable(profile.avatarUrl, "avatarUrl");
        validateUrlNullable(profile.skinUrl, "skinUrl");
        validateLengthNullable(profile.bio, 1000, "bio");
        audit("PROFILE_MEMBER_ACTIVATED", actor, profile.memberId, reason, null, state(profile));
        profilesById.put(profile.memberId, profile);
        memberIdByUserId.put(userId, profile.memberId);
        Map<String, Object> payload = adminProfile(profile);
        if (idempotencyKey != null) {
            idempotency.put(idemKey, new IdempotencyRecord(signature, payload));
        }
        return payload;
    }

    synchronized Map<String, Object> updateMember(AuthUser actor, String memberId, Map<String, Object> body) {
        MemberProfile current = profile(memberId);
        String reason = requiredString(body, "reason");
        if ("ARCHIVED".equals(current.status) && "PUBLIC".equals(optionalString(body, "visibility"))) {
            throw new ApiException(43212, HttpStatus.CONFLICT, "archived profile");
        }
        MemberProfile next = current.copy();
        if (body.containsKey("displayNameSnapshot")) {
            next.displayNameSnapshot = requiredString(body, "displayNameSnapshot");
            validateLength(next.displayNameSnapshot, 2, 24, "displayNameSnapshot");
        }
        if (body.containsKey("avatarUrl")) next.avatarUrl = optionalString(body, "avatarUrl");
        if (body.containsKey("minecraftId")) next.minecraftId = optionalString(body, "minecraftId");
        if (body.containsKey("minecraftUuid")) next.minecraftUuid = optionalString(body, "minecraftUuid");
        if (body.containsKey("skinUrl")) next.skinUrl = optionalString(body, "skinUrl");
        if (body.containsKey("groupId")) next.groupId = optionalString(body, "groupId");
        if (body.containsKey("joinedAt")) next.joinedAt = optionalString(body, "joinedAt") == null ? null : Instant.parse(optionalString(body, "joinedAt"));
        if (body.containsKey("bio")) next.bio = optionalString(body, "bio");
        if (body.containsKey("visibility")) next.visibility = requiredString(body, "visibility");
        if (body.containsKey("adminNote")) next.adminNote = optionalString(body, "adminNote");
        validateProfilePatch(next);
        ensureMinecraftUnique(memberId, next.minecraftId, next.minecraftUuid);
        next.updatedAt = now();
        audit("PROFILE_MEMBER_UPDATED", actor, memberId, reason, state(current), state(next));
        profilesById.put(memberId, next);
        return adminProfile(next);
    }

    synchronized Map<String, Object> updateStatus(AuthUser actor, String memberId, Map<String, Object> body) {
        MemberProfile current = profile(memberId);
        String status = requiredString(body, "status");
        String reason = requiredString(body, "reason");
        if (!ALL_STATUSES.contains(status)) {
            throw ApiException.badRequest("status");
        }
        if (!canTransition(current.status, status)) {
            throw new ApiException(43212, HttpStatus.CONFLICT, "invalid status transition");
        }
        MemberProfile next = current.copy();
        next.status = status;
        next.updatedAt = now();
        if ("ARCHIVED".equals(status)) {
            next.archivedAt = now();
        }
        audit("PROFILE_MEMBER_STATUS_CHANGED", actor, memberId, reason, current.status, status);
        profilesById.put(memberId, next);
        return adminProfile(next);
    }

    synchronized List<Map<String, Object>> groups(boolean includeArchived) {
        return groupsById.values().stream()
                .filter(group -> includeArchived || !group.archived)
                .sorted(Comparator.comparingInt(group -> group.sortOrder))
                .map(this::groupMap)
                .toList();
    }

    synchronized Map<String, Object> createGroup(AuthUser actor, Map<String, Object> body) {
        String name = requiredString(body, "name");
        String reason = requiredString(body, "reason");
        validateGroupFields(name, optionalString(body, "description"), optionalString(body, "color"));
        String idempotencyKey = optionalString(body, "idempotencyKey");
        String idemKey = actor.userId + ":group:" + idempotencyKey;
        String signature = signature(body);
        if (idempotencyKey != null && idempotency.containsKey(idemKey)) {
            IdempotencyRecord record = idempotency.get(idemKey);
            if (!record.signature.equals(signature)) {
                throw new ApiException(43002, HttpStatus.CONFLICT, "idempotency conflict");
            }
            return record.payload;
        }
        ensureGroupNameAvailable(name, null);
        MemberGroup group = new MemberGroup("grp_" + UUID.randomUUID(), name, optionalString(body, "description"), optionalString(body, "color"), intOrDefault(body, "sortOrder", 100), false, now(), now(), null);
        audit("PROFILE_GROUP_CREATED", actor, group.id, reason, null, group.name);
        groupsById.put(group.id, group);
        Map<String, Object> payload = groupMap(group);
        if (idempotencyKey != null) {
            idempotency.put(idemKey, new IdempotencyRecord(signature, payload));
        }
        return payload;
    }

    synchronized Map<String, Object> updateGroup(AuthUser actor, String groupId, Map<String, Object> body) {
        MemberGroup group = group(groupId);
        String reason = requiredString(body, "reason");
        MemberGroup next = group.copy();
        if (body.containsKey("name")) next.name = requiredString(body, "name");
        if (body.containsKey("description")) next.description = optionalString(body, "description");
        if (body.containsKey("color")) next.color = optionalString(body, "color");
        if (body.containsKey("sortOrder")) next.sortOrder = intValue(body, "sortOrder");
        validateGroupFields(next.name, next.description, next.color);
        ensureGroupNameAvailable(next.name, groupId);
        next.updatedAt = now();
        audit("PROFILE_GROUP_UPDATED", actor, groupId, reason, group.name, next.name);
        groupsById.put(groupId, next);
        return groupMap(next);
    }

    synchronized Map<String, Object> archiveGroup(AuthUser actor, String groupId, Map<String, Object> body) {
        MemberGroup group = group(groupId);
        String reason = requiredString(body, "reason");
        if (group.archived) {
            return groupMap(group);
        }
        boolean used = profilesById.values().stream().anyMatch(profile -> Objects.equals(profile.groupId, groupId) && !"ARCHIVED".equals(profile.status));
        if (used) {
            throw new ApiException(43214, HttpStatus.CONFLICT, "group is used");
        }
        MemberGroup next = group.copy();
        next.archived = true;
        next.archivedAt = now();
        next.updatedAt = now();
        audit("PROFILE_GROUP_ARCHIVED", actor, groupId, reason, group.name, "archived");
        groupsById.put(groupId, next);
        return groupMap(next);
    }

    synchronized Map<String, Object> replaceMilestones(AuthUser actor, String memberId, Map<String, Object> body) {
        MemberProfile profile = profile(memberId);
        String reason = requiredString(body, "reason");
        List<MemberMilestone> nextItems = milestoneItems(body.get("items"));
        MemberProfile next = profile.copy();
        next.milestones = nextItems;
        next.updatedAt = now();
        audit("PROFILE_MEMBER_MILESTONES_REPLACED", actor, memberId, reason, String.valueOf(profile.milestones.size()), String.valueOf(nextItems.size()));
        profilesById.put(memberId, next);
        return adminProfile(next);
    }

    synchronized Map<String, Object> replaceWorks(AuthUser actor, String memberId, Map<String, Object> body) {
        MemberProfile profile = profile(memberId);
        String reason = requiredString(body, "reason");
        List<MemberWork> nextItems = workItems(body.get("items"));
        MemberProfile next = profile.copy();
        next.works = nextItems;
        next.updatedAt = now();
        audit("PROFILE_MEMBER_WORKS_REPLACED", actor, memberId, reason, String.valueOf(profile.works.size()), String.valueOf(nextItems.size()));
        profilesById.put(memberId, next);
        return adminProfile(next);
    }

    synchronized Map<String, Object> auditLogs(String memberId, int page, int pageSize) {
        profile(memberId);
        validatePage(page, pageSize);
        List<Map<String, Object>> rows = audits.stream()
                .filter(audit -> Objects.equals(audit.targetId, memberId))
                .sorted(Comparator.comparing((ProfileAudit audit) -> audit.createdAt).reversed())
                .map(this::auditMap)
                .toList();
        return page(rows, page, pageSize);
    }

    private List<MemberMilestone> milestoneItems(Object raw) {
        if (!(raw instanceof List<?> rows) || rows.size() > 50) {
            throw ApiException.badRequest("items");
        }
        List<MemberMilestone> items = new ArrayList<>();
        for (Object item : rows) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                throw ApiException.badRequest("items");
            }
            Map<String, Object> map = normalizeMap(rawMap);
            String type = requiredString(map, "type");
            String title = requiredString(map, "title");
            if (!MILESTONE_TYPES.contains(type)) throw ApiException.badRequest("type");
            validateLength(title, 2, 80, "title");
            String id = optionalString(map, "id");
            items.add(new MemberMilestone(id == null ? "milestone_" + UUID.randomUUID() : id, type, title, optionalString(map, "description"), Instant.parse(requiredString(map, "happenedAt")), booleanValue(map, "publicVisible"), intValue(map, "sortOrder"), now(), now()));
        }
        return items.stream().sorted(Comparator.comparingInt((MemberMilestone item) -> item.sortOrder).thenComparing(item -> item.happenedAt)).toList();
    }

    private List<MemberWork> workItems(Object raw) {
        if (!(raw instanceof List<?> rows) || rows.size() > 30) {
            throw ApiException.badRequest("items");
        }
        List<MemberWork> items = new ArrayList<>();
        for (Object item : rows) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                throw ApiException.badRequest("items");
            }
            Map<String, Object> map = normalizeMap(rawMap);
            String type = requiredString(map, "type");
            String title = requiredString(map, "title");
            if (!WORK_TYPES.contains(type)) throw ApiException.badRequest("type");
            validateLength(title, 2, 80, "title");
            String coverUrl = optionalString(map, "coverUrl");
            validateUrlNullable(coverUrl, "coverUrl");
            String id = optionalString(map, "id");
            items.add(new MemberWork(id == null ? "work_" + UUID.randomUUID() : id, type, title, optionalString(map, "summary"), coverUrl, optionalString(map, "sourceModule"), optionalString(map, "sourceId"), booleanValue(map, "publicVisible"), intValue(map, "sortOrder"), now(), now()));
        }
        return items.stream().sorted(Comparator.comparingInt((MemberWork item) -> item.sortOrder).thenComparing(item -> item.title)).toList();
    }

    private Map<String, Object> normalizeMap(Map<?, ?> rawMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> map.put(String.valueOf(key), value));
        return map;
    }

    private boolean canTransition(String from, String to) {
        if (from.equals(to)) return true;
        return switch (from) {
            case "PENDING_ACTIVATION" -> Set.of("ACTIVE", "REMOVED", "ARCHIVED").contains(to);
            case "ACTIVE" -> Set.of("INACTIVE", "SUSPENDED", "REMOVED", "ARCHIVED").contains(to);
            case "INACTIVE" -> Set.of("ACTIVE", "SUSPENDED", "REMOVED", "ARCHIVED").contains(to);
            case "SUSPENDED" -> Set.of("ACTIVE", "INACTIVE", "REMOVED", "ARCHIVED").contains(to);
            case "REMOVED" -> "ARCHIVED".equals(to);
            default -> false;
        };
    }

    private Comparator<MemberProfile> publicComparator(String sort) {
        if ("displayName_asc".equals(sort)) return Comparator.comparing(profile -> profile.displayNameSnapshot);
        if ("joinedAt_asc".equals(sort)) return Comparator.comparing(profile -> profile.joinedAt);
        if ("updatedAt_desc".equals(sort)) return Comparator.comparing((MemberProfile profile) -> profile.updatedAt).reversed();
        return Comparator.comparing((MemberProfile profile) -> profile.joinedAt).reversed();
    }

    private Comparator<MemberProfile> adminComparator(String sort) {
        if ("displayName_asc".equals(sort)) return Comparator.comparing(profile -> profile.displayNameSnapshot);
        if ("joinedAt_desc".equals(sort)) return Comparator.comparing((MemberProfile profile) -> profile.joinedAt).reversed();
        if ("updatedAt_desc".equals(sort)) return Comparator.comparing((MemberProfile profile) -> profile.updatedAt).reversed();
        return Comparator.comparing((MemberProfile profile) -> profile.createdAt).reversed();
    }

    private boolean matches(MemberProfile profile, String keyword) {
        return contains(profile.displayNameSnapshot, keyword) || contains(profile.minecraftId, keyword) || contains(profile.bio, keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private Map<String, Object> publicSummary(MemberProfile profile) {
        return ProfileController.mapOf(
                "memberId", profile.memberId,
                "displayName", profile.displayNameSnapshot,
                "avatarUrl", profile.avatarUrl,
                "minecraftId", profile.minecraftId,
                "minecraftUuid", profile.minecraftUuid,
                "skinUrl", profile.skinUrl,
                "group", groupSummary(profile.groupId),
                "status", profile.status,
                "joinedAt", profile.joinedAt == null ? null : profile.joinedAt.toString(),
                "bio", profile.bio == null ? null : profile.bio.substring(0, Math.min(160, profile.bio.length())),
                "featuredWorkCount", (int) profile.works.stream().filter(work -> work.publicVisible).count(),
                "milestoneCount", (int) profile.milestones.stream().filter(milestone -> milestone.publicVisible).count(),
                "updatedAt", profile.updatedAt.toString()
        );
    }

    private Map<String, Object> publicDetail(MemberProfile profile) {
        Map<String, Object> detail = publicSummary(profile);
        detail.put("bio", profile.bio);
        detail.put("milestones", profile.milestones.stream().filter(item -> item.publicVisible).map(this::milestoneMap).toList());
        detail.put("workSnapshots", profile.works.stream().filter(item -> item.publicVisible).map(this::workMap).toList());
        detail.put("activitySummary", null);
        detail.put("contributionSummary", null);
        detail.put("createdAt", profile.createdAt.toString());
        return detail;
    }

    private Map<String, Object> currentView(MemberProfile profile) {
        Map<String, Object> map = adminProfile(profile);
        map.remove("adminNote");
        return map;
    }

    private Map<String, Object> adminProfile(MemberProfile profile) {
        return ProfileController.mapOf(
                "memberId", profile.memberId,
                "userId", profile.userId,
                "displayNameSnapshot", profile.displayNameSnapshot,
                "authUserStatusSnapshot", profile.authUserStatusSnapshot,
                "authRolesSnapshot", new ArrayList<>(profile.authRolesSnapshot),
                "avatarUrl", profile.avatarUrl,
                "minecraftId", profile.minecraftId,
                "minecraftUuid", profile.minecraftUuid,
                "skinUrl", profile.skinUrl,
                "group", groupSummary(profile.groupId),
                "status", profile.status,
                "visibility", profile.visibility,
                "joinedAt", profile.joinedAt == null ? null : profile.joinedAt.toString(),
                "bio", profile.bio,
                "adminNote", profile.adminNote,
                "milestones", profile.milestones.stream().map(this::milestoneMap).toList(),
                "workSnapshots", profile.works.stream().map(this::workMap).toList(),
                "createdAt", profile.createdAt.toString(),
                "updatedAt", profile.updatedAt.toString(),
                "archivedAt", profile.archivedAt == null ? null : profile.archivedAt.toString()
        );
    }

    private Map<String, Object> groupSummary(String groupId) {
        return groupId == null ? null : groupMap(groupsById.get(groupId));
    }

    private Map<String, Object> groupMap(MemberGroup group) {
        return ProfileController.mapOf(
                "id", group.id,
                "name", group.name,
                "description", group.description,
                "color", group.color,
                "sortOrder", group.sortOrder,
                "archived", group.archived,
                "createdAt", group.createdAt.toString(),
                "updatedAt", group.updatedAt.toString(),
                "archivedAt", group.archivedAt == null ? null : group.archivedAt.toString()
        );
    }

    private Map<String, Object> milestoneMap(MemberMilestone item) {
        return ProfileController.mapOf("id", item.id, "type", item.type, "title", item.title, "description", item.description, "happenedAt", item.happenedAt.toString(), "publicVisible", item.publicVisible, "sortOrder", item.sortOrder, "createdAt", item.createdAt.toString(), "updatedAt", item.updatedAt.toString());
    }

    private Map<String, Object> workMap(MemberWork item) {
        return ProfileController.mapOf("id", item.id, "type", item.type, "title", item.title, "summary", item.summary, "coverUrl", item.coverUrl, "sourceModule", item.sourceModule, "sourceId", item.sourceId, "publicVisible", item.publicVisible, "sortOrder", item.sortOrder, "createdAt", item.createdAt.toString(), "updatedAt", item.updatedAt.toString());
    }

    private Map<String, Object> auditMap(ProfileAudit audit) {
        return ProfileController.mapOf("id", audit.id, "requestId", audit.requestId, "actorUserId", audit.actorUserId, "actorRole", audit.actorRole, "targetType", "MEMBER_PROFILE", "targetId", audit.targetId, "action", audit.action, "riskLevel", "MEDIUM", "reason", audit.reason, "paramsSummary", null, "beforeState", audit.beforeState, "afterState", audit.afterState, "result", "SUCCESS", "failureReason", null, "createdAt", audit.createdAt.toString());
    }

    private MemberProfile profile(String memberId) {
        MemberProfile profile = profilesById.get(memberId);
        if (profile == null) {
            throw new ApiException(43200, HttpStatus.NOT_FOUND, "profile not found");
        }
        return profile;
    }

    private MemberProfile profileByUserId(String userId) {
        String memberId = memberIdByUserId.get(userId);
        if (memberId == null) {
            throw new ApiException(43200, HttpStatus.NOT_FOUND, "profile not found");
        }
        return profile(memberId);
    }

    private MemberGroup group(String groupId) {
        MemberGroup group = groupsById.get(groupId);
        if (group == null) {
            throw new ApiException(43201, HttpStatus.NOT_FOUND, "group not found");
        }
        return group;
    }

    private void validateProfilePatch(MemberProfile profile) {
        validateUrlNullable(profile.avatarUrl, "avatarUrl");
        validateUrlNullable(profile.skinUrl, "skinUrl");
        validateLengthNullable(profile.bio, 1000, "bio");
        validateLengthNullable(profile.adminNote, 1000, "adminNote");
        if (profile.groupId != null && !groupsById.containsKey(profile.groupId)) throw new ApiException(43201, HttpStatus.NOT_FOUND, "group not found");
        if (profile.visibility != null && !Set.of("PUBLIC", "PRIVATE").contains(profile.visibility)) throw ApiException.badRequest("visibility");
        if (profile.minecraftId != null && !Pattern.matches("[A-Za-z0-9_]{3,16}", profile.minecraftId)) throw ApiException.badRequest("minecraftId");
        if (profile.minecraftUuid != null && !Pattern.matches("[a-f0-9]{32}", profile.minecraftUuid)) throw ApiException.badRequest("minecraftUuid");
    }

    private void ensureMinecraftUnique(String selfMemberId, String minecraftId, String minecraftUuid) {
        for (MemberProfile profile : profilesById.values()) {
            if (Objects.equals(profile.memberId, selfMemberId) || "ARCHIVED".equals(profile.status)) continue;
            if (minecraftId != null && minecraftId.equals(profile.minecraftId)) {
                throw new ApiException(43211, HttpStatus.CONFLICT, "minecraft conflict");
            }
            if (minecraftUuid != null && minecraftUuid.equals(profile.minecraftUuid)) {
                throw new ApiException(43211, HttpStatus.CONFLICT, "minecraft conflict");
            }
        }
    }

    private void ensureGroupNameAvailable(String name, String selfGroupId) {
        for (MemberGroup group : groupsById.values()) {
            if (!group.archived && !Objects.equals(group.id, selfGroupId) && group.name.equalsIgnoreCase(name)) {
                throw new ApiException(43001, HttpStatus.CONFLICT, "group name conflict");
            }
        }
    }

    private void validateGroupFields(String name, String description, String color) {
        validateLength(name, 2, 24, "name");
        validateLengthNullable(description, 200, "description");
        if (color != null && !Pattern.matches("#[A-Fa-f0-9]{6}", color)) {
            throw ApiException.badRequest("color");
        }
    }

    private void audit(String action, AuthUser actor, String targetId, String reason, String before, String after) {
        if (failNextAudit) {
            failNextAudit = false;
            throw new AuditWriteException("profile audit failed");
        }
        audits.add(new ProfileAudit("aud_" + UUID.randomUUID(), RequestIdFilter.currentRequestId(), actor.userId, String.join(",", actor.roles), targetId, action, reason, before, after, now()));
    }

    private Map<String, Object> page(List<Map<String, Object>> rows, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return ProfileController.mapOf("items", rows.subList(from, to), "page", page, "pageSize", pageSize, "total", rows.size());
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(40002, HttpStatus.BAD_REQUEST, "invalid page");
        }
    }

    private String requiredString(Map<String, Object> body, String field) {
        String value = optionalString(body, field);
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(field);
        }
        return value;
    }

    private String optionalString(Map<String, Object> body, String field) {
        if (!body.containsKey(field) || body.get(field) == null) return null;
        return String.valueOf(body.get(field));
    }

    private String optionalStringOrDefault(Map<String, Object> body, String field, String fallback) {
        String value = optionalString(body, field);
        return value == null ? fallback : value;
    }

    private int intValue(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value instanceof Number number) return number.intValue();
        throw ApiException.badRequest(field);
    }

    private int intOrDefault(Map<String, Object> body, String field, int fallback) {
        return body.containsKey(field) ? intValue(body, field) : fallback;
    }

    private boolean booleanValue(Map<String, Object> body, String field) {
        Object value = body.get(field);
        if (value instanceof Boolean bool) return bool;
        throw ApiException.badRequest(field);
    }

    private Instant optionalInstant(Map<String, Object> body, String field, Instant fallback) {
        String value = optionalString(body, field);
        return value == null ? fallback : Instant.parse(value);
    }

    private void validateUrlNullable(String value, String field) {
        if (value == null) return;
        if (value.length() > 500 || !(value.startsWith("http://") || value.startsWith("https://") || value.startsWith("/"))) {
            throw ApiException.badRequest(field);
        }
    }

    private void validateLength(String value, int min, int max, String field) {
        if (value == null || value.length() < min || value.length() > max) {
            throw ApiException.badRequest(field);
        }
    }

    private void validateLengthNullable(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw ApiException.badRequest(field);
        }
    }

    private String signature(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String state(MemberProfile profile) {
        return profile.memberId + "|" + profile.userId + "|" + profile.displayNameSnapshot + "|" + profile.status + "|" + profile.visibility + "|" + profile.groupId + "|" + profile.minecraftUuid;
    }

    private Instant now() {
        return Instant.now();
    }

    void failNextPublicRead() {
        failNextPublicRead = true;
    }

    void failNextAudit() {
        failNextAudit = true;
    }

    List<String> auditActions() {
        return audits.stream().map(audit -> audit.action).toList();
    }

    boolean usesAuthImplementation() {
        return false;
    }

    String memberIdByUserId(String userId) {
        return memberIdByUserId.get(userId);
    }

    boolean memberExistsByUserId(String userId) {
        return memberIdByUserId.containsKey(userId);
    }

    String groupIdByName(String name) {
        return groupsById.values().stream().filter(group -> group.name.equals(name)).findFirst().orElseThrow().id;
    }

    String minecraftUuidByUserId(String userId) {
        return profileByUserId(userId).minecraftUuid;
    }

    String profileBioByUserId(String userId) {
        return profileByUserId(userId).bio;
    }

    String firstMilestoneId(String memberId) {
        return profile(memberId).milestones.getFirst().id;
    }

    String firstWorkId(String memberId) {
        return profile(memberId).works.getFirst().id;
    }

    int milestoneCount(String memberId) {
        return profile(memberId).milestones.size();
    }

    int workCount(String memberId) {
        return profile(memberId).works.size();
    }
}

@RestControllerAdvice
class ProfileExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException exception) {
        Map<String, Object> body = ProfileController.envelope(exception.code, exception.getMessage(), null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        if (exception.code == 40001) {
            body.put("errors", List.of(Map.of("field", exception.field == null ? "request" : exception.field, "reason", exception.getMessage())));
        }
        return ResponseEntity.status(exception.status).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, Object>> methodNotSupported(HttpRequestMethodNotSupportedException exception) {
        Map<String, Object> body = ProfileController.envelope(40000, "invalid request", null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> any(Exception exception) {
        int code = exception instanceof AuditWriteException ? 51201 : 51200;
        Map<String, Object> body = ProfileController.envelope(code, exception instanceof AuditWriteException ? "profile audit failed" : "profile internal error", null);
        body.put("requestId", RequestIdFilter.currentRequestId());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

@Component
class RequestIdFilter extends OncePerRequestFilter {
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    static String currentRequestId() {
        String id = REQUEST_ID.get();
        return id == null ? "req_unknown" : id;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = "req_" + UUID.randomUUID();
        }
        REQUEST_ID.set(requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            REQUEST_ID.remove();
        }
    }
}

class ApiException extends RuntimeException {
    final int code;
    final HttpStatus status;
    final String field;

    ApiException(int code, HttpStatus status, String message) {
        this(code, status, message, null);
    }

    ApiException(int code, HttpStatus status, String message, String field) {
        super(message);
        this.code = code;
        this.status = status;
        this.field = field;
    }

    static ApiException badRequest(String field) {
        return new ApiException(40001, HttpStatus.BAD_REQUEST, "invalid request", field);
    }
}

class AuditWriteException extends RuntimeException {
    AuditWriteException(String message) {
        super(message);
    }
}

class AuthUser {
    final String userId;
    final String displayName;
    final LinkedHashSet<String> roles;
    final LinkedHashSet<String> permissions;
    final String status;
    final MinecraftBinding minecraftBinding;

    AuthUser(String userId, String displayName, Set<String> roles, Set<String> permissions, String status, MinecraftBinding minecraftBinding) {
        this.userId = userId;
        this.displayName = displayName;
        this.roles = new LinkedHashSet<>(roles);
        this.permissions = new LinkedHashSet<>(permissions);
        this.status = status;
        this.minecraftBinding = minecraftBinding;
    }

    AuthUser copy() {
        return new AuthUser(userId, displayName, roles, permissions, status, minecraftBinding);
    }
}

class MinecraftBinding {
    final String minecraftId;
    final String minecraftUuid;
    final Instant verifiedAt;
    final String source;

    MinecraftBinding(String minecraftId, String minecraftUuid, Instant verifiedAt, String source) {
        this.minecraftId = minecraftId;
        this.minecraftUuid = minecraftUuid;
        this.verifiedAt = verifiedAt;
        this.source = source;
    }
}

class MemberGroup {
    final String id;
    String name;
    String description;
    String color;
    int sortOrder;
    boolean archived;
    final Instant createdAt;
    Instant updatedAt;
    Instant archivedAt;

    MemberGroup(String id, String name, String description, String color, int sortOrder, boolean archived, Instant createdAt, Instant updatedAt, Instant archivedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
        this.sortOrder = sortOrder;
        this.archived = archived;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.archivedAt = archivedAt;
    }

    MemberGroup copy() {
        return new MemberGroup(id, name, description, color, sortOrder, archived, createdAt, updatedAt, archivedAt);
    }
}

class MemberProfile {
    final String memberId;
    final String userId;
    String displayNameSnapshot;
    String authUserStatusSnapshot;
    LinkedHashSet<String> authRolesSnapshot;
    String avatarUrl;
    String minecraftId;
    String minecraftUuid;
    String skinUrl;
    String groupId;
    String status;
    String visibility;
    Instant joinedAt;
    String bio;
    String adminNote;
    final Instant createdAt;
    Instant updatedAt;
    Instant archivedAt;
    List<MemberMilestone> milestones = new ArrayList<>();
    List<MemberWork> works = new ArrayList<>();

    MemberProfile(String memberId, String userId, String displayNameSnapshot, String authUserStatusSnapshot, Set<String> authRolesSnapshot, String avatarUrl, String minecraftId, String minecraftUuid, String skinUrl, String groupId, String status, String visibility, Instant joinedAt, String bio, String adminNote, Instant createdAt, Instant updatedAt, Instant archivedAt) {
        this.memberId = memberId;
        this.userId = userId;
        this.displayNameSnapshot = displayNameSnapshot;
        this.authUserStatusSnapshot = authUserStatusSnapshot;
        this.authRolesSnapshot = new LinkedHashSet<>(authRolesSnapshot);
        this.avatarUrl = avatarUrl;
        this.minecraftId = minecraftId;
        this.minecraftUuid = minecraftUuid;
        this.skinUrl = skinUrl;
        this.groupId = groupId;
        this.status = status;
        this.visibility = visibility;
        this.joinedAt = joinedAt;
        this.bio = bio;
        this.adminNote = adminNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.archivedAt = archivedAt;
    }

    MemberProfile copy() {
        MemberProfile copy = new MemberProfile(memberId, userId, displayNameSnapshot, authUserStatusSnapshot, authRolesSnapshot, avatarUrl, minecraftId, minecraftUuid, skinUrl, groupId, status, visibility, joinedAt, bio, adminNote, createdAt, updatedAt, archivedAt);
        copy.milestones = new ArrayList<>(milestones);
        copy.works = new ArrayList<>(works);
        return copy;
    }
}

class MemberMilestone {
    final String id;
    final String type;
    final String title;
    final String description;
    final Instant happenedAt;
    final boolean publicVisible;
    final int sortOrder;
    final Instant createdAt;
    final Instant updatedAt;

    MemberMilestone(String id, String type, String title, String description, Instant happenedAt, boolean publicVisible, int sortOrder, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.happenedAt = happenedAt;
        this.publicVisible = publicVisible;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

class MemberWork {
    final String id;
    final String type;
    final String title;
    final String summary;
    final String coverUrl;
    final String sourceModule;
    final String sourceId;
    final boolean publicVisible;
    final int sortOrder;
    final Instant createdAt;
    final Instant updatedAt;

    MemberWork(String id, String type, String title, String summary, String coverUrl, String sourceModule, String sourceId, boolean publicVisible, int sortOrder, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.summary = summary;
        this.coverUrl = coverUrl;
        this.sourceModule = sourceModule;
        this.sourceId = sourceId;
        this.publicVisible = publicVisible;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

class ProfileAudit {
    final String id;
    final String requestId;
    final String actorUserId;
    final String actorRole;
    final String targetId;
    final String action;
    final String reason;
    final String beforeState;
    final String afterState;
    final Instant createdAt;

    ProfileAudit(String id, String requestId, String actorUserId, String actorRole, String targetId, String action, String reason, String beforeState, String afterState, Instant createdAt) {
        this.id = id;
        this.requestId = requestId;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.targetId = targetId;
        this.action = action;
        this.reason = reason;
        this.beforeState = beforeState;
        this.afterState = afterState;
        this.createdAt = createdAt;
    }
}

class IdempotencyRecord {
    final String signature;
    final Map<String, Object> payload;

    IdempotencyRecord(String signature, Map<String, Object> payload) {
        this.signature = signature;
        this.payload = payload;
    }
}
