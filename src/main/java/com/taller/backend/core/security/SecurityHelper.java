package com.taller.backend.core.security;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import com.taller.backend.modules.backoffice.repository.TallerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SecurityHelper {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TallerRepository tallerRepository;

    /**
     * Obtiene el Taller asociado al usuario que está realizando la petición actual.
     * Lanza una excepción si el usuario no existe o no tiene un taller asignado.
     */
    @Transactional(readOnly = true)
    public Taller getTallerAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado: " + email));

        if (usuario.getTaller() != null) {
            return usuario.getTaller();
        }

        if (usuario.getTallerId() != null) {
            return tallerRepository.findById(usuario.getTallerId())
                    .orElseThrow(() -> new UnauthorizedAccessException("El taller asignado con ID " + usuario.getTallerId() + " no existe"));
        }

        throw new UnauthorizedAccessException("El usuario autenticado no tiene un taller asignado");
    }
}
