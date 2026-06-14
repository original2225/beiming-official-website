package cn.beiming.changelog;

import cn.beiming.engagement.TrustedGatewayAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/changelog")
class ChangelogController {
    private final ChangelogStore store;
    private final ChangelogAuth auth;
    private final ChangelogProperties properties;

    ChangelogController(ChangelogStore store, ChangelogAuth auth, ChangelogProperties properties) {
        this.store = store;
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/releases")
    ResponseEntity<Map<String, Object>> publicReleases(HttpServletRequest request,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int pageSize,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String visibility,
                                                       @RequestParam(required = false) String impactLevel,
                                                       @RequestParam(required = false) String minecraftVersion,
                                                       @RequestParam(required = false) String tag,
                                                       @RequestParam(required = false) String from,
                                                       @RequestParam(required = false) String to,
                                                       @RequestParam(required = false) String sort) {
        validatePage(page, pageSize);
        validateSort(sort, "releasedAt_desc", "releasedAt_asc", "effectiveAt_desc", "updatedAt_desc", "impactLevel_desc");
        if (type != null) {
            validateReleaseType(type);
        }
        if (visibility != null) {
            validateVisibility(visibility);
        }
        if (impactLevel != null) {
            validateEnum(impactLevel, List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        }
        Instant fromInstant = parseOptionalInstant(from);
        Instant toInstant = parseOptionalInstant(to);
        validateRange(fromInstant, toInstant);
        List<Map<String, Object>> items = store.releases.values().stream()
                .filter(ChangelogReleaseRecord::isPublicVisible)
                .filter(release -> keyword == null || release.title.contains(keyword) || release.summary.contains(keyword) || release.body.contains(keyword))
                .filter(release -> type == null || release.type.equals(type))
                .filter(release -> visibility == null || release.visibility.equals(visibility))
                .filter(release -> impactLevel == null || release.impactLevel.equals(impactLevel))
                .filter(release -> minecraftVersion == null || Objects.equals(release.minecraftVersion, minecraftVersion))
                .filter(release -> tag == null || release.matchesTag(tag))
                .filter(release -> inReleaseRange(release, fromInstant, toInstant))
                .sorted(releaseComparator(sort))
                .map(ChangelogReleaseRecord::publicView)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/releases/{releaseIdOrSlug}")
    ResponseEntity<Map<String, Object>> publicRelease(HttpServletRequest request, @PathVariable String releaseIdOrSlug) {
        ChangelogReleaseRecord release = store.findRelease(releaseIdOrSlug).filter(ChangelogReleaseRecord::isPublicVisible)
                .orElseThrow(() -> new ChangelogException(HttpStatus.NOT_FOUND, 49300, "changelog release not found"));
        return ok(request, release.publicView());
    }

    @GetMapping("/versions/latest")
    ResponseEntity<Map<String, Object>> latest(HttpServletRequest request,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(required = false) String visibility,
                                               @RequestParam(required = false) String minecraftVersion) {
        if (type != null) {
            validateReleaseType(type);
        }
        if (visibility != null) {
            validateVisibility(visibility);
        }
        Object data = store.releases.values().stream()
                .filter(ChangelogReleaseRecord::isPublicVisible)
                .filter(release -> type == null || release.type.equals(type))
                .filter(release -> visibility == null || release.visibility.equals(visibility))
                .filter(release -> minecraftVersion == null || Objects.equals(release.minecraftVersion, minecraftVersion))
                .sorted(releaseComparator("releasedAt_desc"))
                .findFirst()
                .map(ChangelogReleaseRecord::summaryView)
                .orElse(null);
        return ok(request, data);
    }

    @GetMapping("/tags")
    ResponseEntity<Map<String, Object>> tags(HttpServletRequest request) {
        Set<String> types = new LinkedHashSet<>();
        Set<String> groupTypes = new LinkedHashSet<>();
        Set<String> impactLevels = new LinkedHashSet<>();
        Set<String> minecraftVersions = new LinkedHashSet<>();
        Set<String> components = new LinkedHashSet<>();
        store.releases.values().stream().filter(ChangelogReleaseRecord::isPublicVisible).forEach(release -> {
            types.add(release.type);
            impactLevels.add(release.impactLevel);
            if (release.minecraftVersion != null) {
                minecraftVersions.add(release.minecraftVersion);
            }
            release.groups.forEach(group -> {
                groupTypes.add(group.type);
                group.items.forEach(item -> {
                    if (item.publicSafe && item.component != null) {
                        components.add(item.component);
                    }
                });
            });
            release.pluginVersions.forEach(plugin -> addText(components, plugin.get("name")));
            release.resourcePackVersions.forEach(pack -> addText(components, pack.get("name")));
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("types", types);
        data.put("groupTypes", groupTypes);
        data.put("impactLevels", impactLevels);
        data.put("minecraftVersions", minecraftVersions);
        data.put("components", components);
        data.put("tags", components);
        return ok(request, data);
    }

    @GetMapping("/changes")
    ResponseEntity<Map<String, Object>> changes(HttpServletRequest request,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int pageSize,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String groupType,
                                                @RequestParam(required = false) String severity,
                                                @RequestParam(required = false) String component,
                                                @RequestParam(required = false) String releaseType,
                                                @RequestParam(required = false) String from,
                                                @RequestParam(required = false) String to,
                                                @RequestParam(required = false) String sort) {
        validatePage(page, pageSize);
        validateSort(sort, "releasedAt_desc", "releasedAt_asc", "severity_desc");
        if (groupType != null) {
            validateEnum(groupType, List.of("ADDED", "CHANGED", "DEPRECATED", "REMOVED", "FIXED", "SECURITY", "PERFORMANCE", "KNOWN_ISSUE"));
        }
        if (severity != null) {
            validateEnum(severity, List.of("INFO", "MINOR", "MAJOR", "BREAKING", "SECURITY"));
        }
        if (releaseType != null) {
            validateReleaseType(releaseType);
        }
        Instant fromInstant = parseOptionalInstant(from);
        Instant toInstant = parseOptionalInstant(to);
        validateRange(fromInstant, toInstant);
        List<Map<String, Object>> items = new ArrayList<>();
        store.releases.values().stream()
                .filter(ChangelogReleaseRecord::isPublicVisible)
                .filter(release -> releaseType == null || release.type.equals(releaseType))
                .filter(release -> inReleaseRange(release, fromInstant, toInstant))
                .forEach(release -> release.groups.stream()
                        .filter(group -> groupType == null || group.type.equals(groupType))
                        .forEach(group -> group.items.stream()
                                .filter(item -> severity == null || item.severity.equals(severity))
                                .filter(item -> component == null || Objects.equals(item.component, component))
                                .filter(item -> keyword == null || item.title.contains(keyword) || item.description.contains(keyword))
                                .map(item -> item.publicSearchView(release, group))
                                .forEach(items::add)));
        items.sort(changeComparator(sort));
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/me/bookmarks")
    ResponseEntity<Map<String, Object>> bookmarks(HttpServletRequest request,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String type,
                                                  @RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to,
                                                  @RequestParam(required = false) String sort) {
        Actor actor = auth.current(request);
        validatePage(page, pageSize);
        if (status != null) {
            validateEnum(status, List.of("ACTIVE", "CANCELED"));
        }
        if (type != null) {
            validateReleaseType(type);
        }
        Instant fromInstant = parseOptionalInstant(from);
        Instant toInstant = parseOptionalInstant(to);
        validateRange(fromInstant, toInstant);
        validateSort(sort, "updatedAt_desc", "createdAt_desc", "releasedAt_desc");
        List<Map<String, Object>> items = store.bookmarks.values().stream()
                .filter(bookmark -> bookmark.userId.equals(actor.userId))
                .filter(bookmark -> status == null || bookmark.status.equals(status))
                .filter(bookmark -> {
                    ChangelogReleaseRecord release = store.releases.get(bookmark.releaseId);
                    return release != null && (type == null || release.type.equals(type)) && inReleaseRange(release, fromInstant, toInstant);
                })
                .sorted(bookmarkComparator(sort))
                .map(bookmark -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("bookmark", bookmark.view());
                    view.put("release", store.releases.get(bookmark.releaseId).currentUserSummaryView("ACTIVE".equals(bookmark.status)));
                    return view;
                })
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @PostMapping("/me/releases/{releaseId}/bookmark")
    ResponseEntity<Map<String, Object>> bookmark(HttpServletRequest request,
                                                 @PathVariable String releaseId,
                                                 @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        auth.failIfRequested(request);
        return idempotent(request, actor, "bookmark:" + releaseId, body, () -> {
            ChangelogReleaseRecord release = store.findRelease(releaseId).filter(ChangelogReleaseRecord::isPublicVisible)
                    .orElseThrow(() -> new ChangelogException(HttpStatus.NOT_FOUND, 49300, "changelog release not found"));
            ensureBookmarkWritable(request);
            ChangelogBookmarkRecord bookmark = store.bookmark(release, actor);
            store.audit("CHANGELOG_RELEASE_BOOKMARKED", release.releaseId, bookmark.bookmarkId, actor, request, body, "SUCCESS", null, null);
            return created(request, Map.of("bookmark", bookmark.view(), "release", release.currentUserSummaryView(true)));
        });
    }

    @PostMapping("/me/releases/{releaseId}/unbookmark")
    ResponseEntity<Map<String, Object>> unbookmark(HttpServletRequest request,
                                                   @PathVariable String releaseId,
                                                   @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        return idempotent(request, actor, "unbookmark:" + releaseId, body, () -> {
            ChangelogReleaseRecord release = store.findRelease(releaseId).orElseThrow(() -> new ChangelogException(HttpStatus.NOT_FOUND, 49300, "changelog release not found"));
            ensureBookmarkWritable(request);
            ChangelogBookmarkRecord bookmark = store.unbookmark(release, actor);
            store.audit("CHANGELOG_RELEASE_UNBOOKMARKED", release.releaseId, bookmark.bookmarkId, actor, request, body, "SUCCESS", null, null);
            return ok(request, Map.of("bookmark", bookmark.view(), "release", release.currentUserSummaryView(false)));
        });
    }

    @GetMapping("/admin/releases")
    ResponseEntity<Map<String, Object>> adminReleases(HttpServletRequest request,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int pageSize,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String type,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String visibility,
                                                      @RequestParam(required = false) String impactLevel,
                                                      @RequestParam(required = false) String createdBy,
                                                      @RequestParam(required = false) String minecraftVersion,
                                                      @RequestParam(required = false) String from,
                                                      @RequestParam(required = false) String to,
                                                      @RequestParam(required = false) String sort) {
        auth.requireStaff(request);
        validatePage(page, pageSize);
        validateSort(sort, "updatedAt_desc", "releasedAt_desc", "releasedAt_asc", "impactLevel_desc");
        if (type != null) {
            validateReleaseType(type);
        }
        if (status != null) {
            validateReleaseStatus(status);
        }
        if (visibility != null) {
            validateVisibility(visibility);
        }
        if (impactLevel != null) {
            validateEnum(impactLevel, List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        }
        Instant fromInstant = parseOptionalInstant(from);
        Instant toInstant = parseOptionalInstant(to);
        validateRange(fromInstant, toInstant);
        List<Map<String, Object>> items = store.releases.values().stream()
                .filter(release -> keyword == null || release.title.contains(keyword) || release.summary.contains(keyword) || release.slug.contains(keyword))
                .filter(release -> type == null || release.type.equals(type))
                .filter(release -> status == null || release.status.equals(status))
                .filter(release -> visibility == null || release.visibility.equals(visibility))
                .filter(release -> impactLevel == null || release.impactLevel.equals(impactLevel))
                .filter(release -> createdBy == null || release.createdBy.equals(createdBy))
                .filter(release -> minecraftVersion == null || Objects.equals(release.minecraftVersion, minecraftVersion))
                .filter(release -> inCreatedRange(release, fromInstant, toInstant))
                .sorted(releaseComparator(sort == null ? "updatedAt_desc" : sort))
                .map(ChangelogReleaseRecord::adminView)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/admin/releases/{releaseId}")
    ResponseEntity<Map<String, Object>> adminRelease(HttpServletRequest request, @PathVariable String releaseId) {
        auth.requireStaff(request);
        ChangelogReleaseRecord release = store.requireRelease(releaseId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("release", release.adminView());
        data.put("bookmarksTotal", release.bookmarkCount);
        data.put("dependencySummary", Map.of("resource", "TEST_STUB", "serverStatus", "TEST_STUB", "content", "TEST_STUB",
                "calendarSync", release.calendarSyncStatus, "notification", release.notificationFailure == null ? "SKIPPED" : "FAILED"));
        data.put("recentAudits", store.audits.stream().filter(audit -> audit.releaseId.equals(release.releaseId)).map(audit -> audit.view()).toList());
        return ok(request, data);
    }

    @PostMapping("/admin/releases")
    synchronized ResponseEntity<Map<String, Object>> createRelease(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, "create-release", body, () -> {
            validateReleaseBody(request, body, true, null);
            ensureAuditWritable(request);
            ChangelogReleaseRecord release = store.createRelease(body, actor);
            store.audit("CHANGELOG_RELEASE_CREATED", release.releaseId, release.releaseId, actor, request, body, "SUCCESS", null, release.status);
            return created(request, release.adminView());
        });
    }

    @PatchMapping("/admin/releases/{releaseId}")
    synchronized ResponseEntity<Map<String, Object>> updateRelease(HttpServletRequest request,
                                                      @PathVariable String releaseId,
                                                      @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, "update-release:" + releaseId, body, () -> {
            ChangelogReleaseRecord release = store.requireRelease(releaseId);
            if (!List.of("DRAFT", "NEEDS_CHANGES", "REJECTED", "APPROVED").contains(release.status)) {
                throw new ChangelogException(HttpStatus.CONFLICT, 49310, "changelog release state conflict");
            }
            if ("HELPER".equals(actor.role) && !release.createdBy.equals(actor.userId)) {
                throw new ChangelogException(HttpStatus.FORBIDDEN, 42001, "role denied");
            }
            validateReleaseBody(request, body, false, release);
            ensureAuditWritable(request);
            store.applyReleaseFields(release, body, actor);
            store.audit("CHANGELOG_RELEASE_UPDATED", release.releaseId, release.releaseId, actor, request, body, "SUCCESS", release.status, release.status);
            return ok(request, release.adminView());
        });
    }

    @PostMapping("/admin/releases/{releaseId}/submit")
    synchronized ResponseEntity<Map<String, Object>> submit(HttpServletRequest request,
                                               @PathVariable String releaseId,
                                               @RequestBody Map<String, Object> body) {
        return transitionStaff(request, releaseId, body, List.of("DRAFT", "NEEDS_CHANGES", "REJECTED"), "PENDING_REVIEW", "CHANGELOG_RELEASE_SUBMITTED");
    }

    @PatchMapping("/admin/releases/{releaseId}/approve")
    synchronized ResponseEntity<Map<String, Object>> approve(HttpServletRequest request,
                                                @PathVariable String releaseId,
                                                @RequestBody Map<String, Object> body) {
        return transitionStaff(request, releaseId, body, List.of("PENDING_REVIEW", "NEEDS_CHANGES"), "APPROVED", "CHANGELOG_RELEASE_APPROVED");
    }

    @PatchMapping("/admin/releases/{releaseId}/reject")
    synchronized ResponseEntity<Map<String, Object>> reject(HttpServletRequest request,
                                               @PathVariable String releaseId,
                                               @RequestBody Map<String, Object> body) {
        return transitionStaff(request, releaseId, body, List.of("PENDING_REVIEW"), "REJECTED", "CHANGELOG_RELEASE_REJECTED");
    }

    @PatchMapping("/admin/releases/{releaseId}/request-changes")
    synchronized ResponseEntity<Map<String, Object>> requestChanges(HttpServletRequest request,
                                                       @PathVariable String releaseId,
                                                       @RequestBody Map<String, Object> body) {
        return transitionStaff(request, releaseId, body, List.of("PENDING_REVIEW"), "NEEDS_CHANGES", "CHANGELOG_RELEASE_CHANGES_REQUESTED");
    }

    @PatchMapping("/admin/releases/{releaseId}/publish")
    synchronized ResponseEntity<Map<String, Object>> publish(HttpServletRequest request,
                                                @PathVariable String releaseId,
                                                @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, "publish:" + releaseId, body, () -> {
            requireReason(body);
            ChangelogReleaseRecord release = store.requireRelease(releaseId);
            if (!List.of("APPROVED", "OFFLINE").contains(release.status)) {
                throw new ChangelogException(HttpStatus.CONFLICT, 49310, "changelog release state conflict");
            }
            validateReleaseReady(release);
            ensureAuditWritable(request);
            String before = release.status;
            release.status = "PUBLISHED";
            release.publishedAt = now();
            release.releasedAt = text(body, "releasedAt", release.releasedAt == null ? release.publishedAt : release.releasedAt);
            release.effectiveAt = text(body, "effectiveAt", release.effectiveAt);
            release.updatedBy = actor.userId;
            release.updatedAt = now();
            applyNotificationFailure(request, release);
            store.audit("CHANGELOG_RELEASE_PUBLISHED", release.releaseId, release.releaseId, actor, request, body, "SUCCESS", before, release.status);
            return ok(request, release.adminView());
        });
    }

    @PatchMapping("/admin/releases/{releaseId}/offline")
    synchronized ResponseEntity<Map<String, Object>> offline(HttpServletRequest request,
                                                @PathVariable String releaseId,
                                                @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, "offline:" + releaseId, body, () -> {
            requireReason(body);
            ChangelogReleaseRecord release = store.requireRelease(releaseId);
            if (!"PUBLISHED".equals(release.status)) {
                throw new ChangelogException(HttpStatus.CONFLICT, 49310, "changelog release state conflict");
            }
            ensureAuditWritable(request);
            String before = release.status;
            release.status = "OFFLINE";
            release.offlineAt = now();
            release.updatedBy = actor.userId;
            release.updatedAt = now();
            applyNotificationFailure(request, release);
            store.audit("CHANGELOG_RELEASE_OFFLINED", release.releaseId, release.releaseId, actor, request, body, "SUCCESS", before, release.status);
            return ok(request, release.adminView());
        });
    }

    @PatchMapping("/admin/releases/{releaseId}/archive")
    synchronized ResponseEntity<Map<String, Object>> archive(HttpServletRequest request,
                                                @PathVariable String releaseId,
                                                @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, "archive:" + releaseId, body, () -> {
            requireReason(body);
            ChangelogReleaseRecord release = store.requireRelease(releaseId);
            if (!List.of("DRAFT", "REJECTED", "NEEDS_CHANGES", "OFFLINE").contains(release.status)) {
                throw new ChangelogException(HttpStatus.CONFLICT, 49310, "changelog release state conflict");
            }
            ensureAuditWritable(request);
            String before = release.status;
            release.status = "ARCHIVED";
            release.archivedAt = now();
            release.updatedBy = actor.userId;
            release.updatedAt = now();
            store.audit("CHANGELOG_RELEASE_ARCHIVED", release.releaseId, release.releaseId, actor, request, body, "SUCCESS", before, release.status);
            return ok(request, release.adminView());
        });
    }

    @PatchMapping("/admin/releases/{releaseId}/delete")
    synchronized ResponseEntity<Map<String, Object>> deleteRelease(HttpServletRequest request,
                                                      @PathVariable String releaseId,
                                                      @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, "delete:" + releaseId, body, () -> {
            requireReason(body);
            if (!"DELETE_CHANGELOG_RELEASE".equals(body.get("confirmText"))) {
                throw new ChangelogException(HttpStatus.BAD_REQUEST, 40001, "delete confirmation required");
            }
            ChangelogReleaseRecord release = store.requireRelease(releaseId);
            if ("PUBLISHED".equals(release.status) || "ARCHIVED".equals(release.status)) {
                throw new ChangelogException(HttpStatus.CONFLICT, 49310, "changelog release state conflict");
            }
            ensureAuditWritable(request);
            String before = release.status;
            release.status = "DELETED";
            release.deletedAt = now();
            release.updatedBy = actor.userId;
            release.updatedAt = now();
            store.audit("CHANGELOG_RELEASE_DELETED", release.releaseId, release.releaseId, actor, request, body, "SUCCESS", before, release.status);
            return ok(request, release.adminView());
        });
    }

    @PostMapping("/admin/releases/{releaseId}/calendar-sync")
    synchronized ResponseEntity<Map<String, Object>> calendarSync(HttpServletRequest request,
                                                     @PathVariable String releaseId,
                                                     @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, "calendar-sync:" + releaseId, body, () -> {
            requireReason(body);
            ChangelogReleaseRecord release = store.requireRelease(releaseId);
            if (properties.enabled()) {
                String mode = request.getHeader("X-Test-Calendar-Mode");
                if ("unavailable".equals(mode)) {
                    throw new ChangelogException(HttpStatus.BAD_GATEWAY, 49140, "calendar unavailable");
                }
                if ("timeout".equals(mode)) {
                    throw new ChangelogException(HttpStatus.GATEWAY_TIMEOUT, 49141, "calendar timeout");
                }
                if ("bad-schema".equals(mode)) {
                    throw new ChangelogException(HttpStatus.BAD_GATEWAY, 49142, "calendar incompatible");
                }
            }
            ensureAuditWritable(request);
            String mode = text(body, "mode", "DRY_RUN");
            String status = "UPSERT_SNAPSHOT".equals(mode) ? "SYNCED" : "SKIPPED";
            if ("SYNCED".equals(status)) {
                release.calendarSyncStatus = "SYNCED";
                release.calendarEventId = "cal-from-" + release.releaseId;
                release.calendarSyncedAt = now();
            }
            store.audit("CHANGELOG_CALENDAR_SYNCED", release.releaseId, release.releaseId, actor, request, body, "SUCCESS", release.status, release.status);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("syncStatus", status);
            data.put("releaseId", release.releaseId);
            data.put("calendarEvent", release.calendarRef());
            data.put("items", List.of(release.summaryView()));
            data.put("lastSyncedAt", release.calendarSyncedAt);
            return ok(request, data);
        });
    }

    @GetMapping("/admin/audit-logs")
    ResponseEntity<Map<String, Object>> auditLogs(HttpServletRequest request,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize,
                                                  @RequestParam(required = false) String actorUserId,
                                                  @RequestParam(required = false) String action,
                                                  @RequestParam(required = false) String targetType,
                                                  @RequestParam(required = false) String targetId,
                                                  @RequestParam(required = false) String releaseId,
                                                  @RequestParam(required = false) String result,
                                                  @RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to,
                                                  @RequestParam(required = false) String sort) {
        auth.requireAdmin(request);
        validatePage(page, pageSize);
        if (result != null) {
            validateEnum(result, List.of("SUCCESS", "FAILED"));
        }
        if (targetType != null) {
            validateEnum(targetType, List.of("CHANGELOG_RELEASE", "CHANGELOG_SERVICE"));
        }
        Instant fromInstant = parseOptionalInstant(from);
        Instant toInstant = parseOptionalInstant(to);
        validateRange(fromInstant, toInstant);
        validateSort(sort, "createdAt_desc", "createdAt_asc");
        List<Map<String, Object>> items = store.audits.stream()
                .filter(audit -> actorUserId == null || audit.actorUserId.equals(actorUserId))
                .filter(audit -> action == null || audit.action.equals(action))
                .filter(audit -> targetType == null || audit.targetType.equals(targetType))
                .filter(audit -> targetId == null || audit.targetId.equals(targetId))
                .filter(audit -> releaseId == null || audit.releaseId.equals(releaseId))
                .filter(audit -> result == null || audit.result.equals(result))
                .filter(audit -> inAuditRange(audit, fromInstant, toInstant))
                .sorted(auditComparator(sort))
                .map(ChangelogAuditRecord::view)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> opsSummary(HttpServletRequest request) {
        Actor actor = auth.requireStaff(request);
        store.audit("CHANGELOG_OPS_SUMMARY_READ", "changelog", "changelog", "CHANGELOG_SERVICE", actor, request, Map.of(), "SUCCESS", null, null);
        return ok(request, store.ops(properties.enabled(), actor));
    }

    private ResponseEntity<Map<String, Object>> transitionStaff(HttpServletRequest request,
                                                                String releaseId,
                                                                Map<String, Object> body,
                                                                List<String> from,
                                                                String to,
                                                                String action) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, action + ":" + releaseId, body, () -> {
            requireReason(body);
            ChangelogReleaseRecord release = store.requireRelease(releaseId);
            if (!from.contains(release.status)) {
                throw new ChangelogException(HttpStatus.CONFLICT, 49310, "changelog release state conflict");
            }
            ensureAuditWritable(request);
            String before = release.status;
            release.status = to;
            release.updatedBy = actor.userId;
            release.updatedAt = now();
            if ("PENDING_REVIEW".equals(to)) {
                release.submittedAt = now();
            }
            if (List.of("APPROVED", "REJECTED", "NEEDS_CHANGES").contains(to)) {
                release.reviewedBy = actor.userId;
                release.reviewedAt = now();
                release.reviewComment = text(body, "reviewComment", release.reviewComment);
            }
            store.audit(action, release.releaseId, release.releaseId, actor, request, body, "SUCCESS", before, to);
            return ok(request, release.adminView());
        });
    }

    private ResponseEntity<Map<String, Object>> idempotent(HttpServletRequest request,
                                                           Actor actor,
                                                           String scope,
                                                           Map<String, Object> body,
                                                           Supplier<ResponseEntity<Map<String, Object>>> supplier) {
        validateIdempotency(body);
        String key = body == null ? null : Objects.toString(body.get("idempotencyKey"), null);
        if (key == null || key.isBlank()) {
            return supplier.get();
        }
        String idempotencyKey = actor.userId + ":" + scope + ":" + key;
        String fingerprint = store.fingerprint(body);
        ChangelogIdempotencyRecord existing = store.idempotency.get(idempotencyKey);
        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new ChangelogException(HttpStatus.CONFLICT, 49312, "changelog idempotency conflict");
            }
            return ResponseEntity.status(existing.status()).body(envelope(request, existing.data()));
        }
        ResponseEntity<Map<String, Object>> response = supplier.get();
        store.idempotency.put(idempotencyKey, new ChangelogIdempotencyRecord(fingerprint, (HttpStatus) response.getStatusCode(), response.getBody().get("data")));
        return response;
    }

    private void validateReleaseBody(HttpServletRequest request, Map<String, Object> body, boolean creating, ChangelogReleaseRecord existing) {
        requireReason(body);
        validateDependencyHeaders(request);
        String idempotencyKey = Objects.toString(body.get("idempotencyKey"), "");
        if (idempotencyKey.length() < 8 || idempotencyKey.length() > 80) {
            throw new ChangelogException(HttpStatus.BAD_REQUEST, 40001, "invalid idempotencyKey");
        }
        String type = text(body, "type", existing == null ? null : existing.type);
        if (type != null) {
            validateReleaseType(type);
        }
        String visibility = text(body, "visibility", existing == null ? null : existing.visibility);
        if (visibility != null) {
            validateVisibility(visibility);
        }
        String impact = text(body, "impactLevel", existing == null ? null : existing.impactLevel);
        if (impact != null) {
            validateEnum(impact, List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        }
        Object groups = body.get("groups");
        if (creating || groups != null) {
            if (!(groups instanceof List<?> list) || list.isEmpty()) {
                throw new ChangelogException(HttpStatus.CONFLICT, 49315, "changelog groups required");
            }
            parseGroups(list);
        }
        String securitySummary = text(body, "securityPublicSummary", existing == null ? null : existing.securityPublicSummary);
        if ("SECURITY".equals(type) && (securitySummary == null || securitySummary.isBlank() || containsSensitive(securitySummary))) {
            throw new ChangelogException(HttpStatus.CONFLICT, 49314, "security public summary required");
        }
        String slug = text(body, "slug", null);
        if (creating && (slug == null || slug.length() < 2 || slug.length() > 100)) {
            throw new ChangelogException(HttpStatus.BAD_REQUEST, 40001, "invalid slug");
        }
        if (slug != null && store.slugExists(slug, existing == null ? null : existing.releaseId)) {
            throw new ChangelogException(HttpStatus.CONFLICT, 49311, "changelog slug conflict");
        }
        String version = text(body, "versionName", null);
        if (version != null && store.versionExists(version, existing == null ? null : existing.releaseId)) {
            throw new ChangelogException(HttpStatus.CONFLICT, 49311, "changelog version conflict");
        }
    }

    private void validateDependencyHeaders(HttpServletRequest request) {
        if (!properties.enabled()) {
            return;
        }
        failHeader(request, "X-Test-Resource-Mode", 49110, 49111, 49112, "resource");
        failHeader(request, "X-Test-Server-Status-Mode", 49120, 49121, 49122, "server status");
        failHeader(request, "X-Test-Content-Mode", 49130, 49131, 49132, "content");
    }

    private void failHeader(HttpServletRequest request, String header, int unavailable, int timeout, int badSchema, String name) {
        String value = request.getHeader(header);
        if ("unavailable".equals(value)) {
            throw new ChangelogException(HttpStatus.BAD_GATEWAY, unavailable, name + " unavailable");
        }
        if ("timeout".equals(value)) {
            throw new ChangelogException(HttpStatus.GATEWAY_TIMEOUT, timeout, name + " timeout");
        }
        if ("bad-schema".equals(value)) {
            throw new ChangelogException(HttpStatus.BAD_GATEWAY, badSchema, name + " incompatible");
        }
    }

    private void validateReleaseReady(ChangelogReleaseRecord release) {
        if (release.groups.isEmpty() || release.groups.stream().anyMatch(group -> group.items.isEmpty())) {
            throw new ChangelogException(HttpStatus.CONFLICT, 49315, "changelog groups required");
        }
        if ("SECURITY".equals(release.type) && (release.securityPublicSummary == null || release.securityPublicSummary.isBlank() || containsSensitive(release.securityPublicSummary))) {
            throw new ChangelogException(HttpStatus.CONFLICT, 49314, "security public summary required");
        }
    }

    private List<ChangelogGroupRecord> parseGroups(List<?> rawGroups) {
        List<ChangelogGroupRecord> groups = new ArrayList<>();
        int index = 0;
        for (Object rawGroup : rawGroups) {
            if (!(rawGroup instanceof Map<?, ?> groupMap)) {
                throw new ChangelogException(HttpStatus.BAD_REQUEST, 40001, "invalid group");
            }
            String type = Objects.toString(groupMap.get("type"), "");
            validateEnum(type, List.of("ADDED", "CHANGED", "DEPRECATED", "REMOVED", "FIXED", "SECURITY", "PERFORMANCE", "KNOWN_ISSUE"));
            Object rawItems = groupMap.get("items");
            if (!(rawItems instanceof List<?> itemList) || itemList.isEmpty()) {
                throw new ChangelogException(HttpStatus.CONFLICT, 49315, "changelog items required");
            }
            List<ChangelogItemRecord> items = new ArrayList<>();
            int itemIndex = 0;
            for (Object rawItem : itemList) {
                if (!(rawItem instanceof Map<?, ?> itemMap)) {
                    throw new ChangelogException(HttpStatus.BAD_REQUEST, 40001, "invalid item");
                }
                String severity = Objects.toString(itemMap.get("severity"), "INFO");
                validateEnum(severity, List.of("INFO", "MINOR", "MAJOR", "BREAKING", "SECURITY"));
                items.add(new ChangelogItemRecord(
                        "citem-" + index + "-" + itemIndex,
                        Objects.toString(itemMap.get("title"), "变更项"),
                        Objects.toString(itemMap.get("description"), "变更说明"),
                        severity,
                        Objects.toString(itemMap.get("component"), "server"),
                        !(itemMap.get("publicSafe") instanceof Boolean value) || value,
                        number(itemMap.get("sortOrder"), itemIndex)
                ));
                itemIndex++;
            }
            groups.add(new ChangelogGroupRecord("cgroup-" + index, type, Objects.toString(groupMap.get("title"), type),
                    Objects.toString(groupMap.get("description"), ""), items, number(groupMap.get("sortOrder"), index)));
            index++;
        }
        return groups;
    }

    private void applyNotificationFailure(HttpServletRequest request, ChangelogReleaseRecord release) {
        if (!properties.enabled()) {
            return;
        }
        String mode = request.getHeader("X-Test-Notification-Mode");
        if (mode == null) {
            return;
        }
        String code = switch (mode) {
            case "unavailable" -> "49150";
            case "timeout" -> "49151";
            case "bad-schema" -> "49152";
            default -> null;
        };
        if (code != null) {
            release.notificationFailure = linkedMap("status", "FAILED", "failureCode", code, "failureType", mode.toUpperCase().replace("-", "_"),
                    "failureReason", "notification dependency failed", "failedAt", now());
        }
    }

    private void ensureAuditWritable(HttpServletRequest request) {
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new ChangelogException(HttpStatus.INTERNAL_SERVER_ERROR, 54901, "changelog audit failed");
        }
    }

    private void ensureBookmarkWritable(HttpServletRequest request) {
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Bookmark"))) {
            throw new ChangelogException(HttpStatus.INTERNAL_SERVER_ERROR, 54903, "changelog bookmark failed");
        }
    }

    private boolean containsSensitive(String value) {
        String lower = value.toLowerCase();
        return lower.contains("token") || lower.contains("node") || lower.contains("exploit") || lower.contains("server") && lower.contains("properties") || lower.contains("powershell");
    }

    private static ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return ResponseEntity.ok(envelope(request, data));
    }

    private static ResponseEntity<Map<String, Object>> created(HttpServletRequest request, Object data) {
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(request, data));
    }

    private static Map<String, Object> envelope(HttpServletRequest request, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("message", "success");
        body.put("data", data);
        body.put("requestId", request.getAttribute("requestId"));
        return body;
    }

    private static Map<String, Object> page(List<Map<String, Object>> items, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items.subList(from, to));
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", items.size());
        return data;
    }

    private static void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ChangelogException(HttpStatus.BAD_REQUEST, 40002, "invalid page");
        }
    }

    private static void validateSort(String sort, String... allowed) {
        if (sort != null && !List.of(allowed).contains(sort)) {
            throw new ChangelogException(HttpStatus.BAD_REQUEST, 40003, "invalid sort");
        }
    }

    private static void validateEnum(String value, List<String> allowed) {
        if (!allowed.contains(value)) {
            throw new ChangelogException(HttpStatus.BAD_REQUEST, 40001, "invalid enum");
        }
    }

    private static void validateReleaseType(String value) {
        validateEnum(value, List.of("SERVER_VERSION", "PLUGIN_CHANGE", "RULE_CHANGE", "RESOURCE_PACK", "MAP_UPDATE", "MAINTENANCE", "SECURITY", "OTHER"));
    }

    private static void validateReleaseStatus(String value) {
        validateEnum(value, List.of("DRAFT", "PENDING_REVIEW", "APPROVED", "REJECTED", "NEEDS_CHANGES", "PUBLISHED", "OFFLINE", "ARCHIVED", "DELETED"));
    }

    private static void validateVisibility(String value) {
        validateEnum(value, List.of("PUBLIC", "MEMBER_ONLY", "STAFF_ONLY"));
    }

    private static void validateIdempotency(Map<String, Object> body) {
        if (body == null || !body.containsKey("idempotencyKey") || body.get("idempotencyKey") == null) {
            return;
        }
        String key = body.get("idempotencyKey").toString();
        if (key.length() < 8 || key.length() > 80) {
            throw new ChangelogException(HttpStatus.BAD_REQUEST, 40001, "invalid idempotencyKey");
        }
    }

    private static void requireReason(Map<String, Object> body) {
        Object reason = body == null ? null : body.get("reason");
        if (reason == null || reason.toString().isBlank() || reason.toString().length() > 200) {
            throw new ChangelogException(HttpStatus.BAD_REQUEST, 40001, "reason required");
        }
    }

    private static Instant parseOptionalInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception exception) {
            throw new ChangelogException(HttpStatus.BAD_REQUEST, 40001, "invalid time");
        }
    }

    private static void validateRange(Instant from, Instant to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new ChangelogException(HttpStatus.CONFLICT, 49316, "changelog time range conflict");
        }
    }

    private static boolean inReleaseRange(ChangelogReleaseRecord release, Instant from, Instant to) {
        if (from == null && to == null) {
            return true;
        }
        if (release.releasedAt == null) {
            return false;
        }
        Instant released = Instant.parse(release.releasedAt);
        return (from == null || !released.isBefore(from)) && (to == null || released.isBefore(to));
    }

    private static boolean inCreatedRange(ChangelogReleaseRecord release, Instant from, Instant to) {
        Instant created = Instant.parse(release.createdAt);
        return (from == null || !created.isBefore(from)) && (to == null || created.isBefore(to));
    }

    private static boolean inAuditRange(ChangelogAuditRecord audit, Instant from, Instant to) {
        Instant created = Instant.parse(audit.createdAt);
        return (from == null || !created.isBefore(from)) && (to == null || created.isBefore(to));
    }

    private static Comparator<ChangelogReleaseRecord> releaseComparator(String sort) {
        String actual = sort == null ? "releasedAt_desc" : sort;
        Comparator<ChangelogReleaseRecord> comparator = switch (actual) {
            case "releasedAt_asc" -> Comparator.comparing(release -> instantOrMin(release.releasedAt));
            case "effectiveAt_desc" -> Comparator.comparing((ChangelogReleaseRecord release) -> instantOrMin(release.effectiveAt)).reversed();
            case "updatedAt_desc" -> Comparator.comparing((ChangelogReleaseRecord release) -> Instant.parse(release.updatedAt)).reversed();
            case "impactLevel_desc" -> Comparator.comparingInt((ChangelogReleaseRecord release) -> impactRank(release.impactLevel)).reversed();
            default -> Comparator.comparing((ChangelogReleaseRecord release) -> instantOrMin(release.releasedAt)).reversed();
        };
        return comparator.thenComparing(release -> release.releaseId);
    }

    private static Comparator<ChangelogBookmarkRecord> bookmarkComparator(String sort) {
        if ("createdAt_desc".equals(sort)) {
            return Comparator.comparing((ChangelogBookmarkRecord bookmark) -> Instant.parse(bookmark.createdAt)).reversed();
        }
        return Comparator.comparing((ChangelogBookmarkRecord bookmark) -> Instant.parse(bookmark.updatedAt)).reversed();
    }

    private static Comparator<Map<String, Object>> changeComparator(String sort) {
        Comparator<Map<String, Object>> byReleasedAt = Comparator.comparing(item -> instantOrMin(Objects.toString(((Map<?, ?>) item.get("release")).get("releasedAt"), null)));
        if ("releasedAt_asc".equals(sort)) {
            return byReleasedAt;
        }
        if ("severity_desc".equals(sort)) {
            return Comparator.comparingInt((Map<String, Object> item) -> severityRank(Objects.toString(item.get("severity"), "INFO"))).reversed()
                    .thenComparing(byReleasedAt.reversed());
        }
        return byReleasedAt.reversed();
    }

    private static Comparator<ChangelogAuditRecord> auditComparator(String sort) {
        Comparator<ChangelogAuditRecord> comparator = Comparator.comparing(audit -> Instant.parse(audit.createdAt));
        return "createdAt_asc".equals(sort) ? comparator : comparator.reversed();
    }

    private static Instant instantOrMin(String value) {
        return value == null ? Instant.EPOCH : Instant.parse(value);
    }

    private static int impactRank(String impact) {
        return switch (impact) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            default -> 1;
        };
    }

    private static int severityRank(String severity) {
        return switch (severity) {
            case "SECURITY" -> 5;
            case "BREAKING" -> 4;
            case "MAJOR" -> 3;
            case "MINOR" -> 2;
            default -> 1;
        };
    }

    private static void addText(Set<String> values, Object value) {
        if (value != null && !value.toString().isBlank()) {
            values.add(value.toString());
        }
    }

    private static int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static String text(Map<String, Object> body, String key, String fallback) {
        Object value = body == null ? null : body.get(key);
        return value == null ? fallback : value.toString();
    }

    private static String now() {
        return Instant.now().toString();
    }

    private static Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(values[i].toString(), values[i + 1]);
        }
        return map;
    }
}

