package cn.beiming.calendar;

import cn.beiming.engagement.TrustedGatewayAuth;
import cn.beiming.engagement.persistence.CalendarPersistence;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Configuration
class CalendarEvidenceConfiguration {
    @Bean
    @ConditionalOnMissingBean
    CalendarFlowEvidenceRecorder calendarFlowEvidenceRecorder(CalendarPersistence persistence) {
        return new PersistentCalendarFlowEvidenceRecorder(persistence);
    }
}

@RestController
@RequestMapping("/api/v1/calendar")
class CalendarController {
    private final CalendarStore store;
    private final CalendarAuth auth;
    private final CalendarProperties properties;
    private final CalendarFlowEvidenceRecorder evidenceRecorder;
    private final CalendarPersistence persistence;

    CalendarController(CalendarStore store, CalendarAuth auth, CalendarProperties properties,
                       CalendarFlowEvidenceRecorder evidenceRecorder, CalendarPersistence persistence) {
        this.store = store;
        this.auth = auth;
        this.properties = properties;
        this.evidenceRecorder = evidenceRecorder;
        this.persistence = persistence;
    }

    @GetMapping("/events")
    ResponseEntity<Map<String, Object>> publicEvents(HttpServletRequest request,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int pageSize,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String type,
                                                     @RequestParam(required = false) String sourceType,
                                                     @RequestParam(required = false) String from,
                                                     @RequestParam(required = false) String to,
                                                     @RequestParam(required = false) String sort) {
        validatePage(page, pageSize);
        validateSort(sort, "startAt_asc", "startAt_desc", "priority_desc", "publishedAt_desc", "updatedAt_desc");
        Instant fromInstant = parseOptionalInstant(from);
        Instant toInstant = parseOptionalInstant(to);
        if (fromInstant != null && toInstant != null && !toInstant.isAfter(fromInstant)) {
            throw new ApiException(HttpStatus.CONFLICT, 49911, "calendar time range conflict");
        }
        List<Map<String, Object>> items = store.events.values().stream()
                .filter(CalendarEventRecord::isPublicVisible)
                .filter(event -> keyword == null || event.title.contains(keyword) || event.summary.contains(keyword))
                .filter(event -> type == null || event.type.equals(type))
                .filter(event -> sourceType == null || event.sourceType.equals(sourceType))
                .filter(event -> overlaps(event, fromInstant, toInstant))
                .sorted(eventComparator(sort))
                .map(CalendarEventRecord::publicView)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/events/{eventId}")
    ResponseEntity<Map<String, Object>> publicEvent(HttpServletRequest request, @PathVariable String eventId) {
        CalendarEventRecord event = store.findEvent(eventId).filter(CalendarEventRecord::isPublicVisible)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 49900, "calendar event not found"));
        return ok(request, event.publicView());
    }

    @GetMapping("/month")
    ResponseEntity<Map<String, Object>> month(HttpServletRequest request,
                                              @RequestParam String month,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) String sourceType) {
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(month);
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid month");
        }
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        Instant rangeStart = yearMonth.atDay(1).atStartOfDay(zone).toInstant();
        Instant rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();
        List<Map<String, Object>> items = store.events.values().stream()
                .filter(CalendarEventRecord::isPublicVisible)
                .filter(event -> type == null || event.type.equals(type))
                .filter(event -> sourceType == null || event.sourceType.equals(sourceType))
                .filter(event -> overlaps(event, rangeStart, rangeEnd))
                .sorted(eventComparator("startAt_asc"))
                .map(CalendarEventRecord::summaryView)
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", month);
        data.put("timezone", "Asia/Shanghai");
        data.put("rangeStart", rangeStart.toString());
        data.put("rangeEnd", rangeEnd.toString());
        data.put("items", items);
        data.put("degraded", false);
        return ok(request, data);
    }

    @GetMapping("/upcoming")
    ResponseEntity<Map<String, Object>> upcoming(HttpServletRequest request,
                                                 @RequestParam(defaultValue = "10") int limit,
                                                 @RequestParam(defaultValue = "30") int days,
                                                 @RequestParam(required = false) String from,
                                                 @RequestParam(required = false) String type,
                                                 @RequestParam(required = false) String sourceType) {
        if (limit < 1 || limit > 50 || days < 1 || days > 180) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40002, "invalid upcoming parameters");
        }
        Instant start = Optional.ofNullable(parseOptionalInstant(from)).orElse(currentInstant(request));
        Instant end = start.plusSeconds(days * 86400L);
        List<Map<String, Object>> items = store.events.values().stream()
                .filter(CalendarEventRecord::isPublicVisible)
                .filter(event -> type == null || event.type.equals(type))
                .filter(event -> sourceType == null || event.sourceType.equals(sourceType))
                .filter(event -> !Instant.parse(event.startAt).isBefore(start))
                .filter(event -> Instant.parse(event.startAt).isBefore(end))
                .sorted(Comparator.comparing((CalendarEventRecord event) -> Instant.parse(event.startAt))
                        .thenComparing((CalendarEventRecord event) -> -event.priority)
                        .thenComparing(event -> event.eventId))
                .limit(limit)
                .map(CalendarEventRecord::summaryView)
                .toList();
        return ok(request, Map.of("items", items));
    }

    @GetMapping("/me/watchlist")
    ResponseEntity<Map<String, Object>> watchlist(HttpServletRequest request,
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
            validateEventType(type);
        }
        validateSort(sort, "updatedAt_desc", "createdAt_desc", "startAt_asc");
        Instant fromInstant = parseOptionalInstant(from);
        Instant toInstant = parseOptionalInstant(to);
        validateRange(fromInstant, toInstant);
        List<Map<String, Object>> items = store.watches.values().stream()
                .filter(watch -> watch.userId.equals(actor.userId))
                .filter(watch -> status == null || watch.status.equals(status))
                .filter(watch -> {
                    CalendarEventRecord event = store.events.get(watch.eventId);
                    return event != null && (type == null || event.type.equals(type)) && overlaps(event, fromInstant, toInstant);
                })
                .sorted(watchComparator(sort))
                .map(watch -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("watch", watch.view());
                    view.put("event", store.events.get(watch.eventId).summaryView());
                    return view;
                })
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @PostMapping("/me/events/{eventId}/watch")
    ResponseEntity<Map<String, Object>> watch(HttpServletRequest request,
                                              @PathVariable String eventId,
                                              @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        auth.failIfRequested(request);
        return idempotent(request, actor, body, () -> {
            CalendarEventRecord event = store.findEvent(eventId).filter(CalendarEventRecord::isPublicVisible)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 49900, "calendar event not found"));
            validateReminder(body);
            ensureWatchWritable(request);
            CalendarWatchRecord watch = store.watch(event, actor, body);
            store.audit("CALENDAR_EVENT_WATCHED", event.eventId, watch.watchId, actor.userId, "SUCCESS");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("watch", watch.view());
            payload.put("event", event.publicView());
            evidenceRecorder.recordWatchWrite(request, "CALENDAR_EVENT_WATCHED", payload, HttpStatus.CREATED.value());
            return created(request, payload);
        });
    }

    @PostMapping("/me/events/{eventId}/unwatch")
    ResponseEntity<Map<String, Object>> unwatch(HttpServletRequest request,
                                                @PathVariable String eventId,
                                                @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        return idempotent(request, actor, body, () -> {
            CalendarEventRecord event = store.findEvent(eventId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 49900, "calendar event not found"));
            ensureWatchWritable(request);
            CalendarWatchRecord watch = store.unwatch(event, actor, body);
            store.audit("CALENDAR_EVENT_UNWATCHED", event.eventId, watch.watchId, actor.userId, "SUCCESS");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("watch", watch.view());
            payload.put("event", event.summaryView());
            evidenceRecorder.recordWatchWrite(request, "CALENDAR_EVENT_UNWATCHED", payload, HttpStatus.OK.value());
            return ok(request, payload);
        });
    }

    @GetMapping("/admin/events")
    ResponseEntity<Map<String, Object>> adminEvents(HttpServletRequest request,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int pageSize,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String type,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String visibility,
                                                    @RequestParam(required = false) String sourceType,
                                                    @RequestParam(required = false) String createdBy,
                                                    @RequestParam(required = false) String from,
                                                    @RequestParam(required = false) String to,
                                                    @RequestParam(required = false) String sort) {
        auth.requireStaff(request);
        validatePage(page, pageSize);
        validateSort(sort, "updatedAt_desc", "startAt_asc", "startAt_desc", "publishedAt_desc");
        if (type != null) {
            validateEventType(type);
        }
        if (status != null) {
            validateEventStatus(status);
        }
        if (visibility != null) {
            validateVisibility(visibility);
        }
        if (sourceType != null) {
            validateSourceType(sourceType);
        }
        Instant fromInstant = parseOptionalInstant(from);
        Instant toInstant = parseOptionalInstant(to);
        validateRange(fromInstant, toInstant);
        List<Map<String, Object>> items = store.events.values().stream()
                .filter(event -> keyword == null || event.title.contains(keyword) || event.summary.contains(keyword))
                .filter(event -> type == null || event.type.equals(type))
                .filter(event -> status == null || event.status.equals(status))
                .filter(event -> visibility == null || event.visibility.equals(visibility))
                .filter(event -> sourceType == null || event.sourceType.equals(sourceType))
                .filter(event -> createdBy == null || event.createdBy.equals(createdBy))
                .filter(event -> overlaps(event, fromInstant, toInstant))
                .sorted(eventComparator(sort == null ? "updatedAt_desc" : sort))
                .map(CalendarEventRecord::adminView)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/admin/events/{eventId}")
    ResponseEntity<Map<String, Object>> adminEvent(HttpServletRequest request, @PathVariable String eventId) {
        auth.requireStaff(request);
        CalendarEventRecord event = store.requireEvent(eventId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("event", event.adminView());
        data.put("watchesTotal", event.watchCount);
        data.put("dependencySummary", Map.of("activity", "TEST_STUB", "notification", "SKIPPED", "changelog", "NOT_CONNECTED"));
        data.put("recentAudits", store.audits.stream().filter(audit -> audit.eventId.equals(event.eventId)).map(audit -> audit.view(event.sourceType)).toList());
        return ok(request, data);
    }

    @PostMapping("/admin/events")
    ResponseEntity<Map<String, Object>> createEvent(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            validateEventBody(body, true, null);
            ensureAuditWritable(request);
            CalendarEventRecord event = store.createEvent(body, actor);
            store.audit("CALENDAR_EVENT_CREATED", event.eventId, event.eventId, actor.userId, "SUCCESS");
            Map<String, Object> payload = event.adminView();
            evidenceRecorder.recordEventWrite(request, "CALENDAR_EVENT_CREATED", payload, HttpStatus.CREATED.value());
            return created(request, payload);
        });
    }

    @PatchMapping("/admin/events/{eventId}")
    ResponseEntity<Map<String, Object>> updateEvent(HttpServletRequest request,
                                                    @PathVariable String eventId,
                                                    @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            CalendarEventRecord event = store.requireEvent(eventId);
            if (!List.of("DRAFT", "NEEDS_CHANGES", "REJECTED", "APPROVED").contains(event.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49910, "calendar event state conflict");
            }
            if ("HELPER".equals(actor.role) && !event.createdBy.equals(actor.userId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
            }
            validateEventBody(body, false, event);
            ensureAuditWritable(request);
            store.applyEventFields(event, body, actor);
            store.audit("CALENDAR_EVENT_UPDATED", event.eventId, event.eventId, actor.userId, "SUCCESS");
            Map<String, Object> payload = event.adminView();
            evidenceRecorder.recordEventWrite(request, "CALENDAR_EVENT_UPDATED", payload, HttpStatus.OK.value());
            return ok(request, payload);
        });
    }

    @PostMapping("/admin/events/{eventId}/submit")
    ResponseEntity<Map<String, Object>> submit(HttpServletRequest request,
                                               @PathVariable String eventId,
                                               @RequestBody Map<String, Object> body) {
        return transitionStaff(request, eventId, body, List.of("DRAFT", "NEEDS_CHANGES", "REJECTED"), "PENDING_REVIEW", "CALENDAR_EVENT_SUBMITTED");
    }

    @PatchMapping("/admin/events/{eventId}/approve")
    ResponseEntity<Map<String, Object>> approve(HttpServletRequest request,
                                                @PathVariable String eventId,
                                                @RequestBody Map<String, Object> body) {
        return transitionStaff(request, eventId, body, List.of("PENDING_REVIEW", "NEEDS_CHANGES"), "APPROVED", "CALENDAR_EVENT_APPROVED");
    }

    @PatchMapping("/admin/events/{eventId}/reject")
    ResponseEntity<Map<String, Object>> reject(HttpServletRequest request,
                                               @PathVariable String eventId,
                                               @RequestBody Map<String, Object> body) {
        return transitionStaff(request, eventId, body, List.of("PENDING_REVIEW"), "REJECTED", "CALENDAR_EVENT_REJECTED");
    }

    @PatchMapping("/admin/events/{eventId}/publish")
    ResponseEntity<Map<String, Object>> publish(HttpServletRequest request,
                                                @PathVariable String eventId,
                                                @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            requireReason(body);
            CalendarEventRecord event = store.requireEvent(eventId);
            if (!List.of("APPROVED", "OFFLINE").contains(event.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49910, "calendar event state conflict");
            }
            ensureAuditWritable(request);
            event.status = "PUBLISHED";
            event.publishedAt = now();
            event.updatedAt = now();
            event.updatedBy = actor.userId;
            event.reminderFailure = notificationFailure(request);
            store.audit("CALENDAR_EVENT_PUBLISHED", event.eventId, event.eventId, actor.userId, "SUCCESS");
            Map<String, Object> payload = event.adminView();
            evidenceRecorder.recordEventWrite(request, "CALENDAR_EVENT_PUBLISHED", payload, HttpStatus.OK.value());
            return ok(request, payload);
        });
    }

    @PatchMapping("/admin/events/{eventId}/offline")
    ResponseEntity<Map<String, Object>> offline(HttpServletRequest request,
                                                @PathVariable String eventId,
                                                @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            requireReason(body);
            if (!hasText(body.get("publicReason"))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "publicReason is required");
            }
            CalendarEventRecord event = store.requireEvent(eventId);
            if (!"PUBLISHED".equals(event.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49910, "calendar event state conflict");
            }
            ensureAuditWritable(request);
            event.status = "OFFLINE";
            event.offlineAt = now();
            event.updatedAt = now();
            event.updatedBy = actor.userId;
            event.reminderFailure = notificationFailure(request);
            store.audit("CALENDAR_EVENT_OFFLINED", event.eventId, event.eventId, actor.userId, "SUCCESS");
            Map<String, Object> payload = event.adminView();
            evidenceRecorder.recordEventWrite(request, "CALENDAR_EVENT_OFFLINED", payload, HttpStatus.OK.value());
            return ok(request, payload);
        });
    }

    @PatchMapping("/admin/events/{eventId}/archive")
    ResponseEntity<Map<String, Object>> archive(HttpServletRequest request,
                                                @PathVariable String eventId,
                                                @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            requireReason(body);
            CalendarEventRecord event = store.requireEvent(eventId);
            if (!List.of("DRAFT", "REJECTED", "NEEDS_CHANGES", "OFFLINE").contains(event.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49910, "calendar event state conflict");
            }
            ensureAuditWritable(request);
            event.status = "ARCHIVED";
            event.archivedAt = now();
            event.updatedAt = now();
            event.updatedBy = actor.userId;
            store.audit("CALENDAR_EVENT_ARCHIVED", event.eventId, event.eventId, actor.userId, "SUCCESS");
            Map<String, Object> payload = event.adminView();
            evidenceRecorder.recordEventWrite(request, "CALENDAR_EVENT_ARCHIVED", payload, HttpStatus.OK.value());
            return ok(request, payload);
        });
    }

    @PatchMapping("/admin/events/{eventId}/delete")
    ResponseEntity<Map<String, Object>> delete(HttpServletRequest request,
                                               @PathVariable String eventId,
                                               @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            requireReason(body);
            if (!"DELETE_CALENDAR_EVENT".equals(Objects.toString(body.get("confirmText"), ""))) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "confirmText is required");
            }
            CalendarEventRecord event = store.requireEvent(eventId);
            if ("PUBLISHED".equals(event.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49910, "calendar event state conflict");
            }
            ensureAuditWritable(request);
            event.status = "DELETED";
            event.deletedAt = now();
            event.updatedAt = now();
            event.updatedBy = actor.userId;
            store.audit("CALENDAR_EVENT_DELETED", event.eventId, event.eventId, actor.userId, "SUCCESS");
            Map<String, Object> payload = event.adminView();
            evidenceRecorder.recordEventWrite(request, "CALENDAR_EVENT_DELETED", payload, HttpStatus.OK.value());
            return ok(request, payload);
        });
    }

    @PostMapping("/admin/sync/activity")
    ResponseEntity<Map<String, Object>> syncActivity(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            requireReason(body);
            Instant from = parseRequiredInstant(body.get("from"));
            Instant to = parseRequiredInstant(body.get("to"));
            request.setAttribute("calendar.syncFrom", from.toString());
            request.setAttribute("calendar.syncTo", to.toString());
            if (!to.isAfter(from)) {
                throw new ApiException(HttpStatus.CONFLICT, 49911, "calendar time range conflict");
            }
            String mode = Objects.toString(body.getOrDefault("mode", "UPSERT_SNAPSHOT"));
            request.setAttribute("calendar.syncMode", mode);
            if (!List.of("UPSERT_SNAPSHOT", "DRY_RUN").contains(mode)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid sync mode");
            }
            if (properties.enabled() && "unavailable".equals(request.getHeader("X-Test-Activity-Mode"))) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, 49810, "activity unavailable");
            }
            ensureAuditWritable(request);
            List<Map<String, Object>> summaries = store.activitySummaries();
            int created = 0;
            int updated = 0;
            if ("UPSERT_SNAPSHOT".equals(mode)) {
                for (Map<String, Object> summary : summaries) {
                    if (store.upsertActivity(summary, actor)) {
                        created++;
                    } else {
                        updated++;
                    }
                }
                store.lastActivitySyncAt = now();
            }
            store.audit("CALENDAR_ACTIVITY_SYNCED", "calendar-sync", "activity", actor.userId, "SUCCESS");
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("syncStatus", "DRY_RUN".equals(mode) ? "SKIPPED" : "SYNCED");
            data.put("createdTotal", created);
            data.put("updatedTotal", updated);
            data.put("skippedTotal", "DRY_RUN".equals(mode) ? summaries.size() : 0);
            data.put("failedTotal", 0);
            data.put("items", summaries);
            data.put("activityMode", "TEST_STUB");
            data.put("lastSyncedAt", store.lastActivitySyncAt);
            data.put("syncedEvents", store.events.values().stream()
                    .filter(event -> "ACTIVITY".equals(event.sourceType))
                    .map(CalendarEventRecord::adminView)
                    .toList());
            evidenceRecorder.recordActivitySyncWrite(request, "CALENDAR_ACTIVITY_SYNCED", data, HttpStatus.OK.value());
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
                                                  @RequestParam(required = false) String eventId,
                                                  @RequestParam(required = false) String sourceType,
                                                  @RequestParam(required = false) String result,
                                                  @RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to,
                                                  @RequestParam(required = false) String sort) {
        auth.requireAdmin(request);
        validatePage(page, pageSize);
        validateSort(sort, "createdAt_desc", "createdAt_asc");
        if (result != null) {
            validateEnum(result, List.of("SUCCESS", "FAILED"));
        }
        if (sourceType != null) {
            validateSourceType(sourceType);
        }
        Instant fromInstant = parseOptionalInstant(from);
        Instant toInstant = parseOptionalInstant(to);
        validateRange(fromInstant, toInstant);
        List<Map<String, Object>> items = store.audits.stream()
                .filter(audit -> actorUserId == null || audit.actorUserId.equals(actorUserId))
                .filter(audit -> action == null || audit.action.equals(action))
                .filter(audit -> targetType == null || audit.targetType().equals(targetType))
                .filter(audit -> targetId == null || audit.targetId.equals(targetId))
                .filter(audit -> eventId == null || audit.eventId.equals(eventId))
                .filter(audit -> sourceType == null || store.sourceTypeFor(audit.eventId).equals(sourceType))
                .filter(audit -> result == null || audit.result.equals(result))
                .filter(audit -> inAuditRange(audit, fromInstant, toInstant))
                .sorted(auditComparator(sort))
                .map(audit -> audit.view(store.sourceTypeFor(audit.eventId)))
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> ops(HttpServletRequest request) {
        Actor actor = auth.requireStaff(request);
        store.audit("CALENDAR_OPS_READ", "calendar", "ops", actor.userId, "SUCCESS");
        return ok(request, store.ops(properties.enabled(), actor, persistence.counts()));
    }

    private ResponseEntity<Map<String, Object>> transitionStaff(HttpServletRequest request,
                                                                String eventId,
                                                                Map<String, Object> body,
                                                                List<String> allowed,
                                                                String target,
                                                                String action) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            requireReason(body);
            CalendarEventRecord event = store.requireEvent(eventId);
            if (!allowed.contains(event.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49910, "calendar event state conflict");
            }
            ensureAuditWritable(request);
            event.status = target;
            event.updatedAt = now();
            event.updatedBy = actor.userId;
            if ("PENDING_REVIEW".equals(target)) {
                event.submittedAt = now();
            }
            if (List.of("APPROVED", "REJECTED").contains(target)) {
                event.reviewedAt = now();
                event.reviewedBy = actor.userId;
            }
            store.audit(action, event.eventId, event.eventId, actor.userId, "SUCCESS");
            Map<String, Object> payload = event.adminView();
            evidenceRecorder.recordEventWrite(request, action, payload, HttpStatus.OK.value());
            return ok(request, payload);
        });
    }

    private ResponseEntity<Map<String, Object>> idempotent(HttpServletRequest request,
                                                           Actor actor,
                                                           Map<String, Object> body,
                                                           Supplier<ResponseEntity<Map<String, Object>>> operation) {
        String key = Objects.toString(body == null ? null : body.get("idempotencyKey"), "");
        request.setAttribute("calendar.actorUserId", actor.userId);
        request.setAttribute("calendar.actorRole", actor.role);
        request.setAttribute("calendar.idempotencyKey", key.isBlank() ? null : key);
        request.setAttribute("calendar.fingerprint", store.fingerprint(body));
        request.setAttribute("calendar.reason", Objects.toString(body == null ? null : body.get("reason"), null));
        request.setAttribute("calendar.scope", scopeFor(request));
        if (!key.isBlank()) {
            if (key.length() < 8 || key.length() > 80) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid idempotencyKey");
            }
            String storageKey = actor.userId + ":" + request.getMethod() + ":" + request.getRequestURI() + ":" + key;
            String scope = Objects.toString(request.getAttribute("calendar.scope"), null);
            String fingerprint = request.getAttribute("calendar.fingerprint").toString();
            Map<String, Object> replay;
            try {
                replay = persistence.replay(actor.userId, scope, key, fingerprint);
            } catch (IllegalStateException exception) {
                throw new ApiException(HttpStatus.CONFLICT, 49914, "calendar idempotency conflict");
            }
            if (replay != null) {
                CalendarIdempotencyRecord existing = store.idempotency.get(storageKey);
                HttpStatus status = existing == null ? HttpStatus.OK : existing.status();
                return envelope(request, status, replay);
            }
            CalendarIdempotencyRecord existing = store.idempotency.get(storageKey);
            if (existing != null) {
                if (!existing.fingerprint().equals(fingerprint)) {
                    throw new ApiException(HttpStatus.CONFLICT, 49914, "calendar idempotency conflict");
                }
                return envelope(request, existing.status(), existing.data());
            }
            ResponseEntity<Map<String, Object>> response = operation.get();
            store.idempotency.put(storageKey, new CalendarIdempotencyRecord(fingerprint, (HttpStatus) response.getStatusCode(), response.getBody().get("data")));
            return response;
        }
        return operation.get();
    }

    private String scopeFor(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        if ("POST".equals(method) && "/api/v1/calendar/admin/events".equals(path)) return "calendar.event.create";
        if ("PATCH".equals(method) && path.matches("/api/v1/calendar/admin/events/[^/]+")) return "calendar.event.update";
        if ("POST".equals(method) && path.matches("/api/v1/calendar/admin/events/[^/]+/submit")) return "calendar.event.transition";
        if ("PATCH".equals(method) && path.matches("/api/v1/calendar/admin/events/[^/]+/(approve|reject|publish|offline|archive|delete)")) return "calendar.event.transition";
        if ("POST".equals(method) && path.matches("/api/v1/calendar/me/events/[^/]+/watch")) return "calendar.watch.create";
        if ("POST".equals(method) && path.matches("/api/v1/calendar/me/events/[^/]+/unwatch")) return "calendar.watch.cancel";
        if ("POST".equals(method) && "/api/v1/calendar/admin/sync/activity".equals(path)) return "calendar.activity-sync.run";
        return "calendar.write";
    }

    private void validateEventBody(Map<String, Object> body, boolean create, CalendarEventRecord existing) {
        requireReason(body);
        if (create || body.containsKey("title")) {
            validateText(body.get("title"), 2, 100, "title");
        }
        if (create || body.containsKey("summary")) {
            validateText(body.get("summary"), 1, 300, "summary");
        }
        if (body.containsKey("description") && Objects.toString(body.get("description"), "").length() > 5000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "description too long");
        }
        if (create || body.containsKey("type")) {
            validateEnum(body.get("type"), List.of("ACTIVITY", "MAINTENANCE", "ENGINEERING_MILESTONE", "VOTE_DEADLINE", "VERSION_RELEASE", "SERVER_SCHEDULE"));
        }
        if (create || body.containsKey("visibility")) {
            validateEnum(body.get("visibility"), List.of("PUBLIC", "MEMBER_ONLY", "STAFF_ONLY"));
        }
        String sourceType = body.containsKey("sourceType")
                ? Objects.toString(body.get("sourceType"))
                : (existing == null ? "MANUAL" : existing.sourceType);
        validateSourceType(sourceType);
        if ((create || body.containsKey("sourceType")) && !List.of("MANUAL", "CHANGELOG").contains(sourceType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "sourceType is not creatable");
        }
        String eventType = body.containsKey("type")
                ? Objects.toString(body.get("type"))
                : (existing == null ? "" : existing.type);
        if ("CHANGELOG".equals(sourceType) && !"VERSION_RELEASE".equals(eventType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "changelog source requires version release");
        }
        if (!"MANUAL".equals(sourceType) && !hasText(body.get("sourceId"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "sourceId is required");
        }
        if (create || body.containsKey("startAt") || body.containsKey("endAt")) {
            Instant start = body.containsKey("startAt") ? parseRequiredInstant(body.get("startAt")) : Instant.parse(existing.startAt);
            Instant end = body.containsKey("endAt") ? parseRequiredInstant(body.get("endAt")) : Instant.parse(existing.endAt);
            if (!end.isAfter(start)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "endAt must be after startAt");
            }
        }
        if (body.containsKey("timezone") && !"Asia/Shanghai".equals(Objects.toString(body.get("timezone")))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid timezone");
        }
        Object labels = body.get("labels");
        if (labels instanceof List<?> list && list.size() > 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "too many labels");
        }
        String relatedUrl = Objects.toString(body.get("relatedUrl"), "");
        if (!relatedUrl.isBlank() && !(relatedUrl.startsWith("/") || relatedUrl.startsWith("http://") || relatedUrl.startsWith("https://"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid relatedUrl");
        }
        validateReminder(body);
    }

    private void validateReminder(Map<String, Object> body) {
        Object offsets = body.get("reminderOffsets");
        if (offsets instanceof List<?> list) {
            if (list.size() > 5) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "too many reminder offsets");
            }
            for (Object offset : list) {
                int value = number(offset, -1);
                if (value < 0 || value > 10080) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid reminder offset");
                }
            }
        }
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40002, "invalid pagination");
        }
    }

    private void validateSort(String sort, String... allowed) {
        if (sort != null && !List.of(allowed).contains(sort)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40003, "invalid sort");
        }
    }

    private void validateEventType(String type) {
        validateEnum(type, List.of("ACTIVITY", "MAINTENANCE", "ENGINEERING_MILESTONE", "VOTE_DEADLINE", "VERSION_RELEASE", "SERVER_SCHEDULE"));
    }

    private void validateEventStatus(String status) {
        validateEnum(status, List.of("DRAFT", "PENDING_REVIEW", "APPROVED", "REJECTED", "NEEDS_CHANGES", "PUBLISHED", "OFFLINE", "ARCHIVED", "DELETED"));
    }

    private void validateVisibility(String visibility) {
        validateEnum(visibility, List.of("PUBLIC", "MEMBER_ONLY", "STAFF_ONLY"));
    }

    private void validateSourceType(String sourceType) {
        validateEnum(sourceType, List.of("MANUAL", "ACTIVITY", "CHANGELOG", "COMMUNITY_POLL", "OPS_PLACEHOLDER"));
    }

    private void validateText(Object value, int min, int max, String field) {
        String text = Objects.toString(value, "");
        if (text.length() < min || text.length() > max) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, field + " invalid");
        }
    }

    private void validateEnum(Object value, List<String> allowed) {
        if (!allowed.contains(Objects.toString(value, ""))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid enum");
        }
    }

    private void requireReason(Map<String, Object> body) {
        if (!hasText(body == null ? null : body.get("reason"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "reason is required");
        }
    }

    private void ensureAuditWritable(HttpServletRequest request) {
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, 54801, "calendar audit write failed");
        }
    }

    private void ensureWatchWritable(HttpServletRequest request) {
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Watch"))) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, 54803, "calendar watch write failed");
        }
    }

    private Map<String, Object> notificationFailure(HttpServletRequest request) {
        if (properties.enabled() && "unavailable".equals(request.getHeader("X-Test-Notification-Mode"))) {
            return Map.of("status", "FAILED", "failureCode", "49820", "failureType", "UNAVAILABLE",
                    "failureReason", "notification unavailable", "failedAt", now());
        }
        return null;
    }

    private boolean overlaps(CalendarEventRecord event, Instant from, Instant to) {
        if (from == null || to == null) {
            return true;
        }
        Instant start = Instant.parse(event.startAt);
        Instant end = Instant.parse(event.endAt);
        return end.isAfter(from) && start.isBefore(to);
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new ApiException(HttpStatus.CONFLICT, 49911, "calendar time range conflict");
        }
    }

    private boolean inAuditRange(CalendarAuditRecord audit, Instant from, Instant to) {
        Instant createdAt = Instant.parse(audit.createdAt);
        return (from == null || !createdAt.isBefore(from)) && (to == null || createdAt.isBefore(to));
    }

    private Comparator<CalendarEventRecord> eventComparator(String sort) {
        String value = sort == null ? "startAt_asc" : sort;
        return switch (value) {
            case "startAt_desc" -> Comparator.comparing((CalendarEventRecord event) -> Instant.parse(event.startAt)).reversed();
            case "priority_desc" -> Comparator.comparing((CalendarEventRecord event) -> event.priority).reversed().thenComparing(event -> event.eventId);
            case "publishedAt_desc" -> Comparator.comparing((CalendarEventRecord event) -> nullableInstant(event.publishedAt)).reversed();
            case "updatedAt_desc" -> Comparator.comparing((CalendarEventRecord event) -> Instant.parse(event.updatedAt)).reversed();
            default -> Comparator.comparing((CalendarEventRecord event) -> Instant.parse(event.startAt)).thenComparing(event -> event.eventId);
        };
    }

    private Comparator<CalendarWatchRecord> watchComparator(String sort) {
        String value = sort == null ? "updatedAt_desc" : sort;
        return switch (value) {
            case "createdAt_desc" -> Comparator.comparing((CalendarWatchRecord watch) -> Instant.parse(watch.createdAt)).reversed();
            case "startAt_asc" -> Comparator.comparing((CalendarWatchRecord watch) -> Instant.parse(store.events.get(watch.eventId).startAt)).thenComparing(watch -> watch.watchId);
            default -> Comparator.comparing((CalendarWatchRecord watch) -> Instant.parse(watch.updatedAt)).reversed();
        };
    }

    private Comparator<CalendarAuditRecord> auditComparator(String sort) {
        Comparator<CalendarAuditRecord> comparator = Comparator.comparing(audit -> Instant.parse(audit.createdAt));
        return "createdAt_asc".equals(sort) ? comparator : comparator.reversed();
    }

    private Instant nullableInstant(String value) {
        return value == null ? Instant.EPOCH : Instant.parse(value);
    }

    private Instant currentInstant(HttpServletRequest request) {
        if (properties.enabled() && request.getHeader("X-Test-Now") != null) {
            return Instant.parse(request.getHeader("X-Test-Now"));
        }
        return Instant.now();
    }

    private Instant parseOptionalInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseRequiredInstant(value);
    }

    private Instant parseRequiredInstant(Object value) {
        try {
            return Instant.parse(Objects.toString(value));
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid instant");
        }
    }

    private ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return envelope(request, HttpStatus.OK, data);
    }

    private ResponseEntity<Map<String, Object>> created(HttpServletRequest request, Object data) {
        return envelope(request, HttpStatus.CREATED, data);
    }

    private ResponseEntity<Map<String, Object>> envelope(HttpServletRequest request, HttpStatus status, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 0);
        body.put("message", "success");
        body.put("data", data);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> page(List<Map<String, Object>> items, int page, int pageSize) {
        int fromIndex = Math.min((page - 1) * pageSize, items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items.subList(fromIndex, toIndex));
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", items.size());
        return data;
    }

    private boolean hasText(Object value) {
        return value != null && !Objects.toString(value).isBlank();
    }

    private int number(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String now() {
        return Instant.now().toString();
    }
}

