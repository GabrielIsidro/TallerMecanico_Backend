package com.taller.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication; // El traductor mágico que arranca tu aplicación Spring Boot
import org.springframework.boot.autoconfigure.SpringBootApplication; // Anotación que le dice a Spring Boot que esta es la clase principal de tu aplicación
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.taller.backend.model.RolUsuario;
import com.taller.backend.model.Usuario;
import com.taller.backend.repository.UsuarioRepository;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication // Anotación que combina las dos anteriores
@EnableScheduling
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	// Este código se ejecuta automáticamente cada vez que arranca Spring Boot
    @Bean
    CommandLineRunner initDatabase(UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            // Verificamos si tu usuario ya existe para no crearlo dos veces
            if (usuarioRepo.findByEmail("admin@tutaller.com").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setNombre("Super");
                admin.setApellido("Admin");
                admin.setEmail("admin@tutaller.com");
                // Encriptamos la contraseña "123456" antes de guardarla
                admin.setPassword(passwordEncoder.encode("123456")); 
                admin.setRol(RolUsuario.SUPER_ADMIN);
                // No le ponemos Taller_ID porque vos sos el dueño del SaaS
                
                usuarioRepo.save(admin);
                System.out.println("¡Usuario SUPER_ADMIN creado con éxito! (admin@tutaller.com / 123456)");
            }
        };
    }

}
