package com.beiming.profile.domain;

import java.util.List;

public record AuthContext(String token, String userId, Role role, List<String> permissions) {
    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.OWNER;
    }

    public boolean isService() {
        return role == Role.SERVICE;
    }
}