@Service
class CalendarStore {
    final Map<String, CalendarEventRecord> events = new ConcurrentHashMap<>();
    final Map<String, CalendarWatchRecord> watches = new ConcurrentHashMap<>();
    final Map<String, CalendarIdempotencyRecord> idempotency = new ConcurrentHashMap<>();
    final List<CalendarAuditRecord> audits = new ArrayList<>();
    final ObjectMapper objectMapper = new ObjectMapper();
    final AtomicInteger eventSeq = new AtomicInteger();
    final AtomicInteger watchSeq = new AtomicInteger();
    String lastActivitySyncAt;

    @PostConstruct
    void seed() {
        CalendarEventRecord seed = new CalendarEventRecord("cal-seed-public", "MANUAL", null, "北冥公开日程", "公开日程摘要", "MAINTENANCE");
        seed.status = "PUBLISHED";
        seed.publishedAt = Instant.now().toString();
        seed.createdBy = "system";
        seed.updatedBy = "system";
        seed.startAt = "2026-06-01T12:00:00Z";
        seed.endAt = "2026-06-01T14:00:00Z";
        events.put(seed.eventId, seed);
    }

    Optional<CalendarEventRecord> findEvent(String eventId) {
        return Optional.ofNullable(events.get(eventId));
    }

    CalendarEventRecord requireEvent(String eventId) {
        return findEvent(eventId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 49900, "calendar event not found"));
    }

    CalendarEventRecord createEvent(Map<String, Object> body, Actor actor) {
        String sourceType = Objects.toString(body.getOrDefault("sourceType", "MANUAL"));
        String sourceId = body.get("sourceId") == null ? null : body.get("sourceId").toString();
        if (sourceId != null && events.values().stream()
                .anyMatch(event -> event.sourceType.equals(sourceType) && sourceId.equals(event.sourceId) && !"DELETED".equals(event.status))) {
            throw new ApiException(HttpStatus.CONFLICT, 49912, "calendar source event exists");
        }
        String id = "cal-" + eventSeq.incrementAndGet();
        CalendarEventRecord event = new CalendarEventRecord(id, sourceType, sourceId,
                text(body, "title", "北冥日程"), text(body, "summary", "日程摘要"), text(body, "type", "MAINTENANCE"));
        applyEventFields(event, body, actor);
        event.createdBy = actor.userId;
        events.put(id, event);
        return event;
    }

    void applyEventFields(CalendarEventRecord event, Map<String, Object> body, Actor actor) {
        event.title = text(body, "title", event.title);
        event.summary = text(body, "summary", event.summary);
        event.description = text(body, "description", event.description);
        event.type = text(body, "type", event.type);
        event.visibility = text(body, "visibility", event.visibility);
        event.startAt = text(body, "startAt", event.startAt);
        event.endAt = text(body, "endAt", event.endAt);
        event.timezone = text(body, "timezone", event.timezone);
        event.allDay = body.get("allDay") instanceof Boolean value ? value : event.allDay;
        event.location = text(body, "location", event.location);
        event.relatedUrl = text(body, "relatedUrl", event.relatedUrl);
        event.priority = body.get("priority") instanceof Number number ? number.intValue() : event.priority;
        event.sourceType = text(body, "sourceType", event.sourceType);
        event.sourceId = body.get("sourceId") == null ? event.sourceId : body.get("sourceId").toString();
        event.updatedBy = actor.userId;
        event.updatedAt = Instant.now().toString();
    }

    synchronized CalendarWatchRecord watch(CalendarEventRecord event, Actor actor, Map<String, Object> body) {
        Optional<CalendarWatchRecord> existing = watches.values().stream()
                .filter(watch -> watch.eventId.equals(event.eventId))
                .filter(watch -> watch.userId.equals(actor.userId))
                .findFirst();
        if (existing.isPresent()) {
            CalendarWatchRecord watch = existing.get();
            if ("CANCELED".equals(watch.status)) {
                watch.status = "ACTIVE";
                watch.canceledAt = null;
                event.watchCount++;
            }
            return watch;
        }
        String id = "cwatch-" + watchSeq.incrementAndGet();
        CalendarWatchRecord watch = new CalendarWatchRecord(id, event.eventId, actor, body);
        watches.put(id, watch);
        event.watchCount++;
        return watch;
    }

    synchronized CalendarWatchRecord unwatch(CalendarEventRecord event, Actor actor, Map<String, Object> body) {
        CalendarWatchRecord watch = watches.values().stream()
                .filter(existing -> existing.eventId.equals(event.eventId))
                .filter(existing -> existing.userId.equals(actor.userId))
                .findFirst()
                .orElseGet(() -> {
                    String id = "cwatch-" + watchSeq.incrementAndGet();
                    CalendarWatchRecord created = new CalendarWatchRecord(id, event.eventId, actor, body);
                    created.status = "CANCELED";
                    created.canceledAt = Instant.now().toString();
                    watches.put(id, created);
                    return created;
                });
        if ("ACTIVE".equals(watch.status)) {
            watch.status = "CANCELED";
            watch.canceledAt = Instant.now().toString();
            event.watchCount = Math.max(0, event.watchCount - 1);
        }
        watch.updatedAt = Instant.now().toString();
        return watch;
    }

    List<Map<String, Object>> activitySummaries() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("activityId", "act-calendar-summary-1");
        item.put("slug", "activity-calendar-summary-1");
        item.put("title", "北冥活动日程");
        item.put("type", "COMMUNITY");
        item.put("visibility", "PUBLIC");
        item.put("status", "PUBLISHED");
        item.put("startAt", "2026-06-10T12:00:00Z");
        item.put("endAt", "2026-06-10T14:00:00Z");
        item.put("registrationCloseAt", "2026-06-10T11:00:00Z");
        item.put("summary", "来自 activity 的只读日程摘要");
        return List.of(item);
    }

    boolean upsertActivity(Map<String, Object> summary, Actor actor) {
        String sourceId = summary.get("activityId").toString();
        Optional<CalendarEventRecord> existing = events.values().stream()
                .filter(event -> "ACTIVITY".equals(event.sourceType))
                .filter(event -> sourceId.equals(event.sourceId))
                .findFirst();
        CalendarEventRecord event = existing.orElseGet(() -> {
            String id = "cal-" + eventSeq.incrementAndGet();
            CalendarEventRecord created = new CalendarEventRecord(id, "ACTIVITY", sourceId, summary.get("title").toString(), summary.get("summary").toString(), "ACTIVITY");
            created.status = "PUBLISHED";
            created.createdBy = actor.userId;
            events.put(id, created);
            return created;
        });
        event.title = summary.get("title").toString();
        event.summary = summary.get("summary").toString();
        event.startAt = summary.get("startAt").toString();
        event.endAt = summary.get("endAt").toString();
        event.visibility = summary.get("visibility").toString();
        event.lastSyncedAt = Instant.now().toString();
        event.sourceSnapshotStale = false;
        event.updatedAt = Instant.now().toString();
        event.updatedBy = actor.userId;
        return existing.isEmpty();
    }

    void audit(String action, String eventId, String targetId, String actorUserId, String result) {
        audits.add(new CalendarAuditRecord("caud-" + (audits.size() + 1), action, eventId, targetId, actorUserId, result));
    }

    String sourceTypeFor(String eventId) {
        CalendarEventRecord event = events.get(eventId);
        return event == null ? "MANUAL" : event.sourceType;
    }

    Map<String, Object> ops(boolean testControlsEnabled, Actor actor, Map<String, Object> counts) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "calendar");
        data.put("port", 8132);
        data.put("legacyPort", 8114);
        data.put("storageMode", counts.getOrDefault("storageMode", "IN_MEMORY"));
        data.put("authMode", actor.authMode);
        data.put("actorUserId", actor.userId);
        data.put("activityMode", "TEST_STUB");
        data.put("notificationMode", "SKIPPED");
        data.put("changelogMode", "NOT_CONNECTED");
        data.put("testControlsEnabled", testControlsEnabled);
        data.put("eventsTotal", counts.getOrDefault("eventsTotal", events.size()));
        data.put("publishedEventsTotal", counts.getOrDefault("publishedEventsTotal", events.values().stream().filter(CalendarEventRecord::isPublicVisible).count()));
        data.put("watchesTotal", counts.getOrDefault("watchesTotal", watches.size()));
        data.put("activitySourceEventsTotal", counts.getOrDefault("activitySourceEventsTotal", events.values().stream().filter(event -> "ACTIVITY".equals(event.sourceType)).count()));
        data.put("manualEventsTotal", counts.getOrDefault("manualEventsTotal", events.values().stream().filter(event -> "MANUAL".equals(event.sourceType)).count()));
        data.put("auditsTotal", counts.getOrDefault("auditsTotal", audits.size()));
        data.put("idempotencyRecordsTotal", counts.getOrDefault("idempotencyRecordsTotal", idempotency.size()));
        data.put("lastActivitySyncAt", counts.getOrDefault("lastActivitySyncAt", lastActivitySyncAt));
        data.put("lastAuditAt", audits.isEmpty() ? null : audits.get(audits.size() - 1).createdAt);
        data.put("productionGaps", List.of("P1_IN_MEMORY_STORAGE", "P1_AUTH_STUB", "P1_ACTIVITY_STUB",
                "NOTIFICATION_DELIVERY_NOT_CONNECTED", "CHANGELOG_NOT_CONNECTED", "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"));
        return data;
    }

    String fingerprint(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(canonical(body));
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid json body");
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
}

