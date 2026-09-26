package com.taller.backend.modules.talleres.repository;

import com.taller.backend.modules.talleres.model.Usuario;
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
    
    java.util.List<Usuario> findByTallerIdAndRol(Long tallerId, com.taller.backend.modules.talleres.model.RolUsuario rol);

    @Modifying
    @Transactional
    @Query("DELETE FROM Usuario u WHERE u.taller.id = :tallerId")
    void deleteByTallerId(Long tallerId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO usuarios (email, password, nombre, apellido, rol, taller_id, debe_cambiar_password) VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)", nativeQuery = true)
    void insertAdminTaller(String email, String password, String nombre, String apellido, String rol, Long tallerId, Boolean debeCambiarPassword);
}
