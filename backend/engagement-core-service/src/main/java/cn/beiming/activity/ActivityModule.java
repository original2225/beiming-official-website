package cn.beiming.activity;

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
import org.springframework.web.bind.annotation.PutMapping;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/activity")
class ActivityController {
    private final ActivityStore store;
    private final ActivityAuth auth;
    private final ActivityProperties properties;

    ActivityController(ActivityStore store, ActivityAuth auth, ActivityProperties properties) {
        this.store = store;
        this.auth = auth;
        this.properties = properties;
    }

    @GetMapping("/events")
    ResponseEntity<Map<String, Object>> publicEvents(HttpServletRequest request,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int pageSize,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String sort) {
        validatePage(page, pageSize);
        validateSort(sort, "startAt_asc", "startAt_desc", "publishedAt_desc", "createdAt_desc");
        List<Map<String, Object>> items = store.activities.values().stream()
                .filter(ActivityRecord::isPublicVisible)
                .filter(activity -> keyword == null || activity.title.contains(keyword) || activity.summary.contains(keyword))
                .sorted(Comparator.comparing(activity -> activity.createdAt))
                .map(ActivityRecord::publicView)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/events/{activityIdOrSlug}")
    ResponseEntity<Map<String, Object>> publicEvent(HttpServletRequest request, @PathVariable String activityIdOrSlug) {
        ActivityRecord activity = store.findActivity(activityIdOrSlug).filter(ActivityRecord::isPublicVisible)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 49600, "activity not found"));
        return ok(request, activity.publicView());
    }

    @GetMapping("/events/{activityId}/result")
    ResponseEntity<Map<String, Object>> publicResult(HttpServletRequest request, @PathVariable String activityId) {
        ActivityRecord activity = store.requireActivity(activityId);
        ActivityResultRecord result = store.results.get(activity.activityId);
        if (!"RESULT_PUBLISHED".equals(activity.status) || result == null || !"PUBLISHED".equals(result.status)) {
            throw new ApiException(HttpStatus.NOT_FOUND, 49602, "activity result not found");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("result", result.view());
        data.put("rewards", store.rewards.values().stream()
                .filter(reward -> reward.activityId.equals(activity.activityId))
                .map(ActivityRewardRecord::publicView)
                .toList());
        return ok(request, data);
    }

    @GetMapping("/calendar-summary")
    ResponseEntity<Map<String, Object>> calendarSummary(HttpServletRequest request,
                                                        @RequestParam(required = false) String sort) {
        validateSort(sort, "startAt_asc", "startAt_desc");
        List<Map<String, Object>> items = store.activities.values().stream()
                .filter(ActivityRecord::isPublicVisible)
                .map(ActivityRecord::calendarSummary)
                .toList();
        return ok(request, Map.of("items", items));
    }

    @GetMapping("/me/registrations")
    ResponseEntity<Map<String, Object>> myRegistrations(HttpServletRequest request,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int pageSize) {
        Actor actor = auth.current(request);
        validatePage(page, pageSize);
        List<Map<String, Object>> items = store.registrations.values().stream()
                .filter(registration -> registration.userId.equals(actor.userId))
                .map(ActivityRegistrationRecord::currentUserView)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/me/registrations/{registrationId}")
    ResponseEntity<Map<String, Object>> myRegistration(HttpServletRequest request, @PathVariable String registrationId) {
        Actor actor = auth.current(request);
        ActivityRegistrationRecord registration = store.requireRegistration(registrationId);
        if (!registration.userId.equals(actor.userId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, 49601, "registration not found");
        }
        return ok(request, registration.currentUserView());
    }

    @PostMapping("/me/events/{activityId}/registrations")
    ResponseEntity<Map<String, Object>> register(HttpServletRequest request,
                                                 @PathVariable String activityId,
                                                 @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        return idempotent(request, actor, body, () -> {
            if (properties.enabled() && "unavailable".equals(request.getHeader("X-Test-Profile-Mode"))) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, 49410, "profile unavailable");
            }
            if (number(body.get("guestCount"), 0) != 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "guestCount is not supported");
            }
            ActivityRecord activity = store.requireActivity(activityId);
            if (!activity.isRegistrationOpen(currentInstant(request))) {
                throw new ApiException(HttpStatus.CONFLICT, 49612, "registration is not open");
            }
            if (activity.requiresMember() && actor.memberId == null) {
                throw new ApiException(HttpStatus.FORBIDDEN, 49620, "activity eligibility denied");
            }
            Optional<ActivityRegistrationRecord> existing = store.registrations.values().stream()
                    .filter(registration -> registration.activityId.equals(activity.activityId))
                    .filter(registration -> registration.userId.equals(actor.userId))
                    .filter(registration -> !List.of("CANCELED", "REJECTED").contains(registration.status))
                    .findFirst();
            if (existing.isPresent()) {
                throw new ApiException(HttpStatus.CONFLICT, 49614, "duplicate registration");
            }
            ensureAuditWritable(request);
            ActivityRegistrationRecord registration = store.createRegistration(activity, actor, body);
            store.audit("ACTIVITY_REGISTERED", activity.activityId, registration.registrationId, actor.userId, "SUCCESS");
            return created(request, registration.currentUserView());
        });
    }

    @PostMapping("/me/registrations/{registrationId}/cancel")
    ResponseEntity<Map<String, Object>> cancelMine(HttpServletRequest request,
                                                   @PathVariable String registrationId,
                                                   @RequestBody Map<String, Object> body) {
        Actor actor = auth.current(request);
        return idempotent(request, actor, body, () -> {
            ActivityRegistrationRecord registration = store.requireRegistration(registrationId);
            if (!registration.userId.equals(actor.userId)) {
                throw new ApiException(HttpStatus.NOT_FOUND, 49601, "registration not found");
            }
            ActivityRecord activity = store.requireActivity(registration.activityId);
            if (List.of("RUNNING", "COMPLETED", "RESULT_PUBLISHED").contains(activity.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49611, "registration cannot be canceled");
            }
            ensureAuditWritable(request);
            store.cancelRegistration(registration, actor.userId);
            store.audit("ACTIVITY_REGISTRATION_CANCELED", activity.activityId, registration.registrationId, actor.userId, "SUCCESS");
            return ok(request, registration.currentUserView());
        });
    }

    @GetMapping("/me/events/{activityId}/check-in")
    ResponseEntity<Map<String, Object>> myCheckIn(HttpServletRequest request, @PathVariable String activityId) {
        Actor actor = auth.current(request);
        return ok(request, store.registrations.values().stream()
                .filter(registration -> registration.activityId.equals(activityId))
                .filter(registration -> registration.userId.equals(actor.userId))
                .findFirst()
                .map(ActivityRegistrationRecord::currentUserView)
                .orElse(null));
    }

    @GetMapping("/me/rewards")
    ResponseEntity<Map<String, Object>> myRewards(HttpServletRequest request,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize) {
        Actor actor = auth.current(request);
        validatePage(page, pageSize);
        List<Map<String, Object>> items = store.rewards.values().stream()
                .filter(reward -> reward.userId.equals(actor.userId))
                .map(ActivityRewardRecord::currentUserView)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/admin/events")
    ResponseEntity<Map<String, Object>> adminEvents(HttpServletRequest request,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "20") int pageSize,
                                                    @RequestParam(required = false) String sort) {
        auth.requireStaff(request);
        validatePage(page, pageSize);
        validateSort(sort, "createdAt_desc", "updatedAt_desc", "startAt_asc", "startAt_desc");
        List<Map<String, Object>> items = store.activities.values().stream().map(ActivityRecord::adminView).toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/admin/events/{activityId}")
    ResponseEntity<Map<String, Object>> adminEvent(HttpServletRequest request, @PathVariable String activityId) {
        auth.requireStaff(request);
        ActivityRecord activity = store.requireActivity(activityId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activity", activity.adminView());
        data.put("registrationsTotal", store.countRegistrations(activity.activityId));
        data.put("result", Optional.ofNullable(store.results.get(activity.activityId)).map(ActivityResultRecord::view).orElse(null));
        data.put("rewardsTotal", store.countRewards(activity.activityId));
        data.put("contributionCandidatesTotal", store.countCandidates(activity.activityId));
        data.put("dependencySummary", Map.of("profile", "TEST_STUB", "notification", "TEST_STUB"));
        return ok(request, data);
    }

    @PostMapping("/admin/events")
    ResponseEntity<Map<String, Object>> createEvent(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            if (properties.enabled() && body.containsKey("linkedContentId") && "unavailable".equals(request.getHeader("X-Test-Content-Mode"))) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, 49440, "content unavailable");
            }
            ensureAuditWritable(request);
            ActivityRecord activity = store.createActivity(body, actor);
            store.audit("ACTIVITY_CREATED", activity.activityId, activity.activityId, actor.userId, "SUCCESS");
            return created(request, activity.adminView());
        });
    }

    @PatchMapping("/admin/events/{activityId}")
    ResponseEntity<Map<String, Object>> updateEvent(HttpServletRequest request,
                                                    @PathVariable String activityId,
                                                    @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            if (!List.of("DRAFT", "NEEDS_CHANGES", "REJECTED", "APPROVED").contains(activity.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49610, "activity state conflict");
            }
            store.validateEditableFields(activity, body);
            ensureAuditWritable(request);
            store.applyEditableFields(activity, body);
            activity.updatedAt = now();
            store.audit("ACTIVITY_UPDATED", activity.activityId, activity.activityId, actor.userId, "SUCCESS");
            return ok(request, activity.adminView());
        });
    }

    @PostMapping("/admin/events/{activityId}/submit")
    ResponseEntity<Map<String, Object>> submitEvent(HttpServletRequest request,
                                                    @PathVariable String activityId,
                                                    @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            if (!List.of("DRAFT", "NEEDS_CHANGES", "REJECTED").contains(activity.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49610, "activity state conflict");
            }
            ensureAuditWritable(request);
            activity.status = "PENDING_REVIEW";
            activity.submittedAt = now();
            store.audit("ACTIVITY_SUBMITTED", activity.activityId, activity.activityId, actor.userId, "SUCCESS");
            return ok(request, activity.adminView());
        });
    }

    @PatchMapping("/admin/events/{activityId}/approve")
    ResponseEntity<Map<String, Object>> approveEvent(HttpServletRequest request,
                                                     @PathVariable String activityId,
                                                     @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            if (!List.of("PENDING_REVIEW", "NEEDS_CHANGES").contains(activity.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49610, "activity state conflict");
            }
            ensureAuditWritable(request);
            activity.status = "APPROVED";
            activity.reviewedAt = now();
            activity.notificationFailure = notificationFailure(request);
            store.audit("ACTIVITY_APPROVED", activity.activityId, activity.activityId, actor.userId, "SUCCESS");
            return ok(request, activity.adminView());
        });
    }

    @PatchMapping("/admin/events/{activityId}/reject")
    ResponseEntity<Map<String, Object>> rejectEvent(HttpServletRequest request,
                                                    @PathVariable String activityId,
                                                    @RequestBody Map<String, Object> body) {
        return reviewEvent(request, activityId, body, "REJECTED", "ACTIVITY_REJECTED");
    }

    @PatchMapping("/admin/events/{activityId}/request-changes")
    ResponseEntity<Map<String, Object>> requestChanges(HttpServletRequest request,
                                                       @PathVariable String activityId,
                                                       @RequestBody Map<String, Object> body) {
        return reviewEvent(request, activityId, body, "NEEDS_CHANGES", "ACTIVITY_CHANGES_REQUESTED");
    }

    @PatchMapping("/admin/events/{activityId}/publish")
    ResponseEntity<Map<String, Object>> publishEvent(HttpServletRequest request,
                                                     @PathVariable String activityId,
                                                     @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            if (!"APPROVED".equals(activity.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49610, "activity state conflict");
            }
            ensureAuditWritable(request);
            activity.status = "REGISTRATION_OPEN";
            activity.publishedAt = now();
            store.audit("ACTIVITY_PUBLISHED", activity.activityId, activity.activityId, actor.userId, "SUCCESS");
            return ok(request, activity.adminView());
        });
    }

    @PatchMapping("/admin/events/{activityId}/open-registration")
    ResponseEntity<Map<String, Object>> openRegistration(HttpServletRequest request,
                                                         @PathVariable String activityId,
                                                         @RequestBody Map<String, Object> body) {
        return transitionStaff(request, activityId, body, List.of("PUBLISHED", "REGISTRATION_CLOSED"), "REGISTRATION_OPEN", "ACTIVITY_REGISTRATION_OPENED");
    }

    @PatchMapping("/admin/events/{activityId}/close-registration")
    ResponseEntity<Map<String, Object>> closeRegistration(HttpServletRequest request,
                                                          @PathVariable String activityId,
                                                          @RequestBody Map<String, Object> body) {
        return transitionStaff(request, activityId, body, List.of("REGISTRATION_OPEN"), "REGISTRATION_CLOSED", "ACTIVITY_REGISTRATION_CLOSED");
    }

    @PatchMapping("/admin/events/{activityId}/start")
    ResponseEntity<Map<String, Object>> startEvent(HttpServletRequest request,
                                                   @PathVariable String activityId,
                                                   @RequestBody Map<String, Object> body) {
        return transitionStaff(request, activityId, body, List.of("PUBLISHED", "REGISTRATION_OPEN", "REGISTRATION_CLOSED"), "RUNNING", "ACTIVITY_STARTED");
    }

    @PatchMapping("/admin/events/{activityId}/complete")
    ResponseEntity<Map<String, Object>> completeEvent(HttpServletRequest request,
                                                      @PathVariable String activityId,
                                                      @RequestBody Map<String, Object> body) {
        return transitionStaff(request, activityId, body, List.of("RUNNING"), "COMPLETED", "ACTIVITY_COMPLETED");
    }

    @PatchMapping("/admin/events/{activityId}/offline")
    ResponseEntity<Map<String, Object>> offlineEvent(HttpServletRequest request,
                                                     @PathVariable String activityId,
                                                     @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            ensureAuditWritable(request);
            activity.status = "OFFLINE";
            activity.offlineAt = now();
            store.audit("ACTIVITY_OFFLINED", activity.activityId, activity.activityId, actor.userId, "SUCCESS");
            return ok(request, activity.adminView());
        });
    }

    @PatchMapping("/admin/events/{activityId}/archive")
    ResponseEntity<Map<String, Object>> archiveEvent(HttpServletRequest request,
                                                     @PathVariable String activityId,
                                                     @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            ensureAuditWritable(request);
            activity.status = "ARCHIVED";
            activity.archivedAt = now();
            store.audit("ACTIVITY_ARCHIVED", activity.activityId, activity.activityId, actor.userId, "SUCCESS");
            return ok(request, activity.adminView());
        });
    }

    @PatchMapping("/admin/events/{activityId}/delete")
    ResponseEntity<Map<String, Object>> deleteEvent(HttpServletRequest request,
                                                    @PathVariable String activityId,
                                                    @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            requireConfirm(body, "DELETE_ACTIVITY_EVENT");
            ActivityRecord activity = store.requireActivity(activityId);
            ensureAuditWritable(request);
            activity.status = "DELETED";
            activity.deletedAt = now();
            store.audit("ACTIVITY_DELETED", activity.activityId, activity.activityId, actor.userId, "SUCCESS");
            return ok(request, activity.adminView());
        });
    }

    @GetMapping("/admin/events/{activityId}/registrations")
    ResponseEntity<Map<String, Object>> adminRegistrations(HttpServletRequest request,
                                                           @PathVariable String activityId,
                                                           @RequestParam(defaultValue = "1") int page,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        auth.requireStaff(request);
        validatePage(page, pageSize);
        store.requireActivity(activityId);
        List<Map<String, Object>> items = store.registrations.values().stream()
                .filter(registration -> registration.activityId.equals(activityId))
                .map(ActivityRegistrationRecord::adminView)
                .toList();
        return ok(request, page(items, page, pageSize));
    }

    @PatchMapping("/admin/registrations/{registrationId}/confirm")
    ResponseEntity<Map<String, Object>> confirmRegistration(HttpServletRequest request,
                                                            @PathVariable String registrationId,
                                                            @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRegistrationRecord registration = store.requireRegistration(registrationId);
            ActivityRecord activity = store.requireActivity(registration.activityId);
            if (!List.of("SUBMITTED", "WAITLISTED").contains(registration.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49611, "registration state conflict");
            }
            if (!activity.hasConfirmedSlot()) {
                throw new ApiException(HttpStatus.CONFLICT, 49613, "activity capacity is full");
            }
            ensureAuditWritable(request);
            registration.status = "CONFIRMED";
            activity.confirmedCount++;
            if (registration.waitlistRank != null) {
                activity.waitlistedCount = Math.max(0, activity.waitlistedCount - 1);
            }
            registration.updatedAt = now();
            registration.notificationFailure = notificationFailure(request);
            store.audit("ACTIVITY_REGISTRATION_CONFIRMED", activity.activityId, registration.registrationId, actor.userId, "SUCCESS");
            return ok(request, registration.adminView());
        });
    }

    @PatchMapping("/admin/registrations/{registrationId}/reject")
    ResponseEntity<Map<String, Object>> rejectRegistration(HttpServletRequest request,
                                                           @PathVariable String registrationId,
                                                           @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRegistrationRecord registration = store.requireRegistration(registrationId);
            ActivityRecord activity = store.requireActivity(registration.activityId);
            if (!List.of("SUBMITTED", "WAITLISTED").contains(registration.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49611, "registration state conflict");
            }
            ensureAuditWritable(request);
            registration.status = "REJECTED";
            registration.updatedAt = now();
            registration.notificationFailure = notificationFailure(request);
            store.audit("ACTIVITY_REGISTRATION_REJECTED", activity.activityId, registration.registrationId, actor.userId, "SUCCESS");
            return ok(request, registration.adminView());
        });
    }

    @PatchMapping("/admin/registrations/{registrationId}/promote")
    ResponseEntity<Map<String, Object>> promoteRegistration(HttpServletRequest request,
                                                            @PathVariable String registrationId,
                                                            @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRegistrationRecord registration = store.requireRegistration(registrationId);
            ActivityRecord activity = store.requireActivity(registration.activityId);
            if (!"WAITLISTED".equals(registration.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49615, "registration is not waitlisted");
            }
            if (!activity.hasConfirmedSlot()) {
                throw new ApiException(HttpStatus.CONFLICT, 49613, "activity capacity is full");
            }
            ensureAuditWritable(request);
            registration.status = "CONFIRMED";
            registration.updatedAt = now();
            activity.confirmedCount++;
            activity.waitlistedCount = Math.max(0, activity.waitlistedCount - 1);
            registration.notificationFailure = notificationFailure(request);
            store.audit("ACTIVITY_WAITLIST_PROMOTED", activity.activityId, registration.registrationId, actor.userId, "SUCCESS");
            return ok(request, registration.adminView());
        });
    }

    @PatchMapping("/admin/registrations/{registrationId}/cancel")
    ResponseEntity<Map<String, Object>> cancelRegistration(HttpServletRequest request,
                                                           @PathVariable String registrationId,
                                                           @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRegistrationRecord registration = store.requireRegistration(registrationId);
            ensureAuditWritable(request);
            store.cancelRegistration(registration, actor.userId);
            store.audit("ACTIVITY_REGISTRATION_ADMIN_CANCELED", registration.activityId, registration.registrationId, actor.userId, "SUCCESS");
            return ok(request, registration.adminView());
        });
    }

    @PatchMapping("/admin/registrations/{registrationId}/check-in")
    ResponseEntity<Map<String, Object>> checkIn(HttpServletRequest request,
                                                @PathVariable String registrationId,
                                                @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRegistrationRecord registration = store.requireRegistration(registrationId);
            ActivityRecord activity = store.requireActivity(registration.activityId);
            if (!"CONFIRMED".equals(registration.status) && !"CHECKED_IN".equals(registration.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49611, "registration state conflict");
            }
            Instant current = currentInstant(request);
            if (!activity.isCheckInWindowOpen(current)) {
                throw new ApiException(HttpStatus.CONFLICT, 49618, "activity check-in window is closed");
            }
            ensureAuditWritable(request);
            if (!"CHECKED_IN".equals(registration.status)) {
                registration.status = "CHECKED_IN";
                registration.checkedInAt = current.toString();
                activity.checkedInCount++;
            }
            registration.updatedAt = now();
            registration.notificationFailure = notificationFailure(request);
            store.audit("ACTIVITY_REGISTRATION_CHECKED_IN", activity.activityId, registration.registrationId, actor.userId, "SUCCESS");
            return ok(request, registration.adminView());
        });
    }

    @PatchMapping("/admin/registrations/{registrationId}/no-show")
    ResponseEntity<Map<String, Object>> noShow(HttpServletRequest request,
                                               @PathVariable String registrationId,
                                               @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRegistrationRecord registration = store.requireRegistration(registrationId);
            ActivityRecord activity = store.requireActivity(registration.activityId);
            if (!"CONFIRMED".equals(registration.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49611, "registration state conflict");
            }
            ensureAuditWritable(request);
            registration.status = "NO_SHOW";
            registration.noShowAt = now();
            registration.updatedAt = now();
            activity.noShowCount++;
            store.audit("ACTIVITY_REGISTRATION_NO_SHOW", activity.activityId, registration.registrationId, actor.userId, "SUCCESS");
            return ok(request, registration.adminView());
        });
    }

    @PutMapping("/admin/events/{activityId}/result")
    ResponseEntity<Map<String, Object>> upsertResult(HttpServletRequest request,
                                                     @PathVariable String activityId,
                                                     @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            if (!List.of("COMPLETED", "RESULT_PUBLISHED").contains(activity.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49610, "activity state conflict");
            }
            ensureAuditWritable(request);
            ActivityResultRecord result = store.results.computeIfAbsent(activity.activityId, id -> new ActivityResultRecord("res-" + store.resultSeq.incrementAndGet(), activity.activityId));
            result.title = text(body, "title", "活动结果");
            result.summary = text(body, "summary", "活动完成");
            result.details = text(body, "details", "");
            result.updatedAt = now();
            store.audit("ACTIVITY_RESULT_UPSERTED", activity.activityId, result.resultId, actor.userId, "SUCCESS");
            return ok(request, result.view());
        });
    }

    @PatchMapping("/admin/events/{activityId}/result/publish")
    ResponseEntity<Map<String, Object>> publishResult(HttpServletRequest request,
                                                      @PathVariable String activityId,
                                                      @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            ActivityResultRecord result = store.results.get(activity.activityId);
            if (result == null) {
                throw new ApiException(HttpStatus.NOT_FOUND, 49602, "result not found");
            }
            ensureAuditWritable(request);
            result.status = "PUBLISHED";
            result.publishedAt = now();
            activity.status = "RESULT_PUBLISHED";
            store.audit("ACTIVITY_RESULT_PUBLISHED", activity.activityId, result.resultId, actor.userId, "SUCCESS");
            return ok(request, result.view());
        });
    }

    @PostMapping("/admin/events/{activityId}/rewards")
    ResponseEntity<Map<String, Object>> createReward(HttpServletRequest request,
                                                     @PathVariable String activityId,
                                                     @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            ActivityRegistrationRecord registration = store.requireRegistration(text(body, "registrationId", ""));
            if (!registration.activityId.equals(activity.activityId) || !"CHECKED_IN".equals(registration.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49616, "reward state conflict");
            }
            ensureAuditWritable(request);
            ActivityRewardRecord reward = store.createReward(activity, registration, body);
            store.audit("ACTIVITY_REWARD_CREATED", activity.activityId, reward.rewardId, actor.userId, "SUCCESS");
            return created(request, reward.adminView());
        });
    }

    @PatchMapping("/admin/rewards/{rewardId}/issue")
    ResponseEntity<Map<String, Object>> issueReward(HttpServletRequest request,
                                                    @PathVariable String rewardId,
                                                    @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRewardRecord reward = store.requireReward(rewardId);
            if (!List.of("PENDING_ISSUE", "ISSUED").contains(reward.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49616, "reward state conflict");
            }
            ensureAuditWritable(request);
            reward.status = "ISSUED";
            reward.issuedAt = now();
            reward.notificationFailure = notificationFailure(request);
            store.audit("ACTIVITY_REWARD_ISSUED", reward.activityId, reward.rewardId, actor.userId, "SUCCESS");
            return ok(request, reward.adminView());
        });
    }

    @PatchMapping("/admin/rewards/{rewardId}/revoke")
    ResponseEntity<Map<String, Object>> revokeReward(HttpServletRequest request,
                                                     @PathVariable String rewardId,
                                                     @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            requireConfirm(body, "REVOKE_ACTIVITY_REWARD");
            ActivityRewardRecord reward = store.requireReward(rewardId);
            ensureAuditWritable(request);
            reward.status = "REVOKED";
            reward.revokedAt = now();
            store.audit("ACTIVITY_REWARD_REVOKED", reward.activityId, reward.rewardId, actor.userId, "SUCCESS");
            return ok(request, reward.adminView());
        });
    }

    @PostMapping("/admin/events/{activityId}/contribution-candidates")
    ResponseEntity<Map<String, Object>> contributionCandidates(HttpServletRequest request,
                                                               @PathVariable String activityId,
                                                               @RequestBody Map<String, Object> body) {
        Actor actor = auth.requireAdmin(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            if (!"RESULT_PUBLISHED".equals(activity.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49622, "contribution candidate is not allowed");
            }
            ensureAuditWritable(request);
            List<Map<String, Object>> created = new ArrayList<>();
            for (ActivityRewardRecord reward : store.rewards.values()) {
                if (reward.activityId.equals(activity.activityId) && "ISSUED".equals(reward.status)) {
                    ActivityContributionRecord candidate = store.createCandidate(activity, reward);
                    created.add(candidate.view());
                }
            }
            store.audit("ACTIVITY_CONTRIBUTION_CANDIDATES_CREATED", activity.activityId, activity.activityId, actor.userId, "SUCCESS");
            return created(request, Map.of("items", created, "total", created.size()));
        });
    }

    @GetMapping("/admin/audit-logs")
    ResponseEntity<Map<String, Object>> auditLogs(HttpServletRequest request,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int pageSize) {
        auth.requireAdmin(request);
        validatePage(page, pageSize);
        List<Map<String, Object>> items = store.audits.stream().map(ActivityAuditRecord::view).toList();
        return ok(request, page(items, page, pageSize));
    }

    @GetMapping("/admin/ops/summary")
    ResponseEntity<Map<String, Object>> opsSummary(HttpServletRequest request) {
        auth.requireStaff(request);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "activity");
        data.put("port", 8132);
        data.put("legacyPort", 8113);
        data.put("storageMode", "IN_MEMORY");
        data.put("authMode", "TEST_STUB");
        data.put("profileMode", "TEST_STUB");
        data.put("notificationMode", "TEST_STUB");
        data.put("attendanceMode", "SKIPPED");
        data.put("communityMode", "TEST_STUB");
        data.put("contentMode", "TEST_STUB");
        data.put("resourceMode", "TEST_STUB");
        data.put("testControlsEnabled", properties.enabled());
        data.put("activitiesTotal", store.activities.size());
        data.put("publishedActivitiesTotal", store.activities.values().stream().filter(ActivityRecord::isPublicVisible).count());
        data.put("openRegistrationsTotal", store.registrations.size());
        data.put("waitlistedRegistrationsTotal", store.registrations.values().stream().filter(registration -> "WAITLISTED".equals(registration.status)).count());
        data.put("checkedInRegistrationsTotal", store.registrations.values().stream().filter(registration -> "CHECKED_IN".equals(registration.status)).count());
        data.put("resultsPublishedTotal", store.results.values().stream().filter(result -> "PUBLISHED".equals(result.status)).count());
        data.put("rewardsTotal", store.rewards.size());
        data.put("contributionCandidatesTotal", store.candidates.size());
        data.put("auditsTotal", store.audits.size());
        data.put("idempotencyRecordsTotal", store.idempotencyRecords.size());
        data.put("lastAuditAt", store.audits.isEmpty() ? null : store.audits.get(store.audits.size() - 1).createdAt);
        data.put("productionGaps", List.of(
                "P1_IN_MEMORY_STORAGE",
                "P1_AUTH_STUB",
                "P1_PROFILE_STUB",
                "P1_NOTIFICATION_STUB",
                "P1_COMMUNITY_STUB",
                "P1_CONTENT_STUB",
                "P1_RESOURCE_STUB",
                "ATTENDANCE_CONTRIBUTION_NOT_CONNECTED",
                "CALENDAR_NOT_CONNECTED",
                "TEST_CONTROLS_DISABLED_OUTSIDE_TEST"
        ));
        return ok(request, data);
    }

    private ResponseEntity<Map<String, Object>> reviewEvent(HttpServletRequest request, String activityId, Map<String, Object> body, String status, String action) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            if (!"PENDING_REVIEW".equals(activity.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49610, "activity state conflict");
            }
            ensureAuditWritable(request);
            activity.status = status;
            activity.reviewedAt = now();
            store.audit(action, activity.activityId, activity.activityId, actor.userId, "SUCCESS");
            return ok(request, activity.adminView());
        });
    }

    private ResponseEntity<Map<String, Object>> transitionStaff(HttpServletRequest request, String activityId, Map<String, Object> body,
                                                                List<String> from, String to, String action) {
        Actor actor = auth.requireStaff(request);
        return idempotent(request, actor, body, () -> {
            ActivityRecord activity = store.requireActivity(activityId);
            if (!from.contains(activity.status)) {
                throw new ApiException(HttpStatus.CONFLICT, 49610, "activity state conflict");
            }
            ensureAuditWritable(request);
            activity.status = to;
            activity.updatedAt = now();
            store.audit(action, activity.activityId, activity.activityId, actor.userId, "SUCCESS");
            return ok(request, activity.adminView());
        });
    }

    private Map<String, Object> notificationFailure(HttpServletRequest request) {
        if (properties.enabled() && "unavailable".equals(request.getHeader("X-Test-Notification-Mode"))) {
            return Map.of(
                    "status", "FAILED",
                    "failureCode", "49420",
                    "failureType", "UNAVAILABLE",
                    "failureReason", "notification unavailable",
                    "failedAt", now()
            );
        }
        return null;
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40002, "invalid pagination");
        }
    }

    private void validateSort(String sort, String... allowed) {
        if (sort == null || sort.isBlank()) {
            return;
        }
        if (List.of(allowed).contains(sort)) {
            return;
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, 40003, "invalid sort");
    }

    private void validateIdempotency(Map<String, Object> body) {
        Object key = body == null ? null : body.get("idempotencyKey");
        if (key != null && (key.toString().length() < 8 || key.toString().length() > 80)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid idempotencyKey");
        }
    }

    private void ensureAuditWritable(HttpServletRequest request) {
        if (properties.enabled() && "true".equals(request.getHeader("X-Test-Fail-Audit"))) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, 54601, "activity audit failed");
        }
    }

    private ResponseEntity<Map<String, Object>> idempotent(HttpServletRequest request,
                                                           Actor actor,
                                                           Map<String, Object> body,
                                                           Supplier<ResponseEntity<Map<String, Object>>> action) {
        validateIdempotency(body);
        String idempotencyKey = idempotencyKey(body);
        if (idempotencyKey == null) {
            return action.get();
        }
        String semanticKey = request.getMethod() + " " + request.getRequestURI();
        String storageKey = actor.userId + "|" + semanticKey + "|" + idempotencyKey;
        String fingerprint = store.fingerprint(body);
        synchronized (store) {
            ActivityIdempotencyRecord existing = store.idempotencyRecords.get(storageKey);
            if (existing != null) {
                if (!existing.fingerprint().equals(fingerprint)) {
                    throw new ApiException(HttpStatus.CONFLICT, 49617, "idempotency key conflict");
                }
                return response(request, existing.status(), existing.code(), existing.message(), existing.data());
            }
            ResponseEntity<Map<String, Object>> result = action.get();
            Map<String, Object> resultBody = Objects.requireNonNull(result.getBody());
            store.idempotencyRecords.put(storageKey, new ActivityIdempotencyRecord(
                    fingerprint,
                    HttpStatus.valueOf(result.getStatusCode().value()),
                    ((Number) resultBody.get("code")).intValue(),
                    resultBody.get("message").toString(),
                    resultBody.get("data")
            ));
            return result;
        }
    }

    private String idempotencyKey(Map<String, Object> body) {
        Object value = body == null ? null : body.get("idempotencyKey");
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return value.toString();
    }

    private void requireConfirm(Map<String, Object> body, String expected) {
        validateIdempotency(body);
        if (!expected.equals(text(body, "confirmText", ""))) {
            throw new ApiException(HttpStatus.FORBIDDEN, 42003, "confirmation required");
        }
    }

    private <T> Map<String, Object> page(List<T> items, int page, int pageSize) {
        int from = Math.min((page - 1) * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", items.subList(from, to));
        data.put("page", page);
        data.put("pageSize", pageSize);
        data.put("total", items.size());
        return data;
    }

    private ResponseEntity<Map<String, Object>> ok(HttpServletRequest request, Object data) {
        return response(request, HttpStatus.OK, 0, "success", data);
    }

    private ResponseEntity<Map<String, Object>> created(HttpServletRequest request, Object data) {
        return response(request, HttpStatus.CREATED, 0, "success", data);
    }

    private ResponseEntity<Map<String, Object>> response(HttpServletRequest request, HttpStatus status, int code, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        body.put("requestId", request.getAttribute("requestId"));
        return ResponseEntity.status(status).body(body);
    }

    private String text(Map<String, Object> body, String key, String fallback) {
        Object value = body == null ? null : body.get(key);
        return value == null ? fallback : value.toString();
    }

    private int number(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        return Integer.parseInt(value.toString());
    }

    private String now() {
        return Instant.now().toString();
    }

    private Instant currentInstant(HttpServletRequest request) {
        String testNow = request.getHeader("X-Test-Now");
        if (properties.enabled() && testNow != null && !testNow.isBlank()) {
            return Instant.parse(testNow);
        }
        return Instant.now();
    }
}