record CalendarIdempotencyRecord(String fingerprint, HttpStatus status, Object data) {
}

class CalendarEventRecord {
    final String eventId;
    String sourceType;
    String sourceId;
    String sourceVersion;
    String title;
    String summary;
    String description = "日程描述";
    String type;
    String status = "DRAFT";
    String visibility = "PUBLIC";
    String startAt = "2026-06-01T12:00:00Z";
    String endAt = "2026-06-01T14:00:00Z";
    String timezone = "Asia/Shanghai";
    boolean allDay;
    String location = "北冥服务器";
    String relatedUrl = "/calendar";
    int priority = 50;
    int watchCount;
    String createdBy = "system";
    String updatedBy = "system";
    String reviewedBy;
    String submittedAt;
    String reviewedAt;
    String publishedAt;
    String offlineAt;
    String archivedAt;
    String deletedAt;
    String lastSyncedAt;
    boolean sourceSnapshotStale;
    Map<String, Object> reminderFailure;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    CalendarEventRecord(String eventId, String sourceType, String sourceId, String title, String summary, String type) {
        this.eventId = eventId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.title = title;
        this.summary = summary;
        this.type = type;
    }

    boolean isPublicVisible() {
        return "PUBLISHED".equals(status) && "PUBLIC".equals(visibility);
    }

