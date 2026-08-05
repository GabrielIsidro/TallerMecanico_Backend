package com.taller.backend.dto;
import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
}