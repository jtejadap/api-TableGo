package com.tablego.api.dto;

import com.tablego.api.model.Role;
import com.tablego.api.model.User;

public record UserResponse(Long id, String nombre, String email, Role role, boolean activo) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getNombre(), user.getEmail(), user.getRole(), user.isActivo());
    }
}