    Map<String, Object> summaryView() {
        Map<String, Object> view = baseView(false);
        view.remove("description");
        return view;
    }

    Map<String, Object> publicView() {
        return baseView(false);
    }

    Map<String, Object> adminView() {
        Map<String, Object> view = baseView(true);
        view.put("createdBy", createdBy);
        view.put("updatedBy", updatedBy);
        view.put("reviewedBy", reviewedBy);
        view.put("submittedAt", submittedAt);
        view.put("reviewedAt", reviewedAt);
        view.put("deletedAt", deletedAt);
        return view;
    }

    private Map<String, Object> baseView(boolean includeReminderFailure) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("eventId", eventId);
        view.put("source", sourceView());
        view.put("title", title);
        view.put("summary", summary);
        view.put("description", description);
        view.put("type", type);
        view.put("status", status);
        view.put("visibility", visibility);
        view.put("startAt", startAt);
        view.put("endAt", endAt);
        view.put("timezone", timezone);
        view.put("allDay", allDay);
        view.put("location", location);
        view.put("relatedUrl", relatedUrl);
        view.put("labels", List.of("calendar"));
        view.put("priority", priority);
        view.put("watchCount", watchCount);
        view.put("reminderPolicy", reminderPolicy(includeReminderFailure));
        view.put("reviewComment", null);
        view.put("publishedAt", publishedAt);
        view.put("offlineAt", offlineAt);
        view.put("archivedAt", archivedAt);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        view.put("lastSyncedAt", lastSyncedAt);
        return view;
    }

    private Map<String, Object> sourceView() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("sourceType", sourceType);
        source.put("sourceId", sourceId);
        source.put("sourceVersion", sourceVersion);
        source.put("sourceUrl", sourceId == null ? null : "/source/" + sourceId);
        source.put("syncStatus", sourceSnapshotStale ? "STALE" : ("MANUAL".equals(sourceType) ? "SKIPPED" : "SYNCED"));
        source.put("sourceSnapshotStale", sourceSnapshotStale);
        source.put("lastSyncedAt", lastSyncedAt);
        source.put("failure", null);
        return source;
    }

    private Map<String, Object> reminderPolicy(boolean includeFailure) {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("enabled", true);
        policy.put("offsetMinutes", List.of(60));
        policy.put("channels", List.of("IN_APP"));
        policy.put("lastReminderStatus", reminderFailure == null ? "SKIPPED" : "FAILED");
        policy.put("lastReminderAt", null);
        policy.put("failure", includeFailure ? reminderFailure : null);
        return policy;
    }
}

