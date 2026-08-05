package com.taller.backend.config;

import com.taller.backend.model.RolUsuario;
import com.taller.backend.model.Usuario;
import com.taller.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private com.taller.backend.repository.PlanSuscripcionRepository planRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Verificar si existe el SUPER_ADMIN, si no, crearlo
        String adminEmail = "gabrielisidro8@gmail.com"; // Tu email personal
        
        if (usuarioRepository.findByEmail(adminEmail).isEmpty()) {
            Usuario superAdmin = new Usuario();
            superAdmin.setEmail(adminEmail);
            superAdmin.setPassword(passwordEncoder.encode("superadmin123")); // Contraseña por defecto
            superAdmin.setNombre("Gabriel");
            superAdmin.setApellido("Garcia");
            superAdmin.setRol(RolUsuario.SUPER_ADMIN);
            // El SuperAdmin NO tiene Taller asignado (taller = null)
            
            usuarioRepository.save(superAdmin);
            
            System.out.println("=========================================================");
            System.out.println(" SUPER ADMIN CREADO EXITOSAMENTE");
            System.out.println(" Email: " + adminEmail);
            System.out.println(" Password: superadmin123");
            System.out.println("=========================================================");
        }

        // Initialize Plans
        if (planRepository.count() == 0) {
            System.out.println("No se encontraron planes, creando los planes por defecto...");

            planRepository.save(new com.taller.backend.model.PlanSuscripcion(null, com.taller.backend.model.TipoPlan.BASE, com.taller.backend.model.FrecuenciaPlan.MENSUAL, 5000.0, "Plan Base Mensual", true));
            planRepository.save(new com.taller.backend.model.PlanSuscripcion(null, com.taller.backend.model.TipoPlan.BASE, com.taller.backend.model.FrecuenciaPlan.ANUAL, 50000.0, "Plan Base Anual", true));
            planRepository.save(new com.taller.backend.model.PlanSuscripcion(null, com.taller.backend.model.TipoPlan.PRO, com.taller.backend.model.FrecuenciaPlan.MENSUAL, 20000.0, "Plan Pro Mensual", true));
            planRepository.save(new com.taller.backend.model.PlanSuscripcion(null, com.taller.backend.model.TipoPlan.PRO, com.taller.backend.model.FrecuenciaPlan.ANUAL, 200000.0, "Plan Pro Anual", true));

            System.out.println("Planes inicializados correctamente.");
        }
    }
}
