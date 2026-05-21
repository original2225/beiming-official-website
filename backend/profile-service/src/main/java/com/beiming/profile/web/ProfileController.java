package com.beiming.profile.web;

import com.beiming.profile.api.ApiResponse;
import com.beiming.profile.api.PageResponse;
import com.beiming.profile.api.RequestIdFilter;
import com.beiming.profile.domain.AuthContext;
import com.beiming.profile.domain.MemberStatus;
import com.beiming.profile.domain.Role;
import com.beiming.profile.service.ProfileService;
import com.beiming.profile.service.ProfileService.MutationResult;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/members")
    ApiResponse<PageResponse<Map<String, Object>>> publicMembers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String memberGroupId,
            @RequestParam(required = false) MemberStatus status) {
        return ApiResponse.success(profileService.publicMembers(page, pageSize, keyword, memberGroupId, status));
    }

    @GetMapping("/members/{profileId}")
    ApiResponse<Map<String, Object>> publicMember(@PathVariable String profileId) {
        return ApiResponse.success(profileService.publicMember(profileId));
    }

    @GetMapping("/me")
    ApiResponse<Map<String, Object>> me(HttpServletRequest request) {
        return ApiResponse.success(profileService.me(auth(request)));
    }

    @PatchMapping("/me")
    ApiResponse<Map<String, Object>> updateMe(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        return ApiResponse.success(profileService.updateMe(auth(request), body));
    }

    @GetMapping("/admin/members")
    ApiResponse<PageResponse<Map<String, Object>>> adminMembers(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String memberGroupId,
            @RequestParam(required = false) MemberStatus status,
            @RequestParam(required = false) Boolean publicVisible) {
        return ApiResponse.success(profileService.adminMembers(auth(request), page, pageSize, keyword, memberGroupId, status,
                publicVisible));
    }

    @PostMapping("/admin/members")
    ResponseEntity<ApiResponse<Map<String, Object>>> createMember(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        MutationResult result = profileService.createMember(auth(request), body, false);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result.data()));
    }

    @GetMapping("/admin/members/{profileId}")
    ApiResponse<Map<String, Object>> adminMember(HttpServletRequest request, @PathVariable String profileId) {
        return ApiResponse.success(profileService.adminMember(auth(request), profileId));
    }

    @PatchMapping("/admin/members/{profileId}")
    ApiResponse<Map<String, Object>> updateAdminMember(HttpServletRequest request, @PathVariable String profileId,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(profileService.updateAdminMember(auth(request), profileId, body));
    }

    @PatchMapping("/admin/members/{profileId}/status")
    ApiResponse<Map<String, Object>> updateStatus(HttpServletRequest request, @PathVariable String profileId,
            @RequestBody StatusUpdateRequest body) {
        return ApiResponse.success(profileService.updateStatus(auth(request), profileId, body.status(), body.reason()));
    }

    @PostMapping("/admin/groups")
    ResponseEntity<ApiResponse<Map<String, Object>>> createGroup(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(profileService.createGroup(auth(request), body)));
    }

    @GetMapping("/admin/groups")
    ApiResponse<List<Map<String, Object>>> groups(HttpServletRequest request) {
        return ApiResponse.success(profileService.groups(auth(request)));
    }

    @PatchMapping("/admin/groups/{groupId}")
    ApiResponse<Map<String, Object>> updateGroup(HttpServletRequest request, @PathVariable String groupId,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(profileService.updateGroup(auth(request), groupId, body));
    }

    @PostMapping("/internal/members/activate")
    ResponseEntity<ApiResponse<Map<String, Object>>> activateMember(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        MutationResult result = profileService.createMember(auth(request), body, true);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success(result.data()));
    }

    private AuthContext auth(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        String token = authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length())
                : null;
        String roleHeader = request.getHeader("X-Auth-Role");
        Role role = roleHeader == null || roleHeader.isBlank() ? Role.USER : Role.valueOf(roleHeader);
        String permissionsHeader = request.getHeader("X-Auth-Permissions");
        List<String> permissions = permissionsHeader == null || permissionsHeader.isBlank()
                ? List.of()
                : Arrays.stream(permissionsHeader.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList();
        return new AuthContext(token, request.getHeader("X-Auth-User-Id"), role, permissions);
    }

    @SuppressWarnings("unused")
    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? null : requestId.toString();
    }

    record StatusUpdateRequest(MemberStatus status, String reason) {
    }
}
