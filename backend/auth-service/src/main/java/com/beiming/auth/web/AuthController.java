package com.beiming.auth.web;

import com.beiming.auth.api.ApiResponse;
import com.beiming.auth.api.PageResponse;
import com.beiming.auth.api.RequestIdFilter;
import com.beiming.auth.domain.InviteStatus;
import com.beiming.auth.domain.InviteType;
import com.beiming.auth.domain.Permission;
import com.beiming.auth.domain.Role;
import com.beiming.auth.domain.UserStatus;
import com.beiming.auth.service.AuthDtos.AuthResult;
import com.beiming.auth.service.AuthDtos.InviteCreation;
import com.beiming.auth.service.AuthDtos.InviteSummary;
import com.beiming.auth.service.AuthDtos.InviteUseSummary;
import com.beiming.auth.service.AuthDtos.UserSummary;
import com.beiming.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    ResponseEntity<ApiResponse<AuthResult>> register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        AuthResult result = authService.register(request.inviteCode(), request.username(), request.password(),
                request.displayName(), request.email(), sourceIp(servletRequest), requestId(servletRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @PostMapping("/login")
    ApiResponse<AuthResult> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.login(request.username(), request.password(), sourceIp(servletRequest),
                requestId(servletRequest)));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization,
            HttpServletRequest request) {
        authService.logout(token(authorization), sourceIp(request), requestId(request));
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    ApiResponse<UserSummary> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success(authService.me(token(authorization)));
    }

    @PostMapping("/session/verify")
    ApiResponse<Map<String, Object>> verify(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success(authService.verifySession(token(authorization)));
    }

    @PostMapping("/password-reset/request")
    ApiResponse<Map<String, Object>> requestPasswordReset(@RequestBody PasswordResetRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.requestPasswordReset(request.usernameOrEmail(), sourceIp(servletRequest),
                requestId(servletRequest)));
    }

    @PostMapping("/password-reset/confirm")
    ApiResponse<Map<String, Object>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.confirmPasswordReset(request.token(), request.newPassword(),
                sourceIp(servletRequest), requestId(servletRequest)));
    }

    @GetMapping("/users")
    ApiResponse<PageResponse<UserSummary>> users(@RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status) {
        return ApiResponse.success(authService.users(token(authorization), page, pageSize, keyword, role, status));
    }

    @GetMapping("/users/{userId}")
    ApiResponse<UserSummary> user(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String userId) {
        return ApiResponse.success(authService.user(token(authorization), userId));
    }

    @PatchMapping("/users/{userId}")
    ApiResponse<UserSummary> updateUser(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String userId,
            @RequestBody Map<String, Object> updates,
            HttpServletRequest request) {
        return ApiResponse.success(authService.updateUser(token(authorization), userId, updates, sourceIp(request),
                requestId(request)));
    }

    @PatchMapping("/users/{userId}/roles")
    ApiResponse<UserSummary> updateRoles(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String userId,
            @Valid @RequestBody RoleUpdateRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.updateRoles(token(authorization), userId, request.role(),
                request.permissions() == null ? List.of() : request.permissions(), request.reason(),
                sourceIp(servletRequest), requestId(servletRequest)));
    }

    @GetMapping("/invites")
    ApiResponse<PageResponse<InviteSummary>> invites(@RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(authService.invites(token(authorization), page, pageSize));
    }

    @PostMapping("/invites")
    ResponseEntity<ApiResponse<InviteCreation>> createInvite(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody InviteCreateRequest request,
            HttpServletRequest servletRequest) {
        InviteCreation result = authService.createInvite(token(authorization), request.type(), request.role(),
                request.permissions() == null ? List.of() : request.permissions(), request.maxUses(), request.expiresAt(),
                request.note(), sourceIp(servletRequest), requestId(servletRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @PatchMapping("/invites/{inviteId}")
    ApiResponse<InviteSummary> updateInvite(@RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String inviteId,
            @RequestBody InviteUpdateRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.updateInvite(token(authorization), inviteId, request.status(),
                sourceIp(servletRequest), requestId(servletRequest)));
    }

    @GetMapping("/invites/{inviteId}/uses")
    ApiResponse<PageResponse<InviteUseSummary>> inviteUses(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String inviteId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(authService.inviteUses(token(authorization), inviteId, page, pageSize));
    }

    @PutMapping("/minecraft-binding")
    ApiResponse<UserSummary> bindMinecraft(@RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody MinecraftBindingRequest request,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.bindMinecraft(token(authorization), request.minecraftId(),
                request.minecraftUuid(), sourceIp(servletRequest), requestId(servletRequest)));
    }

    @DeleteMapping("/minecraft-binding")
    ApiResponse<UserSummary> unbindMinecraft(@RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String userId,
            HttpServletRequest servletRequest) {
        return ApiResponse.success(authService.unbindMinecraft(token(authorization), userId, sourceIp(servletRequest),
                requestId(servletRequest)));
    }

    private String token(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }

    private String sourceIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? null : requestId.toString();
    }

    record RegisterRequest(
            @NotBlank String inviteCode,
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String displayName,
            @Email @NotBlank String email) {
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    record PasswordResetRequest(String usernameOrEmail) {
    }

    record PasswordResetConfirmRequest(@NotBlank String token, @NotBlank String newPassword) {
    }

    record RoleUpdateRequest(@NotNull Role role, List<Permission> permissions, String reason) {
    }

    record InviteCreateRequest(
            @NotNull InviteType type,
            @NotNull Role role,
            List<Permission> permissions,
            @Min(1) int maxUses,
            @NotNull Instant expiresAt,
            String note) {
    }

    record InviteUpdateRequest(InviteStatus status) {
    }

    record MinecraftBindingRequest(@NotBlank String minecraftId, @NotBlank String minecraftUuid) {
    }
}