@Service
class ChangelogStore {
    final Map<String, ChangelogReleaseRecord> releases = new ConcurrentHashMap<>();
    final Map<String, ChangelogBookmarkRecord> bookmarks = new ConcurrentHashMap<>();
    final Map<String, ChangelogIdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    final List<ChangelogAuditRecord> audits = new ArrayList<>();
    private final AtomicInteger releaseSeq = new AtomicInteger(1);
    private final AtomicInteger bookmarkSeq = new AtomicInteger(1);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    void seed() {
        ChangelogReleaseRecord release = new ChangelogReleaseRecord("chg-seed-public", "seed-public", "v1.20.4-seed", "北冥服务器种子更新", "公开种子更新日志", "SERVER_VERSION");
        release.status = "PUBLISHED";
        release.publishedAt = "2026-05-25T10:00:00Z";
        release.releasedAt = "2026-05-25T10:00:00Z";
        release.effectiveAt = "2026-05-25T11:00:00Z";
        release.groups = List.of(new ChangelogGroupRecord("seed-group", "ADDED", "新增内容", "种子分组",
                List.of(new ChangelogItemRecord("seed-item", "种子变更项", "公开安全的种子说明", "INFO", "server", true, 10)), 10));
        releases.put(release.releaseId, release);
        audit("CHANGELOG_SEEDED", release.releaseId, release.releaseId, "system", "SUCCESS");
    }

