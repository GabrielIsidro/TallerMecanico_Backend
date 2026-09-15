package com.taller.backend.core.dto;
import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
}
