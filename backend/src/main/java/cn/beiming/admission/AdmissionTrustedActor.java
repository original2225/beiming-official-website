package cn.beiming.admission;

import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AdmissionTrustedActor {
    private static final Set<String> ALLOWED_ROLES = Set.of("OWNER", "ADMIN", "HELPER", "USER");

    private AdmissionTrustedActor() {
    }

    public static boolean hasGatewayContext(HttpServletRequest request) {
        return request != null && !Objects.toString(request.getHeader("X-Gateway-Internal-Request-Id"), "").isBlank();
    }

    public static Actor parse(HttpServletRequest request) {
        String userId = Objects.toString(request.getHeader("X-Beiming-Actor-User-Id"), "").trim();
        if (userId.isBlank()) {
            throw new IllegalArgumentException("missing actor user");
        }
        return new Actor(userId, "Actor " + userId, roles(request.getHeader("X-Beiming-Actor-Roles")), minecraftBinding(request));
    }

    private static Set<String> roles(String header) {
        Set<String> roles = new LinkedHashSet<>();
        if (header == null || header.isBlank()) {
            return roles;
        }
        for (String value : header.split(",")) {
            String role = value.trim();
            if (role.isBlank()) {
                continue;
            }
            if (!ALLOWED_ROLES.contains(role)) {
                throw new IllegalArgumentException("invalid actor role");
            }
            roles.add(role);
        }
        return roles;
    }

    private static Map<String, Object> minecraftBinding(HttpServletRequest request) {
        String minecraftId = Objects.toString(request.getHeader("X-Beiming-Actor-Minecraft-Id"), "").trim();
        String minecraftUuid = Objects.toString(request.getHeader("X-Beiming-Actor-Minecraft-Uuid"), "").trim();
        if (minecraftId.isBlank() && minecraftUuid.isBlank()) {
            return null;
        }
        Map<String, Object> binding = new LinkedHashMap<>();
        binding.put("minecraftId", minecraftId.isBlank() ? null : minecraftId);
        binding.put("minecraftUuid", minecraftUuid.isBlank() ? null : minecraftUuid);
        binding.put("verified", true);
        binding.put("snapshotAt", "2026-06-03T00:00:00+08:00");
        return binding;
    }

    public record Actor(String userId, String displayName, Set<String> roles, Map<String, Object> minecraftBinding) {
    }
}