    Optional<ChangelogReleaseRecord> findRelease(String idOrSlug) {
        ChangelogReleaseRecord byId = releases.get(idOrSlug);
        if (byId != null) {
            return Optional.of(byId);
        }
        return releases.values().stream().filter(release -> release.slug.equals(idOrSlug)).findFirst();
    }

    ChangelogReleaseRecord requireRelease(String idOrSlug) {
        return findRelease(idOrSlug).orElseThrow(() -> new ChangelogException(HttpStatus.NOT_FOUND, 49300, "changelog release not found"));
    }

    boolean slugExists(String slug, String exceptId) {
        return releases.values().stream().anyMatch(release -> release.slug.equals(slug) && !release.releaseId.equals(exceptId));
    }

    boolean versionExists(String version, String exceptId) {
        return releases.values().stream().anyMatch(release -> release.versionName.equals(version) && !release.releaseId.equals(exceptId));
    }

    synchronized ChangelogReleaseRecord createRelease(Map<String, Object> body, Actor actor) {
        String id = "chg-" + releaseSeq.incrementAndGet();
        ChangelogReleaseRecord release = new ChangelogReleaseRecord(id, body.get("slug").toString(), body.get("versionName").toString(),
                body.get("title").toString(), body.get("summary").toString(), body.get("type").toString());
        applyReleaseFields(release, body, actor);
        release.status = "DRAFT";
        release.createdBy = actor.userId;
        release.updatedBy = actor.userId;
        releases.put(id, release);
        return release;
    }

