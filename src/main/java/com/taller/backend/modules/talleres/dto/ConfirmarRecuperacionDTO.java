package com.taller.backend.modules.talleres.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarRecuperacionDTO {

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El formato de correo no es válido.")
    private String email;

    @NotBlank(message = "El código de verificación es obligatorio.")
    @Size(min = 6, max = 6, message = "El código de verificación debe tener exactamente 6 dígitos.")
    private String codigo;

    @NotBlank(message = "La nueva contraseña es obligatoria.")
    @Size(min = 6, message = "La nueva contraseña debe tener al menos 6 caracteres.")
    private String nuevaPassword;

    @NotBlank(message = "La confirmación de contraseña es obligatoria.")
    private String confirmarPassword;
}
