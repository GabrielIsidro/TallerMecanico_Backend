package com.taller.backend.security;

import com.taller.backend.model.Taller;
import com.taller.backend.model.Usuario;
import com.taller.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityHelper {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Obtiene el Taller asociado al usuario que está realizando la petición actual.
     * Lanza una excepción si el usuario no existe o no tiene un taller asignado.
     */
    public Taller getTallerAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en BD"));

        if (usuario.getTaller() == null) {
            throw new RuntimeException("El usuario no tiene un taller asignado");
        }

        return usuario.getTaller();
    }
}