    synchronized void applyReleaseFields(ChangelogReleaseRecord release, Map<String, Object> body, Actor actor) {
        release.slug = text(body, "slug", release.slug);
        release.versionName = text(body, "versionName", release.versionName);
        release.title = text(body, "title", release.title);
        release.summary = text(body, "summary", release.summary);
        release.body = text(body, "body", release.body);
        release.type = text(body, "type", release.type);
        release.visibility = text(body, "visibility", release.visibility);
        release.impactLevel = text(body, "impactLevel", release.impactLevel);
        release.releasedAt = text(body, "releasedAt", release.releasedAt);
        release.effectiveAt = text(body, "effectiveAt", release.effectiveAt);
        release.minecraftVersion = text(body, "minecraftVersion", release.minecraftVersion);
        if (body.containsKey("pluginVersions")) {
            release.pluginVersions = objectList(body.get("pluginVersions"));
        }
        if (body.containsKey("resourcePackVersions")) {
            release.resourcePackVersions = objectList(body.get("resourcePackVersions"));
        }
        release.mapVersion = text(body, "mapVersion", release.mapVersion);
        release.compatibilityNotes = text(body, "compatibilityNotes", release.compatibilityNotes);
        release.knownIssues = text(body, "knownIssues", release.knownIssues);
        release.rollbackNotes = text(body, "rollbackNotes", release.rollbackNotes);
        release.securityPublicSummary = text(body, "securityPublicSummary", release.securityPublicSummary);
        release.internalNote = text(body, "internalNote", release.internalNote);
        if (body.get("groups") instanceof List<?> groups) {
            release.groups = parseGroups(groups);
        }
        if (body.containsKey("relatedResourceIds")) {
            release.relatedResources = relatedResources(body.get("relatedResourceIds"));
        }
        if (body.containsKey("relatedServerInstanceIds")) {
            release.relatedServerInstances = relatedServers(body.get("relatedServerInstanceIds"));
        }
        if (body.containsKey("relatedContentId")) {
            release.relatedContent = relatedContent(body.get("relatedContentId"));
        }
        release.updatedBy = actor.userId;
        release.updatedAt = Instant.now().toString();
    }