class CalendarWatchRecord {
    final String watchId;
    final String eventId;
    final String userId;
    final String displayName;
    boolean reminderEnabled = true;
    List<Integer> reminderOffsets = List.of(60);
    String status = "ACTIVE";
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;
    String canceledAt;

    CalendarWatchRecord(String watchId, String eventId, Actor actor, Map<String, Object> body) {
        this.watchId = watchId;
        this.eventId = eventId;
        this.userId = actor.userId;
        this.displayName = actor.displayName;
        if (body.get("reminderEnabled") instanceof Boolean value) {
            this.reminderEnabled = value;
        }
        if (body.get("reminderOffsets") instanceof List<?> list) {
            this.reminderOffsets = list.stream().map(value -> Integer.parseInt(value.toString())).toList();
        }
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("watchId", watchId);
        view.put("eventId", eventId);
        view.put("userId", userId);
        view.put("displayNameSnapshot", displayName);
        view.put("reminderEnabled", reminderEnabled);
        view.put("reminderOffsets", reminderOffsets);
        view.put("status", status);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        view.put("canceledAt", canceledAt);
        return view;
    }
}

class CalendarAuditRecord {
    final String auditId;
    final String action;
    final String eventId;
    final String targetId;
    final String actorUserId;
    final String result;
    final String createdAt = Instant.now().toString();