@Service
class ActivityStore {
    final Map<String, ActivityRecord> activities = new ConcurrentHashMap<>();
    final Map<String, ActivityRegistrationRecord> registrations = new ConcurrentHashMap<>();
    final Map<String, ActivityResultRecord> results = new ConcurrentHashMap<>();
    final Map<String, ActivityRewardRecord> rewards = new ConcurrentHashMap<>();
    final Map<String, ActivityContributionRecord> candidates = new ConcurrentHashMap<>();
    final List<ActivityAuditRecord> audits = new ArrayList<>();
    final Map<String, ActivityIdempotencyRecord> idempotencyRecords = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    final AtomicInteger activitySeq = new AtomicInteger();
    final AtomicInteger registrationSeq = new AtomicInteger();
    final AtomicInteger resultSeq = new AtomicInteger();
    final AtomicInteger rewardSeq = new AtomicInteger();
    final AtomicInteger candidateSeq = new AtomicInteger();

    @PostConstruct
    void seed() {
        ActivityRecord seed = new ActivityRecord("act-seed-open", "seed-open", "北冥公开活动", "公开活动摘要", "公开活动描述");
        seed.status = "REGISTRATION_OPEN";
        seed.publishedAt = Instant.now().toString();
        activities.put(seed.activityId, seed);
    }