    private List<ChangelogGroupRecord> parseGroups(List<?> rawGroups) {
        List<ChangelogGroupRecord> groups = new ArrayList<>();
        int index = 0;
        for (Object rawGroup : rawGroups) {
            Map<?, ?> group = (Map<?, ?>) rawGroup;
            List<ChangelogItemRecord> items = new ArrayList<>();
            int itemIndex = 0;
            for (Object rawItem : (List<?>) group.get("items")) {
                Map<?, ?> item = (Map<?, ?>) rawItem;
                items.add(new ChangelogItemRecord("citem-" + index + "-" + itemIndex, Objects.toString(item.get("title"), "变更项"),
                        Objects.toString(item.get("description"), "变更说明"), Objects.toString(item.get("severity"), "INFO"),
                        Objects.toString(item.get("component"), "server"), !(item.get("publicSafe") instanceof Boolean value) || value,
                        number(item.get("sortOrder"), itemIndex)));
                itemIndex++;
            }
            groups.add(new ChangelogGroupRecord("cgroup-" + index, Objects.toString(group.get("type"), "ADDED"),
                    Objects.toString(group.get("title"), "分组"), Objects.toString(group.get("description"), ""), items,
                    number(group.get("sortOrder"), index)));
            index++;
        }
        return groups;
    }

