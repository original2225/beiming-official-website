package cn.beiming.engagement;

import jakarta.servlet.http.HttpServletRequest;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

public final class TrustedGatewayAuth {
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9_.:-]{1,128}");
    private static final Pattern MINECRAFT_UUID_PATTERN = Pattern.compile("[a-f0-9]{32}");
    private static final Set<String> VALID_ROLES = Set.of("OWNER", "ADMIN", "HELPER", "USER");
    private static final Set<String> VALID_PERMISSIONS = Set.of("NODE_READ", "NODE_WRITE", "CONTAINER_OPERATE",
            "VM_OPERATE", "FILE_MANAGE", "TERMINAL_ACCESS", "HIGH_RISK_APPROVE");

    private TrustedGatewayAuth() {
    }

    public static Optional<Actor> from(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }
        String gatewayRequestId = request.getHeader("X-Gateway-Internal-Request-Id");
        if (gatewayRequestId == null) {
            return Optional.empty();
        }
        if (gatewayRequestId.isBlank() || !REQUEST_ID_PATTERN.matcher(gatewayRequestId).matches()) {
            throw new MalformedContextException();
        }
        String userId = request.getHeader("X-Beiming-Actor-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new MalformedContextException();
        }
        LinkedHashSet<String> roles = csv(request.getHeader("X-Beiming-Actor-Roles"), VALID_ROLES);
        if (roles.isEmpty()) {
            throw new MalformedContextException();
        }
        LinkedHashSet<String> permissions = csv(request.getHeader("X-Beiming-Actor-Permissions"), VALID_PERMISSIONS);
        String minecraftId = blankToNull(request.getHeader("X-Beiming-Actor-Minecraft-Id"));
        String minecraftUuid = blankToNull(request.getHeader("X-Beiming-Actor-Minecraft-Uuid"));
        if (minecraftId != null || minecraftUuid != null) {
            if (minecraftId == null || minecraftUuid == null || !MINECRAFT_UUID_PATTERN.matcher(minecraftUuid).matches()) {
                throw new MalformedContextException();
            }
        }
        return Optional.of(new Actor(userId.trim(), roles, permissions, minecraftId, minecraftUuid, "TRUSTED_GATEWAY_CONTEXT"));
    }

    public static String primaryRole(Set<String> roles) {
        if (roles.contains("OWNER")) {
            return "OWNER";
        }
        if (roles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roles.contains("HELPER")) {
            return "HELPER";
        }
        return "USER";
    }

    private static LinkedHashSet<String> csv(String value, Set<String> allowed) {
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
                throw new MalformedContextException();
            }
            parsed.add(item);
        }
        return parsed;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record Actor(String userId,
                        Set<String> roles,
                        Set<String> permissions,
                        String minecraftId,
                        String minecraftUuid,
                        String authMode) {
        public boolean hasAny(String... candidates) {
            for (String candidate : candidates) {
                if (roles.contains(candidate)) {
                    return true;
                }
            }
            return false;
        }

        public String primaryRole() {
            return TrustedGatewayAuth.primaryRole(roles);
        }
    }

    public static final class MalformedContextException extends RuntimeException {
    }
}
