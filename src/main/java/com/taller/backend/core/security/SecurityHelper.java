package com.taller.backend.core.security;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
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
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado: " + email));

        if (usuario.getTaller() == null) {
            throw new UnauthorizedAccessException("El usuario autenticado no tiene un taller asignado");
        }

        return usuario.getTaller();
    }
}
