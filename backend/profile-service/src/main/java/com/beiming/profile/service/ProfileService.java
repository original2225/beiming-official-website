package com.beiming.profile.service;

import com.beiming.profile.api.ApiException;
import com.beiming.profile.api.PageResponse;
import com.beiming.profile.domain.AuthContext;
import com.beiming.profile.domain.AuditLog;
import com.beiming.profile.domain.MemberAchievement;
import com.beiming.profile.domain.MemberGroup;
import com.beiming.profile.domain.MemberProfile;
import com.beiming.profile.domain.MemberStatus;
import com.beiming.profile.domain.MemberWork;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
    private final AtomicInteger groupSequence = new AtomicInteger(1);
    private final AtomicInteger profileSequence = new AtomicInteger(1);
    private final AtomicInteger achievementSequence = new AtomicInteger(1);
    private final AtomicInteger workSequence = new AtomicInteger(1);
    private final AtomicInteger auditSequence = new AtomicInteger(1);
    private final Map<String, MemberGroup> groupsById = new LinkedHashMap<>();
    private final Map<String, MemberProfile> profilesById = new LinkedHashMap<>();
    private final Map<String, String> profileIdByAuthUserId = new LinkedHashMap<>();
    private final Map<String, String> profileIdByMinecraftId = new LinkedHashMap<>();
    private final Map<String, String> profileIdByMinecraftUuid = new LinkedHashMap<>();
    private final List<AuditLog> auditLogs = new ArrayList<>();

    public synchronized PageResponse<Map<String, Object>> publicMembers(int page, int pageSize, String keyword,
            String memberGroupId, MemberStatus status) {
        if (status != null && status != MemberStatus.ACTIVE) {
            return page(List.of(), page, pageSize);
        }
        String normalizedKeyword = normalize(keyword);
        List<Map<String, Object>> items = profilesById.values().stream()
                .filter(profile -> profile.status == MemberStatus.ACTIVE && profile.publicVisible)
                .filter(profile -> memberGroupId == null || memberGroupId.equals(profile.memberGroupId))
                .filter(profile -> normalizedKeyword == null || normalize(profile.displayName).contains(normalizedKeyword)
                        || normalize(profile.minecraftId).contains(normalizedKeyword)
                        || normalize(profile.usernameSnapshot).contains(normalizedKeyword))
                .sorted(Comparator.comparing((MemberProfile profile) -> profile.joinedAt == null ? profile.createdAt : profile.joinedAt).reversed())
                .map(this::profileResponse)
                .toList();
        return page(items, page, pageSize);
    }

    public synchronized Map<String, Object> publicMember(String profileId) {
        MemberProfile profile = profileById(profileId);
        if (profile.status != MemberStatus.ACTIVE || !profile.publicVisible) {
            throw new ApiException(43000, HttpStatus.NOT_FOUND, "profile not found");
        }
        return profileResponse(profile);
    }

    public synchronized Map<String, Object> me(AuthContext auth) {
        requireLogin(auth);
        String profileId = profileIdByAuthUserId.get(auth.userId());
        if (profileId == null) {
            throw new ApiException(43000, HttpStatus.NOT_FOUND, "profile not found");
        }
        return profileResponse(profilesById.get(profileId));
    }

    public synchronized Map<String, Object> updateMe(AuthContext auth, Map<String, Object> body) {
        requireLogin(auth);
        String profileId = profileIdByAuthUserId.get(auth.userId());
        if (profileId == null) {
            throw new ApiException(43000, HttpStatus.NOT_FOUND, "profile not found");
        }
        MemberProfile profile = profilesById.get(profileId);
        Object before = profileResponse(profile);
        applySelfFields(profile, body);
        audit(auth, "PROFILE", profile.id, "UPDATE_SELF_PROFILE", "MEDIUM", null, before, profileResponse(profile), "SUCCESS");
        return profileResponse(profile);
    }

    public synchronized PageResponse<Map<String, Object>> adminMembers(AuthContext auth, int page, int pageSize, String keyword,
            String memberGroupId, MemberStatus status, Boolean publicVisible) {
        requireAdmin(auth);
        String normalizedKeyword = normalize(keyword);
        List<Map<String, Object>> items = profilesById.values().stream()
                .filter(profile -> memberGroupId == null || memberGroupId.equals(profile.memberGroupId))
                .filter(profile -> status == null || profile.status == status)
                .filter(profile -> publicVisible == null || profile.publicVisible == publicVisible)
                .filter(profile -> normalizedKeyword == null || normalize(profile.displayName).contains(normalizedKeyword)
                        || normalize(profile.minecraftId).contains(normalizedKeyword)
                        || normalize(profile.usernameSnapshot).contains(normalizedKeyword))
                .sorted(Comparator.comparing((MemberProfile profile) -> profile.createdAt).reversed())
                .map(this::profileResponse)
                .toList();
        return page(items, page, pageSize);
    }

    public synchronized MutationResult createMember(AuthContext auth, Map<String, Object> body, boolean internal) {
        if (internal) {
            requireService(auth);
            Optional<MemberProfile> existing = Optional.ofNullable(profileIdByAuthUserId.get(text(body, "authUserId")))
                    .map(profilesById::get);
            if (existing.isPresent()) {
                applyAdminFields(existing.get(), body, true);
                existing.get().status = MemberStatus.ACTIVE;
                existing.get().updatedAt = Instant.now();
                audit(auth, "PROFILE", existing.get().id, "INTERNAL_ACTIVATE_PROFILE", "MEDIUM", null, null,
                        profileResponse(existing.get()), "SUCCESS");
                return new MutationResult(profileResponse(existing.get()), false);
            }
        } else {
            requireAdmin(auth);
        }
        String authUserId = required(body, "authUserId");
        if (profileIdByAuthUserId.containsKey(authUserId)) {
            throw new ApiException(43100, HttpStatus.CONFLICT, "member profile already exists");
        }
        String groupId = required(body, "memberGroupId");
        requireGroup(groupId);
        assertMinecraftUnused(null, text(body, "minecraftId"), text(body, "minecraftUuid"));
        Instant now = Instant.now();
        MemberProfile profile = new MemberProfile(nextProfileId(), authUserId, now);
        applyAdminFields(profile, body, true);
        if (internal) {
            profile.status = MemberStatus.ACTIVE;
        }
        profilesById.put(profile.id, profile);
        profileIdByAuthUserId.put(profile.authUserId, profile.id);
        indexMinecraft(profile);
        audit(auth, "PROFILE", profile.id, internal ? "INTERNAL_ACTIVATE_PROFILE" : "CREATE_PROFILE", "MEDIUM", null, null,
                profileResponse(profile), "SUCCESS");
        return new MutationResult(profileResponse(profile), true);
    }

    public synchronized Map<String, Object> adminMember(AuthContext auth, String profileId) {
        requireAdmin(auth);
        return profileResponse(profileById(profileId));
    }

    public synchronized Map<String, Object> updateAdminMember(AuthContext auth, String profileId, Map<String, Object> body) {
        requireAdmin(auth);
        MemberProfile profile = profileById(profileId);
        Object before = profileResponse(profile);
        applyAdminFields(profile, body, false);
        audit(auth, "PROFILE", profile.id, "UPDATE_PROFILE", "MEDIUM", null, before, profileResponse(profile), "SUCCESS");
        return profileResponse(profile);
    }

    public synchronized Map<String, Object> updateStatus(AuthContext auth, String profileId, MemberStatus status, String reason) {
        requireAdmin(auth);
        MemberProfile profile = profileById(profileId);
        if (reason == null || reason.isBlank()) {
            throw new ApiException(40001, HttpStatus.BAD_REQUEST, "invalid request");
        }
        if (!canTransition(profile.status, status)) {
            throw new ApiException(43103, HttpStatus.CONFLICT, "member status transition not allowed");
        }
        Object before = profileResponse(profile);
        profile.status = status;
        profile.updatedAt = Instant.now();
        audit(auth, "PROFILE", profile.id, "UPDATE_PROFILE_STATUS", "HIGH", reason, before, profileResponse(profile),
                "SUCCESS");
        return profileResponse(profile);
    }

    public synchronized Map<String, Object> createGroup(AuthContext auth, Map<String, Object> body) {
        requireAdmin(auth);
        String name = required(body, "name");
        if (groupsById.values().stream().anyMatch(group -> normalize(group.name).equals(normalize(name)))) {
            throw new ApiException(43104, HttpStatus.CONFLICT, "member group name already exists");
        }
        Instant now = Instant.now();
        MemberGroup group = new MemberGroup(nextGroupId(), name, text(body, "description"), intValue(body, "sortOrder", 0),
                boolValue(body, "enabled", true), now);
        groupsById.put(group.id, group);
        audit(auth, "PROFILE_GROUP", group.id, "CREATE_PROFILE_GROUP", "MEDIUM", null, null, groupResponse(group),
                "SUCCESS");
        return groupResponse(group);
    }

    public synchronized List<Map<String, Object>> groups(AuthContext auth) {
        requireAdmin(auth);
        return groupsById.values().stream()
                .sorted(Comparator.comparingInt(group -> group.sortOrder))
                .map(this::groupResponse)
                .toList();
    }

    public synchronized Map<String, Object> updateGroup(AuthContext auth, String groupId, Map<String, Object> body) {
        requireAdmin(auth);
        MemberGroup group = requireGroup(groupId);
        Object before = groupResponse(group);
        if (body.containsKey("enabled") && !boolValue(body, "enabled", group.enabled) && groupInUse(groupId)) {
            throw new ApiException(43105, HttpStatus.CONFLICT, "member group is in use");
        }
        if (body.containsKey("name")) {
            String name = required(body, "name");
            if (groupsById.values().stream().anyMatch(existing -> !existing.id.equals(groupId)
                    && normalize(existing.name).equals(normalize(name)))) {
                throw new ApiException(43104, HttpStatus.CONFLICT, "member group name already exists");
            }
            group.name = name;
        }
        if (body.containsKey("description")) {
            group.description = text(body, "description");
        }
        if (body.containsKey("sortOrder")) {
            group.sortOrder = intValue(body, "sortOrder", group.sortOrder);
        }
        if (body.containsKey("enabled")) {
            group.enabled = boolValue(body, "enabled", group.enabled);
        }
        group.updatedAt = Instant.now();
        audit(auth, "PROFILE_GROUP", group.id, "UPDATE_PROFILE_GROUP", "MEDIUM", null, before, groupResponse(group),
                "SUCCESS");
        return groupResponse(group);
    }

    public synchronized long auditCount() {
        return auditLogs.size();
    }

    public synchronized String lastAuditReason() {
        if (auditLogs.isEmpty()) {
            return null;
        }
        return auditLogs.getLast().reason();
    }

    private void applySelfFields(MemberProfile profile, Map<String, Object> body) {
        if (body.containsKey("displayName")) {
            profile.displayName = required(body, "displayName");
        }
        if (body.containsKey("avatarUrl")) {
            profile.avatarUrl = text(body, "avatarUrl");
        }
        if (body.containsKey("skinUrl")) {
            profile.skinUrl = text(body, "skinUrl");
        }
        if (body.containsKey("bio")) {
            profile.bio = text(body, "bio");
        }
        if (body.containsKey("publicVisible")) {
            profile.publicVisible = boolValue(body, "publicVisible", profile.publicVisible);
        }
        if (body.containsKey("achievements")) {
            profile.achievements = achievements(body.get("achievements"));
        }
        if (body.containsKey("works")) {
            profile.works = works(body.get("works"));
        }
        profile.updatedAt = Instant.now();
    }

    private void applyAdminFields(MemberProfile profile, Map<String, Object> body, boolean creating) {
        if (creating || body.containsKey("usernameSnapshot")) {
            profile.usernameSnapshot = required(body, "usernameSnapshot");
        }
        if (creating || body.containsKey("displayName")) {
            profile.displayName = required(body, "displayName");
        }
        if (creating || body.containsKey("minecraftId") || body.containsKey("minecraftUuid")) {
            String minecraftId = required(body, "minecraftId");
            String minecraftUuid = required(body, "minecraftUuid");
            assertMinecraftUnused(profile.id, minecraftId, minecraftUuid);
            unindexMinecraft(profile);
            profile.minecraftId = minecraftId;
            profile.minecraftUuid = minecraftUuid;
            indexMinecraft(profile);
        }
        if (body.containsKey("avatarUrl")) {
            profile.avatarUrl = text(body, "avatarUrl");
        }
        if (body.containsKey("skinUrl")) {
            profile.skinUrl = text(body, "skinUrl");
        }
        if (creating || body.containsKey("memberGroupId")) {
            profile.memberGroupId = requireGroup(required(body, "memberGroupId")).id;
        }
        if (creating || body.containsKey("status")) {
            profile.status = enumValue(MemberStatus.class, Objects.toString(body.getOrDefault("status", "PENDING_ACTIVATION")));
        }
        if (creating || body.containsKey("publicVisible")) {
            profile.publicVisible = boolValue(body, "publicVisible", false);
        }
        if (creating || body.containsKey("joinedAt")) {
            profile.joinedAt = instantValue(body.get("joinedAt"), Instant.now());
        }
        if (body.containsKey("bio")) {
            profile.bio = text(body, "bio");
        }
        if (body.containsKey("achievements")) {
            profile.achievements = achievements(body.get("achievements"));
        }
        if (body.containsKey("works")) {
            profile.works = works(body.get("works"));
        }
        if (body.containsKey("activitySummary")) {
            profile.activitySummary = text(body, "activitySummary");
        }
        if (body.containsKey("contributionSummary")) {
            profile.contributionSummary = text(body, "contributionSummary");
        }
        profile.updatedAt = Instant.now();
    }

    private boolean canTransition(MemberStatus from, MemberStatus to) {
        if (to == null || from == MemberStatus.ARCHIVED) {
            return false;
        }
        return switch (from) {
            case PENDING_ACTIVATION -> to == MemberStatus.ACTIVE;
            case ACTIVE -> to == MemberStatus.INACTIVE || to == MemberStatus.REMOVED || to == MemberStatus.BANNED
                    || to == MemberStatus.ARCHIVED;
            case INACTIVE -> to == MemberStatus.ACTIVE || to == MemberStatus.REMOVED || to == MemberStatus.ARCHIVED;
            case REMOVED, BANNED -> to == MemberStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
    }

    private void requireLogin(AuthContext auth) {
        if (auth == null || auth.token() == null || auth.token().isBlank()) {
            throw new ApiException(41000, HttpStatus.UNAUTHORIZED, "unauthorized");
        }
    }

    private void requireAdmin(AuthContext auth) {
        requireLogin(auth);
        if (!auth.isAdmin()) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role permission denied");
        }
    }

    private void requireService(AuthContext auth) {
        requireLogin(auth);
        if (!auth.isService()) {
            throw new ApiException(42001, HttpStatus.FORBIDDEN, "role permission denied");
        }
    }

    private MemberProfile profileById(String profileId) {
        MemberProfile profile = profilesById.get(profileId);
        if (profile == null) {
            throw new ApiException(43000, HttpStatus.NOT_FOUND, "profile not found");
        }
        return profile;
    }

    private MemberGroup requireGroup(String groupId) {
        MemberGroup group = groupsById.get(groupId);
        if (group == null) {
            throw new ApiException(43102, HttpStatus.NOT_FOUND, "member group not found");
        }
        return group;
    }

    private boolean groupInUse(String groupId) {
        return profilesById.values().stream().anyMatch(profile -> groupId.equals(profile.memberGroupId));
    }

    private void assertMinecraftUnused(String currentProfileId, String minecraftId, String minecraftUuid) {
        String existingById = profileIdByMinecraftId.get(normalize(minecraftId));
        String existingByUuid = profileIdByMinecraftUuid.get(normalize(minecraftUuid));
        if (existingById != null && !existingById.equals(currentProfileId)
                || existingByUuid != null && !existingByUuid.equals(currentProfileId)) {
            throw new ApiException(43101, HttpStatus.CONFLICT, "minecraft identity already used");
        }
    }

    private void indexMinecraft(MemberProfile profile) {
        if (profile.minecraftId != null) {
            profileIdByMinecraftId.put(normalize(profile.minecraftId), profile.id);
        }
        if (profile.minecraftUuid != null) {
            profileIdByMinecraftUuid.put(normalize(profile.minecraftUuid), profile.id);
        }
    }

    private void unindexMinecraft(MemberProfile profile) {
        if (profile.minecraftId != null) {
            profileIdByMinecraftId.remove(normalize(profile.minecraftId));
        }
        if (profile.minecraftUuid != null) {
            profileIdByMinecraftUuid.remove(normalize(profile.minecraftUuid));
        }
    }

    private Map<String, Object> profileResponse(MemberProfile profile) {
        MemberGroup group = profile.memberGroupId == null ? null : groupsById.get(profile.memberGroupId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", profile.id);
        response.put("authUserId", profile.authUserId);
        response.put("usernameSnapshot", profile.usernameSnapshot);
        response.put("displayName", profile.displayName);
        response.put("minecraftId", profile.minecraftId);
        response.put("minecraftUuid", profile.minecraftUuid);
        response.put("avatarUrl", profile.avatarUrl);
        response.put("skinUrl", profile.skinUrl);
        response.put("memberGroupId", profile.memberGroupId);
        response.put("memberGroupName", group == null ? null : group.name);
        response.put("status", profile.status == null ? null : profile.status.name());
        response.put("publicVisible", profile.publicVisible);
        response.put("joinedAt", profile.joinedAt);
        response.put("bio", profile.bio);
        response.put("achievements", profile.achievements);
        response.put("works", profile.works);
        response.put("activitySummary", profile.activitySummary);
        response.put("contributionSummary", profile.contributionSummary);
        response.put("createdAt", profile.createdAt);
        response.put("updatedAt", profile.updatedAt);
        return response;
    }

    private Map<String, Object> groupResponse(MemberGroup group) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", group.id);
        response.put("name", group.name);
        response.put("description", group.description);
        response.put("sortOrder", group.sortOrder);
        response.put("enabled", group.enabled);
        response.put("createdAt", group.createdAt);
        response.put("updatedAt", group.updatedAt);
        return response;
    }

    private List<MemberAchievement> achievements(Object value) {
        if (!(value instanceof List<?> rawItems)) {
            return List.of();
        }
        List<MemberAchievement> items = new ArrayList<>();
        for (Object rawItem : rawItems) {
            Map<?, ?> item = (Map<?, ?>) rawItem;
            items.add(new MemberAchievement(nextAchievementId(), Objects.toString(item.get("title"), ""),
                    Objects.toString(item.get("description"), ""), instantValue(item.get("occurredAt"), Instant.now())));
        }
        return items;
    }

    private List<MemberWork> works(Object value) {
        if (!(value instanceof List<?> rawItems)) {
            return List.of();
        }
        List<MemberWork> items = new ArrayList<>();
        for (Object rawItem : rawItems) {
            Map<?, ?> item = (Map<?, ?>) rawItem;
            items.add(new MemberWork(nextWorkId(), Objects.toString(item.get("title"), ""),
                    Objects.toString(item.get("description"), ""), Objects.toString(item.get("coverUrl"), null),
                    Objects.toString(item.get("linkUrl"), null),
                    item.get("sortOrder") instanceof Number number ? number.intValue() : 0));
        }
        return items;
    }

    private <T> PageResponse<T> page(List<T> items, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int from = Math.min((safePage - 1) * safePageSize, items.size());
        int to = Math.min(from + safePageSize, items.size());
        return new PageResponse<>(items.subList(from, to), safePage, safePageSize, items.size());
    }

    private String required(Map<String, Object> body, String field) {
        String value = text(body, field);
        if (value == null || value.isBlank()) {
            throw new ApiException(40001, HttpStatus.BAD_REQUEST, "invalid request");
        }
        return value;
    }

    private String text(Map<String, Object> body, String field) {
        Object value = body.get(field);
        return value == null ? null : Objects.toString(value);
    }

    private boolean boolValue(Map<String, Object> body, String field, boolean fallback) {
        Object value = body.get(field);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value == null ? fallback : Boolean.parseBoolean(Objects.toString(value));
    }

    private int intValue(Map<String, Object> body, String field, int fallback) {
        Object value = body.get(field);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? fallback : Integer.parseInt(Objects.toString(value));
    }

    private Instant instantValue(Object value, Instant fallback) {
        return value == null ? fallback : Instant.parse(Objects.toString(value));
    }

    private <E extends Enum<E>> E enumValue(Class<E> enumType, String value) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (RuntimeException exception) {
            throw new ApiException(40001, HttpStatus.BAD_REQUEST, "invalid request");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String nextGroupId() {
        return "group_" + "%03d".formatted(groupSequence.getAndIncrement());
    }

    private String nextProfileId() {
        return "profile_" + "%03d".formatted(profileSequence.getAndIncrement());
    }

    private String nextAchievementId() {
        return "achievement_" + "%03d".formatted(achievementSequence.getAndIncrement());
    }

    private String nextWorkId() {
        return "work_" + "%03d".formatted(workSequence.getAndIncrement());
    }

    private void audit(AuthContext auth, String targetType, String targetId, String action, String riskLevel, String reason,
            Object beforeState, Object afterState, String result) {
        auditLogs.add(new AuditLog("audit_" + "%03d".formatted(auditSequence.getAndIncrement()),
                auth == null ? null : auth.userId(), auth == null ? null : auth.role(), targetType, targetId, action,
                riskLevel, reason, beforeState, afterState, result, Instant.now()));
    }

    public record MutationResult(Map<String, Object> data, boolean created) {
    }
}