    Optional<ActivityRecord> findActivity(String idOrSlug) {
        ActivityRecord direct = activities.get(idOrSlug);
        if (direct != null) {
            return Optional.of(direct);
        }
        return activities.values().stream().filter(activity -> activity.slug.equals(idOrSlug)).findFirst();
    }

    ActivityRecord requireActivity(String activityId) {
        return findActivity(activityId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, 49600, "activity not found"));
    }

    ActivityRegistrationRecord requireRegistration(String registrationId) {
        ActivityRegistrationRecord registration = registrations.get(registrationId);
        if (registration == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 49601, "registration not found");
        }
        return registration;
    }

    ActivityRewardRecord requireReward(String rewardId) {
        ActivityRewardRecord reward = rewards.get(rewardId);
        if (reward == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, 49603, "reward not found");
        }
        return reward;
    }

    ActivityRecord createActivity(Map<String, Object> body, Actor actor) {
        String slug = text(body, "slug", "activity-" + activitySeq.incrementAndGet());
        if (activities.values().stream().anyMatch(activity -> activity.slug.equals(slug))) {
            throw new ApiException(HttpStatus.CONFLICT, 49619, "activity slug exists");
        }
        Instant start = Instant.parse(text(body, "startAt", "2026-06-01T12:00:00Z"));
        Instant end = Instant.parse(text(body, "endAt", "2026-06-01T14:00:00Z"));
        if (!end.isAfter(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid activity time");
        }
        String registrationOpenAt = text(body, "registrationOpenAt", null);
        String registrationCloseAt = text(body, "registrationCloseAt", null);
        if (registrationCloseAt != null && Instant.parse(registrationCloseAt).isAfter(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid registration close time");
        }
        if (registrationOpenAt != null && registrationCloseAt != null && Instant.parse(registrationOpenAt).isAfter(Instant.parse(registrationCloseAt))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid registration open time");
        }
        int capacity = number(body.get("capacity"), 10);
        int waitlistCapacity = number(body.get("waitlistCapacity"), 0);
        if (capacity < 1 || waitlistCapacity < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid activity capacity");
        }
        String id = "act-" + activitySeq.incrementAndGet();
        ActivityRecord activity = new ActivityRecord(id, slug, text(body, "title", "北冥活动"), text(body, "summary", "活动摘要"), text(body, "description", "活动描述"));
        activity.type = text(body, "type", "BUILD");
        activity.visibility = text(body, "visibility", "PUBLIC");
        activity.registrationPolicy = text(body, "registrationPolicy", "OPEN");
        activity.startAt = start.toString();
        activity.endAt = end.toString();
        activity.registrationOpenAt = registrationOpenAt;
        activity.registrationCloseAt = registrationCloseAt;
        activity.capacity = capacity;
        activity.waitlistCapacity = waitlistCapacity;
        activity.locationText = text(body, "locationText", null);
        activity.coverImageUrl = text(body, "coverImageUrl", null);
        activity.createdBy = actor.userId;
        activities.put(id, activity);
        return activity;
    }

    void applyEditableFields(ActivityRecord activity, Map<String, Object> body) {
        if (body.containsKey("title")) activity.title = text(body, "title", activity.title);
        if (body.containsKey("summary")) activity.summary = text(body, "summary", activity.summary);
        if (body.containsKey("description")) activity.description = text(body, "description", activity.description);
        if (body.containsKey("startAt")) activity.startAt = text(body, "startAt", activity.startAt);
        if (body.containsKey("endAt")) activity.endAt = text(body, "endAt", activity.endAt);
        if (body.containsKey("registrationOpenAt")) activity.registrationOpenAt = text(body, "registrationOpenAt", activity.registrationOpenAt);
        if (body.containsKey("registrationCloseAt")) activity.registrationCloseAt = text(body, "registrationCloseAt", activity.registrationCloseAt);
        if (body.containsKey("capacity")) activity.capacity = number(body.get("capacity"), activity.capacity);
        if (body.containsKey("waitlistCapacity")) activity.waitlistCapacity = number(body.get("waitlistCapacity"), activity.waitlistCapacity);
    }

    void validateEditableFields(ActivityRecord activity, Map<String, Object> body) {
        String startAt = body.containsKey("startAt") ? text(body, "startAt", activity.startAt) : activity.startAt;
        String endAt = body.containsKey("endAt") ? text(body, "endAt", activity.endAt) : activity.endAt;
        String registrationOpenAt = body.containsKey("registrationOpenAt") ? text(body, "registrationOpenAt", activity.registrationOpenAt) : activity.registrationOpenAt;
        String registrationCloseAt = body.containsKey("registrationCloseAt") ? text(body, "registrationCloseAt", activity.registrationCloseAt) : activity.registrationCloseAt;
        int capacity = body.containsKey("capacity") ? number(body.get("capacity"), activity.capacity) : activity.capacity;
        int waitlistCapacity = body.containsKey("waitlistCapacity") ? number(body.get("waitlistCapacity"), activity.waitlistCapacity) : activity.waitlistCapacity;
        Instant start = Instant.parse(startAt);
        Instant end = Instant.parse(endAt);
        if (!end.isAfter(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid activity time");
        }
        if (registrationCloseAt != null && Instant.parse(registrationCloseAt).isAfter(start)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid registration close time");
        }
        if (registrationOpenAt != null && registrationCloseAt != null && Instant.parse(registrationOpenAt).isAfter(Instant.parse(registrationCloseAt))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid registration open time");
        }
        if (capacity < 1 || waitlistCapacity < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 40001, "invalid activity capacity");
        }
    }

    ActivityRegistrationRecord createRegistration(ActivityRecord activity, Actor actor, Map<String, Object> body) {
        String id = "areg-" + registrationSeq.incrementAndGet();
        ActivityRegistrationRecord registration = new ActivityRegistrationRecord(id, activity.activityId, actor);
        if ("APPROVAL_REQUIRED".equals(activity.registrationPolicy)) {
            registration.status = "SUBMITTED";
        } else if (activity.hasConfirmedSlot()) {
            registration.status = "CONFIRMED";
            activity.confirmedCount++;
        } else if (activity.hasWaitlistSlot()) {
            registration.status = "WAITLISTED";
            registration.waitlistRank = activity.waitlistedCount + 1;
            activity.waitlistedCount++;
        } else {
            throw new ApiException(HttpStatus.CONFLICT, 49613, "activity capacity is full");
        }
        registrations.put(id, registration);
        return registration;
    }

    void cancelRegistration(ActivityRegistrationRecord registration, String actorUserId) {
        ActivityRecord activity = requireActivity(registration.activityId);
        if (!List.of("SUBMITTED", "CONFIRMED", "WAITLISTED").contains(registration.status)) {
            throw new ApiException(HttpStatus.CONFLICT, 49611, "registration state conflict");
        }
        if ("CONFIRMED".equals(registration.status)) {
            activity.confirmedCount = Math.max(0, activity.confirmedCount - 1);
        }
        if ("WAITLISTED".equals(registration.status)) {
            activity.waitlistedCount = Math.max(0, activity.waitlistedCount - 1);
        }
        registration.status = "CANCELED";
        registration.canceledAt = Instant.now().toString();
        registration.updatedAt = Instant.now().toString();
    }

    ActivityRewardRecord createReward(ActivityRecord activity, ActivityRegistrationRecord registration, Map<String, Object> body) {
        String id = "arwd-" + rewardSeq.incrementAndGet();
        ActivityRewardRecord reward = new ActivityRewardRecord(id, activity.activityId, registration, body);
        rewards.put(id, reward);
        return reward;
    }

    ActivityContributionRecord createCandidate(ActivityRecord activity, ActivityRewardRecord reward) {
        String id = "acand-" + candidateSeq.incrementAndGet();
        ActivityContributionRecord candidate = new ActivityContributionRecord(id, activity.activityId, reward);
        candidates.put(id, candidate);
        return candidate;
    }

    int countRegistrations(String activityId) {
        return (int) registrations.values().stream().filter(registration -> registration.activityId.equals(activityId)).count();
    }

    int countRewards(String activityId) {
        return (int) rewards.values().stream().filter(reward -> reward.activityId.equals(activityId)).count();
    }

    int countCandidates(String activityId) {
        return (int) candidates.values().stream().filter(candidate -> candidate.activityId.equals(activityId)).count();
    }

    void audit(String action, String activityId, String targetId, String actorUserId, String result) {
        audits.add(new ActivityAuditRecord("aaud-" + (audits.size() + 1), action, activityId, targetId, actorUserId, result));
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

    private int number(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        return Integer.parseInt(value.toString());
    }
}

record ActivityIdempotencyRecord(String fingerprint, HttpStatus status, int code, String message, Object data) {
}

class ActivityRecord {
    final String activityId;
    final String slug;
    String title;
    String summary;
    String description;
    String type = "BUILD";
    String visibility = "PUBLIC";
    String registrationPolicy = "OPEN";
    String status = "DRAFT";
    String startAt = "2026-06-01T12:00:00Z";
    String endAt = "2026-06-01T14:00:00Z";
    String registrationOpenAt;
    String registrationCloseAt;
    int capacity = 10;
    int waitlistCapacity = 5;
    int confirmedCount;
    int waitlistedCount;
    int checkedInCount;
    int noShowCount;
    String locationText;
    String coverImageUrl;
    String createdBy = "system";
    String submittedAt;
    String reviewedAt;
    String publishedAt;
    String offlineAt;
    String archivedAt;
    String deletedAt;
    Map<String, Object> notificationFailure;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    ActivityRecord(String activityId, String slug, String title, String summary, String description) {
        this.activityId = activityId;
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.description = description;
    }

    boolean isPublicVisible() {
        return List.of("PUBLISHED", "REGISTRATION_OPEN", "REGISTRATION_CLOSED", "RUNNING", "COMPLETED", "RESULT_PUBLISHED").contains(status);
    }

    boolean isRegistrationOpen(Instant current) {
        if (!"REGISTRATION_OPEN".equals(status)) {
            return false;
        }
        if (registrationOpenAt != null && current.isBefore(Instant.parse(registrationOpenAt))) {
            return false;
        }
        return registrationCloseAt == null || !current.isAfter(Instant.parse(registrationCloseAt));
    }

    boolean isCheckInWindowOpen(Instant current) {
        Instant start = Instant.parse(startAt).minusSeconds(3600);
        Instant end = Instant.parse(endAt).plusSeconds(86400);
        return !current.isBefore(start) && !current.isAfter(end);
    }

    boolean requiresMember() {
        return List.of("MEMBER_ONLY", "STAFF_ONLY", "INVITE_ONLY").contains(visibility);
    }

    boolean hasConfirmedSlot() {
        return capacity < 0 || confirmedCount < capacity;
    }

    boolean hasWaitlistSlot() {
        return waitlistCapacity > 0 && waitlistedCount < waitlistCapacity;
    }

    Map<String, Object> publicView() {
        Map<String, Object> view = baseView();
        view.put("description", description);
        return view;
    }

    Map<String, Object> adminView() {
        Map<String, Object> view = publicView();
        view.put("createdBy", createdBy);
        view.put("submittedAt", submittedAt);
        view.put("reviewedAt", reviewedAt);
        view.put("publishedAt", publishedAt);
        view.put("offlineAt", offlineAt);
        view.put("archivedAt", archivedAt);
        view.put("deletedAt", deletedAt);
        view.put("notificationFailure", notificationFailure);
        return view;
    }

    Map<String, Object> calendarSummary() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("activityId", activityId);
        view.put("slug", slug);
        view.put("title", title);
        view.put("type", type);
        view.put("visibility", visibility);
        view.put("status", status);
        view.put("startAt", startAt);
        view.put("endAt", endAt);
        view.put("registrationCloseAt", registrationCloseAt);
        view.put("summary", summary);
        return view;
    }

    private Map<String, Object> baseView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("activityId", activityId);
        view.put("slug", slug);
        view.put("title", title);
        view.put("summary", summary);
        view.put("type", type);
        view.put("visibility", visibility);
        view.put("registrationPolicy", registrationPolicy);
        view.put("status", status);
        view.put("startAt", startAt);
        view.put("endAt", endAt);
        view.put("registrationOpenAt", registrationOpenAt);
        view.put("registrationCloseAt", registrationCloseAt);
        view.put("capacity", capacity);
        view.put("waitlistCapacity", waitlistCapacity);
        view.put("confirmedCount", confirmedCount);
        view.put("waitlistedCount", waitlistedCount);
        view.put("checkedInCount", checkedInCount);
        view.put("noShowCount", noShowCount);
        view.put("locationText", locationText);
        view.put("coverImageUrl", coverImageUrl);
        view.put("tags", List.of("activity"));
        view.put("profileSnapshotStale", false);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        return view;
    }
}