    private List<Map<String, Object>> relatedResources(Object value) {
        if (!(value instanceof List<?> ids)) {
            return List.of();
        }
        return ids.stream().map(id -> linkedMap("resourceId", id.toString(), "slug", id.toString(), "versionName", "2026.06",
                "visibility", "PUBLIC", "downloadAvailable", true, "resourceSnapshotStale", false, "failure", null)).toList();
    }

    private List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(raw -> {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    ((Map<?, ?>) raw).forEach((key, nested) -> copy.put(key.toString(), nested));
                    return copy;
                })
                .toList();
    }

    private List<Map<String, Object>> relatedServers(Object value) {
        if (!(value instanceof List<?> ids)) {
            return List.of();
        }
        return ids.stream().map(id -> linkedMap("instanceId", id.toString(), "name", "北冥生存服", "minecraftVersion", "1.20.4",
                "serverStatusSnapshotStale", false)).toList();
    }

    private Map<String, Object> relatedContent(Object value) {
        if (value == null) {
            return null;
        }
        return linkedMap("contentId", value.toString(), "slug", "release-note", "title", "更新说明页", "url", "/content/release-note", "contentSnapshotStale", false);
    }

    synchronized ChangelogBookmarkRecord bookmark(ChangelogReleaseRecord release, Actor actor) {
        Optional<ChangelogBookmarkRecord> existing = bookmarks.values().stream()
                .filter(bookmark -> bookmark.releaseId.equals(release.releaseId))
                .filter(bookmark -> bookmark.userId.equals(actor.userId))
                .findFirst();
        if (existing.isPresent()) {
            ChangelogBookmarkRecord bookmark = existing.get();
            if ("CANCELED".equals(bookmark.status)) {
                bookmark.status = "ACTIVE";
                bookmark.canceledAt = null;
                bookmark.updatedAt = Instant.now().toString();
                release.bookmarkCount++;
            }
            return bookmark;
        }
        String id = "cbmk-" + bookmarkSeq.incrementAndGet();
        ChangelogBookmarkRecord bookmark = new ChangelogBookmarkRecord(id, release.releaseId, actor);
        bookmarks.put(id, bookmark);
        release.bookmarkCount++;
        return bookmark;
    }

    synchronized ChangelogBookmarkRecord unbookmark(ChangelogReleaseRecord release, Actor actor) {
        ChangelogBookmarkRecord bookmark = bookmarks.values().stream()
                .filter(existing -> existing.releaseId.equals(release.releaseId))
                .filter(existing -> existing.userId.equals(actor.userId))
                .findFirst()
                .orElseGet(() -> {
                    String id = "cbmk-" + bookmarkSeq.incrementAndGet();
                    ChangelogBookmarkRecord created = new ChangelogBookmarkRecord(id, release.releaseId, actor);
                    created.status = "CANCELED";
                    created.canceledAt = Instant.now().toString();
                    bookmarks.put(id, created);
                    return created;
                });
        if ("ACTIVE".equals(bookmark.status)) {
            bookmark.status = "CANCELED";
            bookmark.canceledAt = Instant.now().toString();
            release.bookmarkCount = Math.max(0, release.bookmarkCount - 1);
        }
        bookmark.updatedAt = Instant.now().toString();
        return bookmark;
    }

    void audit(String action, String releaseId, String targetId, String actorUserId, String result) {
        audits.add(new ChangelogAuditRecord("chaud-" + (audits.size() + 1), action, releaseId, targetId, "CHANGELOG_RELEASE",
                actorUserId, "SYSTEM", result, null, null, Map.of(), null, null, null));
    }

    void audit(String action, String releaseId, String targetId, String actorUserId, String result, String stateFrom, String stateTo) {
        audits.add(new ChangelogAuditRecord("chaud-" + (audits.size() + 1), action, releaseId, targetId, "CHANGELOG_RELEASE",
                actorUserId, "SYSTEM", result, null, null, Map.of(), stateFrom, stateTo, null));
    }

    void audit(String action, String releaseId, String targetId, Actor actor, HttpServletRequest request, Map<String, Object> body,
               String result, String stateFrom, String stateTo) {
        audit(action, releaseId, targetId, "CHANGELOG_RELEASE", actor, request, body, result, stateFrom, stateTo);
    }

    void audit(String action, String releaseId, String targetId, String targetType, Actor actor, HttpServletRequest request, Map<String, Object> body,
               String result, String stateFrom, String stateTo) {
        String requestId = Objects.toString(request.getAttribute("requestId"), null);
        String reason = body == null ? null : Objects.toString(body.get("reason"), null);
        audits.add(new ChangelogAuditRecord("chaud-" + (audits.size() + 1), action, releaseId, targetId, targetType,
                actor.userId, actor.role, result, requestId, reason, paramsSummary(body), stateFrom, stateTo, null));
    }

    private Map<String, Object> paramsSummary(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        body.forEach((key, value) -> {
            if (!"internalNote".equals(key) && !"reviewComment".equals(key) && !"securityPublicSummary".equals(key)) {
                summary.put(key, value instanceof List<?> list ? "list:" + list.size() : value);
            }
        });
        return summary;
    }

    Map<String, Object> ops(boolean testControlsEnabled, Actor actor) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "changelog");
        data.put("port", 8132);
        data.put("legacyPort", 8115);
        data.put("storageMode", "IN_MEMORY");
        data.put("authMode", actor.authMode);
        data.put("actorUserId", actor.userId);
        data.put("resourceMode", "TEST_STUB");
        data.put("serverStatusMode", "TEST_STUB");
        data.put("contentMode", "TEST_STUB");
        data.put("calendarSyncMode", "SKIPPED");
        data.put("notificationMode", "SKIPPED");
        data.put("testControlsEnabled", testControlsEnabled);
        data.put("releasesTotal", releases.size());
        data.put("publishedReleasesTotal", releases.values().stream().filter(ChangelogReleaseRecord::isPublicVisible).count());
        data.put("bookmarksTotal", bookmarks.size());
        data.put("auditsTotal", audits.size());
        data.put("idempotencyRecordsTotal", idempotency.size());
        data.put("lastPublishedAt", releases.values().stream().map(release -> release.publishedAt).filter(Objects::nonNull).max(String::compareTo).orElse(null));
        data.put("lastAuditAt", audits.isEmpty() ? null : audits.get(audits.size() - 1).createdAt);
        data.put("productionGaps", List.of("P1_IN_MEMORY_STORAGE", "P1_AUTH_STUB", "P1_RESOURCE_STUB", "P1_SERVER_STATUS_STUB",
                "P1_CONTENT_STUB", "CALENDAR_WRITE_NOT_CONNECTED", "NOTIFICATION_DELIVERY_NOT_CONNECTED", "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"));
        return data;
    }

    String fingerprint(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(canonical(body));
        } catch (Exception exception) {
            throw new ChangelogException(HttpStatus.BAD_REQUEST, 40001, "invalid json body");
        }
    }

    private Object canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, nested) -> sorted.put(key.toString(), canonical(nested)));
            return sorted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::canonical).toList();
        }
        return value;
    }

    private String text(Map<String, Object> body, String key, String fallback) {
        Object value = body == null ? null : body.get(key);
        return value == null ? fallback : value.toString();
    }

    private int number(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private Map<String, Object> linkedMap(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(values[i].toString(), values[i + 1]);
        }
        return map;
    }
}

