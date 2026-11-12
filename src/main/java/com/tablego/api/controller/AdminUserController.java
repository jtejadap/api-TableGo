package com.tablego.api.controller;

import com.tablego.api.dto.CreateUserRequest;
import com.tablego.api.dto.UserResponse;
import com.tablego.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Gestion de usuarios (crear recepcionistas). Solo ADMINISTRADOR. */
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createRecepcionist(@Valid @RequestBody CreateUserRequest request) {
        var user = userService.createRecepcionist(request.nombre(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.findAll().stream().map(UserResponse::from).toList();
    }
}