    CalendarAuditRecord(String auditId, String action, String eventId, String targetId, String actorUserId, String result) {
        this.auditId = auditId;
        this.action = action;
        this.eventId = eventId;
        this.targetId = targetId;
        this.actorUserId = actorUserId;
        this.result = result;
    }

    String targetType() {
        if ("calendar-sync".equals(eventId)) {
            return "CALENDAR_SYNC";
        }
        if ("calendar".equals(eventId)) {
            return "CALENDAR_OPS";
        }
        return "CALENDAR_EVENT";
    }

    Map<String, Object> view(String sourceType) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", auditId);
        view.put("action", action);
        view.put("targetType", targetType());
        view.put("eventId", eventId);
        view.put("targetId", targetId);
        view.put("sourceType", sourceType);
        view.put("actorUserId", actorUserId);
        view.put("result", result);
        view.put("riskLevel", "LOW");
        view.put("createdAt", createdAt);
        return view;
    }
}

@Service
class CalendarAuth {
    private final CalendarProperties properties;

    CalendarAuth(CalendarProperties properties) {
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
            throw new ApiException(HttpStatus.BAD_GATEWAY, 49802, "auth incompatible");
        }
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        }
        if (!header.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        }
        String token = header.substring("Bearer ".length());
        return switch (token) {
            case "owner-token" -> local("owner-user", "OWNER", "Owner");
            case "admin-token" -> local("admin-user", "ADMIN", "Admin");
            case "helper-token" -> local("helper-user", "HELPER", "Helper");
            case "user-token" -> local("plain-user", "USER", "PlainUser");
            case "member-user-1-token" -> local("member-user-1", "USER", "MemberOne");
            case "member-user-2-token" -> local("member-user-2", "USER", "MemberTwo");
            default -> throw new ApiException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        };
    }

    void failIfRequested(HttpServletRequest request) {
        if (properties.enabled() && "unavailable".equals(request.getHeader("X-Test-Auth-Mode"))) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, 49800, "auth unavailable");
        }
    }

    Actor requireStaff(HttpServletRequest request) {
        Actor actor = current(request);
        if (!List.of("HELPER", "ADMIN", "OWNER").contains(actor.role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
        }
        return actor;
    }

    Actor requireAdmin(HttpServletRequest request) {
        Actor actor = current(request);
        if (!List.of("ADMIN", "OWNER").contains(actor.role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, 42001, "role denied");
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

interface CalendarFlowEvidenceRecorder {
    void recordEventWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordWatchWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);

    void recordActivitySyncWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode);
}

class NoopCalendarFlowEvidenceRecorder implements CalendarFlowEvidenceRecorder {
    @Override
    public void recordEventWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordWatchWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }

    @Override
    public void recordActivitySyncWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
    }
}