record ChangelogIdempotencyRecord(String fingerprint, HttpStatus status, Object data) {
}

class ChangelogReleaseRecord {
    final String releaseId;
    String slug;
    String versionName;
    String title;
    String summary;
    String body = "更新说明";
    String type;
    String status = "DRAFT";
    String visibility = "PUBLIC";
    String impactLevel = "MEDIUM";
    String releasedAt;
    String effectiveAt;
    String minecraftVersion = "1.20.4";
    List<Map<String, Object>> pluginVersions = List.of();
    List<Map<String, Object>> resourcePackVersions = List.of();
    String mapVersion;
    List<ChangelogGroupRecord> groups = new ArrayList<>();
    String compatibilityNotes;
    String knownIssues;
    String rollbackNotes;
    String securityPublicSummary;
    String internalNote;
    List<Map<String, Object>> relatedResources = List.of();
    List<Map<String, Object>> relatedServerInstances = List.of();
    Map<String, Object> relatedContent;
    String calendarSyncStatus = "SKIPPED";
    String calendarEventId;
    String calendarSyncedAt;
    Map<String, Object> notificationFailure;
    int bookmarkCount;
    String createdBy = "system";
    String updatedBy = "system";
    String reviewedBy;
    String reviewComment;
    String submittedAt;
    String reviewedAt;
    String publishedAt;
    String offlineAt;
    String archivedAt;
    String deletedAt;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    ChangelogReleaseRecord(String releaseId, String slug, String versionName, String title, String summary, String type) {
        this.releaseId = releaseId;
        this.slug = slug;
        this.versionName = versionName;
        this.title = title;
        this.summary = summary;
        this.type = type;
    }

    boolean isPublicVisible() {
        return "PUBLISHED".equals(status) && "PUBLIC".equals(visibility);
    }

    Map<String, Object> summaryView() {
        Map<String, Object> view = baseView(false, true);
        view.remove("body");
        view.remove("groups");
        return view;
    }

    Map<String, Object> currentUserSummaryView(boolean bookmarkedByCurrentUser) {
        Map<String, Object> view = summaryView();
        view.put("bookmarkedByCurrentUser", bookmarkedByCurrentUser);
        return view;
    }

    Map<String, Object> publicView() {
        Map<String, Object> view = baseView(false, true);
        view.remove("reviewComment");
        view.remove("submittedAt");
        view.remove("reviewedAt");
        view.remove("offlineAt");
        view.remove("archivedAt");
        return view;
    }

    Map<String, Object> adminView() {
        Map<String, Object> view = baseView(true, false);
        view.put("createdBy", createdBy);
        view.put("updatedBy", updatedBy);
        view.put("reviewedBy", reviewedBy);
        view.put("internalNote", internalNote);
        view.put("deletedAt", deletedAt);
        return view;
    }

    Map<String, Object> baseView(boolean includeFailure, boolean redactSecurity) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("releaseId", releaseId);
        view.put("slug", slug);
        view.put("versionName", versionName);
        view.put("title", title);
        view.put("summary", summary);
        view.put("body", body);
        view.put("type", type);
        view.put("status", status);
        view.put("visibility", visibility);
        view.put("impactLevel", impactLevel);
        view.put("releasedAt", releasedAt);
        view.put("effectiveAt", effectiveAt);
        view.put("minecraftVersion", minecraftVersion);
        view.put("pluginVersions", pluginVersions);
        view.put("resourcePackVersions", resourcePackVersions);
        view.put("mapVersion", mapVersion);
        view.put("groups", groups.stream().map(group -> group.view(this, redactSecurity)).toList());
        view.put("compatibilityNotes", compatibilityNotes);
        view.put("knownIssues", knownIssues);
        view.put("rollbackNotes", redactSecurity ? null : rollbackNotes);
        view.put("securityPublicSummary", securityPublicSummary);
        view.put("relatedResources", relatedResources);
        view.put("relatedServerInstances", relatedServerInstances);
        view.put("relatedCalendarEvent", calendarRef());
        view.put("relatedContent", relatedContent);
        view.put("notificationSummary", notificationSummary(includeFailure));
        view.put("bookmarkCount", bookmarkCount);
        view.put("reviewComment", reviewComment);
        view.put("submittedAt", submittedAt);
        view.put("reviewedAt", reviewedAt);
        view.put("publishedAt", publishedAt);
        view.put("offlineAt", offlineAt);
        view.put("archivedAt", archivedAt);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        return view;
    }

    Map<String, Object> calendarRef() {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("eventId", calendarEventId);
        ref.put("title", calendarEventId == null ? null : title);
        ref.put("startAt", effectiveAt);
        ref.put("syncStatus", calendarSyncStatus);
        ref.put("lastSyncedAt", calendarSyncedAt);
        ref.put("failure", null);
        return ref;
    }

    Map<String, Object> notificationSummary(boolean includeFailure) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("status", notificationFailure == null ? "SKIPPED" : "FAILED");
        summary.put("targetAudience", "PUBLIC");
        summary.put("lastAttemptAt", notificationFailure == null ? null : notificationFailure.get("failedAt"));
        summary.put("failure", includeFailure ? notificationFailure : null);
        return summary;
    }

    boolean matchesTag(String tag) {
        return groups.stream().flatMap(group -> group.items.stream())
                .anyMatch(item -> item.publicSafe && Objects.equals(item.component, tag))
                || pluginVersions.stream().anyMatch(plugin -> Objects.equals(plugin.get("name"), tag))
                || resourcePackVersions.stream().anyMatch(pack -> Objects.equals(pack.get("name"), tag));
    }
}

