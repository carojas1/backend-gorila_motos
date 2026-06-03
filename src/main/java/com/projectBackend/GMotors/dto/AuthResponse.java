package com.projectBackend.GMotors.dto;

import com.projectBackend.GMotors.model.Usuario;

public class AuthResponse {
    public Usuario usuario;
    public String token;

    public AuthResponse(Usuario usuario, String token) {
        this.usuario = usuario;
        this.token = token;
    }
}

