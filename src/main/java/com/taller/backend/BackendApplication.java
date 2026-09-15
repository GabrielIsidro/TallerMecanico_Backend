package com.taller.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.taller.backend.modules.backoffice.model.SuperAdmin;
import com.taller.backend.modules.backoffice.repository.SuperAdminRepository;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(SuperAdminRepository superAdminRepo, PasswordEncoder passwordEncoder) {
        return args -> {
            if (superAdminRepo.findByEmail("admin@tutaller.com").isEmpty()) {
                SuperAdmin admin = new SuperAdmin();
                admin.setNombre("Super Admin");
                admin.setEmail("admin@tutaller.com");
                admin.setPassword(passwordEncoder.encode("123456"));

                superAdminRepo.save(admin);
                System.out.println("¡Usuario SUPER_ADMIN creado con éxito! (admin@tutaller.com / 123456)");
            }
        };
    }
}