class ActivityRegistrationRecord {
    final String registrationId;
    final String activityId;
    final String userId;
    final String memberId;
    final String displayName;
    String status = "SUBMITTED";
    Integer waitlistRank;
    String checkedInAt;
    String noShowAt;
    String canceledAt;
    Map<String, Object> notificationFailure;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    ActivityRegistrationRecord(String registrationId, String activityId, Actor actor) {
        this.registrationId = registrationId;
        this.activityId = activityId;
        this.userId = actor.userId;
        this.memberId = actor.memberId;
        this.displayName = actor.displayName;
    }

    Map<String, Object> currentUserView() {
        Map<String, Object> view = baseView();
        view.put("notificationStatus", notificationFailure == null ? "DELIVERED" : "FAILED");
        return view;
    }

    Map<String, Object> adminView() {
        Map<String, Object> view = baseView();
        view.put("notificationFailure", notificationFailure);
        return view;
    }

    private Map<String, Object> baseView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("registrationId", registrationId);
        view.put("activityId", activityId);
        view.put("participant", participant());
        view.put("status", status);
        view.put("answers", Map.of("note", "我会准时参加"));
        view.put("guestCount", 0);
        view.put("waitlistRank", waitlistRank);
        view.put("checkedInAt", checkedInAt);
        view.put("noShowAt", noShowAt);
        view.put("canceledAt", canceledAt);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        return view;
    }

    Map<String, Object> participant() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("userId", userId);
        view.put("memberId", memberId);
        view.put("displayNameSnapshot", displayName);
        view.put("avatarUrlSnapshot", null);
        view.put("memberGroupSnapshot", "default");
        view.put("memberStatusSnapshot", memberId == null ? null : "ACTIVE");
        view.put("minecraftIdSnapshot", memberId == null ? null : displayName + "MC");
        view.put("profileSnapshotStale", false);
        return view;
    }
}