class PersistentCalendarFlowEvidenceRecorder implements CalendarFlowEvidenceRecorder {
    private final CalendarPersistence persistence;

    PersistentCalendarFlowEvidenceRecorder(CalendarPersistence persistence) {
        this.persistence = persistence;
    }

    @Override
    public void recordEventWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
        persist(request, action, payload, responseCode, "CALENDAR_EVENT");
    }

    @Override
    public void recordWatchWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
        persist(request, action, payload, responseCode, "CALENDAR_WATCH");
    }

    @Override
    public void recordActivitySyncWrite(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode) {
        persist(request, action, payload, responseCode, "CALENDAR_SYNC");
    }

    private void persist(HttpServletRequest request, String action, Map<String, Object> payload, int responseCode, String targetType) {
        String actorUserId = Objects.toString(request.getAttribute("calendar.actorUserId"), null);
        String actorRole = Objects.toString(request.getAttribute("calendar.actorRole"), null);
        String scope = Objects.toString(request.getAttribute("calendar.scope"), null);
        String idempotencyKey = Objects.toString(request.getAttribute("calendar.idempotencyKey"), null);
        String fingerprint = Objects.toString(request.getAttribute("calendar.fingerprint"), null);
        String reason = Objects.toString(request.getAttribute("calendar.reason"), null);
        Map<String, Object> snapshot = snapshotFor(action, payload);
        persistence.persistWrite(request, actorUserId, actorRole, scope, action, targetType, targetId(action, payload), "LOW",
                beforeStatus(payload), afterStatus(payload), reason, idempotencyKey, fingerprint, snapshot, payload, responseCode);
    }

    private Map<String, Object> snapshotFor(String action, Map<String, Object> payload) {
        Map<String, Object> snapshot = new LinkedHashMap<>(payload);
        snapshot.put("snapshotType", switch (action) {
            case "CALENDAR_EVENT_WATCHED", "CALENDAR_EVENT_UNWATCHED" -> "WATCH";
            case "CALENDAR_ACTIVITY_SYNCED" -> "ACTIVITY_SYNC";
            default -> "EVENT";
        });
        return snapshot;
    }

    private String targetId(String action, Map<String, Object> payload) {
        if (payload.containsKey("eventId")) {
            return Objects.toString(payload.get("eventId"), null);
        }
        Object watch = payload.get("watch");
        if (watch instanceof Map<?, ?> watchMap && watchMap.get("watchId") != null) {
            return watchMap.get("watchId").toString();
        }
        if ("CALENDAR_ACTIVITY_SYNCED".equals(action)) {
            return "activity";
        }
        return null;
    }

    private String beforeStatus(Map<String, Object> payload) {
        return Objects.toString(payload.get("beforeStatus"), null);
    }

    private String afterStatus(Map<String, Object> payload) {
        Object status = payload.get("status");
        if (status != null) {
            return status.toString();
        }
        Object watch = payload.get("watch");
        if (watch instanceof Map<?, ?> watchMap && watchMap.get("status") != null) {
            return watchMap.get("status").toString();
        }
        Object syncStatus = payload.get("syncStatus");
        return syncStatus == null ? null : syncStatus.toString();
    }
}

@Component
class CalendarProperties {
    private final boolean enabled;

    CalendarProperties(@Value("${calendar.test-controls.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }
}

@Component
class CalendarRequestIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .filter(value -> !value.isBlank())
                .orElse("req_" + UUID.randomUUID());
        request.setAttribute("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}

@RestControllerAdvice(basePackageClasses = CalendarController.class)
class CalendarExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException exception, HttpServletRequest request) {
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
        body.put("code", 54800);
        body.put("message", "calendar internal error");
        body.put("data", null);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

class ApiException extends RuntimeException {
    final HttpStatus status;
    final int code;

    ApiException(HttpStatus status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
