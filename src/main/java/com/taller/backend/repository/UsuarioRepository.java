package com.taller.backend.repository;

import com.taller.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // Spring Boot es tan inteligente que al leer "findByEmail", 
    // arma la consulta SQL automáticamente: SELECT * FROM usuarios WHERE email = ?
    Optional<Usuario> findByEmail(String email);
    
    java.util.List<Usuario> findByTallerId(Long tallerId);
    
    java.util.List<Usuario> findByTallerIdAndRol(Long tallerId, com.taller.backend.model.RolUsuario rol);

    @Modifying
    @Transactional
    @Query("DELETE FROM Usuario u WHERE u.taller.id = :tallerId")
    void deleteByTallerId(Long tallerId);
}