class ActivityResultRecord {
    final String resultId;
    final String activityId;
    String status = "DRAFT";
    String title = "活动结果";
    String summary = "活动完成";
    String details = "";
    String publishedAt;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    ActivityResultRecord(String resultId, String activityId) {
        this.resultId = resultId;
        this.activityId = activityId;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("resultId", resultId);
        view.put("activityId", activityId);
        view.put("status", status);
        view.put("title", title);
        view.put("summary", summary);
        view.put("details", details);
        view.put("participantTotal", 1);
        view.put("winnerTotal", 1);
        view.put("publishedAt", publishedAt);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        return view;
    }
}

class ActivityRewardRecord {
    final String rewardId;
    final String activityId;
    final String registrationId;
    final String userId;
    final String memberId;
    final Map<String, Object> recipient;
    String type;
    String title;
    String description;
    int quantity;
    int candidateDelta;
    String status = "PENDING_ISSUE";
    String issuedAt;
    String revokedAt;
    Map<String, Object> notificationFailure;
    final String createdAt = Instant.now().toString();
    String updatedAt = createdAt;

    ActivityRewardRecord(String rewardId, String activityId, ActivityRegistrationRecord registration, Map<String, Object> body) {
        this.rewardId = rewardId;
        this.activityId = activityId;
        this.registrationId = registration.registrationId;
        this.userId = registration.userId;
        this.memberId = registration.memberId;
        this.recipient = registration.participant();
        this.type = Objects.toString(body.get("type"), "POINTS_CANDIDATE");
        this.title = Objects.toString(body.get("title"), "活动贡献奖励");
        this.description = Objects.toString(body.get("description"), "");
        this.quantity = body.get("quantity") instanceof Number number ? number.intValue() : 1;
        this.candidateDelta = body.get("scoreCandidateDelta") instanceof Number number ? number.intValue() : 0;
    }