class ChangelogGroupRecord {
    final String groupId;
    final String type;
    final String title;
    final String description;
    final List<ChangelogItemRecord> items;
    final int sortOrder;

    ChangelogGroupRecord(String groupId, String type, String title, String description, List<ChangelogItemRecord> items, int sortOrder) {
        this.groupId = groupId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.items = items;
        this.sortOrder = sortOrder;
    }

    Map<String, Object> view(ChangelogReleaseRecord release, boolean redactSecurity) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("groupId", groupId);
        view.put("type", type);
        view.put("title", title);
        view.put("description", description);
        view.put("items", items.stream().map(item -> item.view(release, redactSecurity)).toList());
        view.put("sortOrder", sortOrder);
        return view;
    }
}

class ChangelogItemRecord {
    final String itemId;
    final String title;
    final String description;
    final String severity;
    final String component;
    final boolean publicSafe;
    final int sortOrder;

    ChangelogItemRecord(String itemId, String title, String description, String severity, String component, boolean publicSafe, int sortOrder) {
        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.component = component;
        this.publicSafe = publicSafe;
        this.sortOrder = sortOrder;
    }

    Map<String, Object> view(ChangelogReleaseRecord release, boolean redactSecurity) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("itemId", itemId);
        view.put("title", title);
        view.put("description", redactSecurity && "SECURITY".equals(release.type) && !publicSafe ? "已脱敏安全修复说明" : description);
        view.put("severity", severity);
        view.put("component", component);
        view.put("publicSafe", publicSafe);
        view.put("sortOrder", sortOrder);
        return view;
    }

    Map<String, Object> publicSearchView(ChangelogReleaseRecord release, ChangelogGroupRecord group) {
        Map<String, Object> view = view(release, true);
        view.put("release", release.summaryView());
        view.put("groupType", group.type);
        return view;
    }
}

class ChangelogBookmarkRecord {
    final String bookmarkId;
    final String releaseId;
    final String userId;
    final String displayName;
    String status = "ACTIVE";
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;
    String canceledAt;

    ChangelogBookmarkRecord(String bookmarkId, String releaseId, Actor actor) {
        this.bookmarkId = bookmarkId;
        this.releaseId = releaseId;
        this.userId = actor.userId;
        this.displayName = actor.displayName;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("bookmarkId", bookmarkId);
        view.put("releaseId", releaseId);
        view.put("userId", userId);
        view.put("displayNameSnapshot", displayName);
        view.put("status", status);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        view.put("canceledAt", canceledAt);
        return view;
    }
}

class ChangelogAuditRecord {
    final String auditId;
    final String action;
    final String releaseId;
    final String targetId;
    final String targetType;
    final String actorUserId;
    final String actorRole;
    final String result;
    final String requestId;
    final String reason;
    final Map<String, Object> paramsSummary;
    final String stateFrom;
    final String stateTo;
    final String failureReason;
    final String createdAt = Instant.now().toString();

    ChangelogAuditRecord(String auditId, String action, String releaseId, String targetId, String targetType, String actorUserId,
                         String actorRole, String result, String requestId, String reason, Map<String, Object> paramsSummary,
                         String stateFrom, String stateTo, String failureReason) {
        this.auditId = auditId;
        this.action = action;
        this.releaseId = releaseId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.result = result;
        this.requestId = requestId;
        this.reason = reason;
        this.paramsSummary = paramsSummary == null ? Map.of() : paramsSummary;
        this.stateFrom = stateFrom;
        this.stateTo = stateTo;
        this.failureReason = failureReason;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", auditId);
        view.put("requestId", requestId);
        view.put("action", action);
        view.put("targetType", targetType);
        view.put("releaseId", releaseId);
        view.put("targetId", targetId);
        view.put("actorUserId", actorUserId);
        view.put("actorRole", actorRole);
        view.put("result", result);
        view.put("riskLevel", "LOW");
        view.put("reason", reason);
        view.put("paramsSummary", paramsSummary);
        view.put("beforeState", stateFrom);
        view.put("afterState", stateTo);
        view.put("stateFrom", stateFrom);
        view.put("stateTo", stateTo);
        view.put("failureReason", failureReason);
        view.put("createdAt", createdAt);
        return view;
    }
}

@Service
class ChangelogAuth {
    private final ChangelogProperties properties;

    ChangelogAuth(ChangelogProperties properties) {
        this.properties = properties;
    }

    Actor current(HttpServletRequest request) {
        failIfRequested(request);
        try {
            var trusted = TrustedGatewayAuth.from(request);
            if (trusted.isPresent()) {
                TrustedGatewayAuth.Actor actor = trusted.get();
                return new Actor(actor.userId(), actor.primaryRole(), actor.userId(), actor.authMode());
            }
        } catch (TrustedGatewayAuth.MalformedContextException exception) {
            throw new ChangelogException(HttpStatus.BAD_GATEWAY, 49102, "auth incompatible");
        }
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            throw new ChangelogException(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        }
        if (!header.startsWith("Bearer ")) {
            throw new ChangelogException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        }
        String token = header.substring("Bearer ".length());
        return switch (token) {
            case "owner-token" -> local("owner-user", "OWNER", "Owner");
            case "admin-token" -> local("admin-user", "ADMIN", "Admin");
            case "helper-token" -> local("helper-user", "HELPER", "Helper");
            case "user-token" -> local("plain-user", "USER", "PlainUser");
            case "member-user-1-token" -> local("member-user-1", "USER", "MemberOne");
            case "member-user-2-token" -> local("member-user-2", "USER", "MemberTwo");
            default -> throw new ChangelogException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        };
    }

    void failIfRequested(HttpServletRequest request) {
        if (properties.enabled() && "unavailable".equals(request.getHeader("X-Test-Auth-Mode"))) {
            throw new ChangelogException(HttpStatus.BAD_GATEWAY, 49100, "auth unavailable");
        }
    }

    Actor requireStaff(HttpServletRequest request) {
        Actor actor = current(request);
        if (!List.of("HELPER", "ADMIN", "OWNER").contains(actor.role)) {
            throw new ChangelogException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        return actor;
    }

    Actor requireAdmin(HttpServletRequest request) {
        Actor actor = current(request);
        if (!List.of("ADMIN", "OWNER").contains(actor.role)) {
            throw new ChangelogException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        return actor;
    }

    private Actor local(String userId, String role, String displayName) {
        return new Actor(userId, role, displayName, "TEST_STUB");
    }
}

class Actor {
    final String userId;
    final String role;
    final String displayName;
    final String authMode;

    Actor(String userId, String role, String displayName, String authMode) {
        this.userId = userId;
        this.role = role;
        this.displayName = displayName;
        this.authMode = authMode;
    }
}

@Component
class ChangelogProperties {
    private final boolean enabled;

    ChangelogProperties(@Value("${changelog.test-controls.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }
}

@Component
class ChangelogRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .filter(value -> !value.isBlank())
                .orElse("req_" + UUID.randomUUID());
        request.setAttribute("requestId", requestId);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}

@RestControllerAdvice(basePackageClasses = ChangelogController.class)
class ChangelogExceptionHandler {
    @ExceptionHandler(ChangelogException.class)
    ResponseEntity<Map<String, Object>> api(ChangelogException exception, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", exception.code);
        body.put("message", exception.getMessage());
        body.put("data", null);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(exception.status).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> fallback(Exception exception, HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 54900);
        body.put("message", "changelog internal error");
        body.put("data", null);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

class ChangelogException extends RuntimeException {
    final HttpStatus status;
    final int code;

    ChangelogException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