    Map<String, Object> publicView() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("rewardId", rewardId);
        view.put("activityId", activityId);
        view.put("recipient", recipient);
        view.put("type", type);
        view.put("title", title);
        view.put("quantity", quantity);
        view.put("status", status);
        return view;
    }

    Map<String, Object> currentUserView() {
        return publicView();
    }

    Map<String, Object> adminView() {
        Map<String, Object> view = publicView();
        view.put("registrationId", registrationId);
        view.put("description", description);
        view.put("scoreCandidateDelta", candidateDelta);
        view.put("issuedAt", issuedAt);
        view.put("revokedAt", revokedAt);
        view.put("notificationFailure", notificationFailure);
        view.put("createdAt", createdAt);
        view.put("updatedAt", updatedAt);
        return view;
    }
}

class ActivityContributionRecord {
    final String candidateId;
    final String activityId;
    final String rewardId;
    final String memberId;
    final String userId;
    final int delta;
    final String createdAt = Instant.now().toString();

    ActivityContributionRecord(String candidateId, String activityId, ActivityRewardRecord reward) {
        this.candidateId = candidateId;
        this.activityId = activityId;
        this.rewardId = reward.rewardId;
        this.memberId = reward.memberId;
        this.userId = reward.userId;
        this.delta = reward.candidateDelta;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("candidateId", candidateId);
        view.put("activityId", activityId);
        view.put("rewardId", rewardId);
        view.put("memberId", memberId);
        view.put("userId", userId);
        view.put("title", "活动贡献候选");
        view.put("description", "等待 future attendance 正式入口接收");
        view.put("scoreDelta", delta);
        view.put("status", "PENDING");
        view.put("attendanceResponseSummary", null);
        view.put("createdAt", createdAt);
        view.put("updatedAt", createdAt);
        return view;
    }
}

class ActivityAuditRecord {
    final String auditId;
    final String action;
    final String activityId;
    final String targetId;
    final String actorUserId;
    final String result;
    final String createdAt = Instant.now().toString();

    ActivityAuditRecord(String auditId, String action, String activityId, String targetId, String actorUserId, String result) {
        this.auditId = auditId;
        this.action = action;
        this.activityId = activityId;
        this.targetId = targetId;
        this.actorUserId = actorUserId;
        this.result = result;
    }

    Map<String, Object> view() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", auditId);
        view.put("action", action);
        view.put("activityId", activityId);
        view.put("targetId", targetId);
        view.put("actorUserId", actorUserId);
        view.put("result", result);
        view.put("riskLevel", "LOW");
        view.put("createdAt", createdAt);
        return view;
    }
}

@Service
class ActivityAuth {
    Actor current(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || header.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, 41000, "unauthenticated");
        }
        if (!header.startsWith("Bearer ")) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        }
        String token = header.substring("Bearer ".length());
        return switch (token) {
            case "owner-token" -> new Actor("owner-user", "OWNER", "Owner", "mem-owner");
            case "admin-token" -> new Actor("admin-user", "ADMIN", "Admin", "mem-admin");
            case "helper-token" -> new Actor("helper-user", "HELPER", "Helper", "mem-helper");
            case "user-token" -> new Actor("plain-user", "USER", "PlainUser", null);
            case "member-user-1-token" -> new Actor("member-user-1", "USER", "MemberOne", "mem-001");
            case "member-user-2-token" -> new Actor("member-user-2", "USER", "MemberTwo", "mem-002");
            case "pending-profile-token" -> new Actor("pending-user", "USER", "PendingUser", null);
            default -> throw new ApiException(HttpStatus.UNAUTHORIZED, 41003, "bad token");
        };
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
}

class Actor {
    final String userId;
    final String role;
    final String displayName;
    final String memberId;

    Actor(String userId, String role, String displayName, String memberId) {
        this.userId = userId;
        this.role = role;
        this.displayName = displayName;
        this.memberId = memberId;
    }
}

@Component
class ActivityProperties {
    private final boolean enabled;

    ActivityProperties(@Value("${activity.test-controls.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    boolean enabled() {
        return enabled;
    }
}

@Component
class ActivityRequestIdFilter extends OncePerRequestFilter {
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

@RestControllerAdvice
class ActivityExceptionHandler {
    private final ObjectMapper objectMapper;

    ActivityExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
        body.put("code", 54600);
        body.put("message", "activity internal error");
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
