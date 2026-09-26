package com.taller.backend.core.config;

import com.taller.backend.modules.talleres.model.RolUsuario;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private com.taller.backend.modules.backoffice.repository.SuperAdminRepository superAdminRepo;

    @Autowired
    private com.taller.backend.modules.backoffice.repository.PlanSuscripcionRepository planRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Verificar si existe el SUPER_ADMIN, si no, crearlo
        String adminEmail = "gabrielisidro8@gmail.com"; // Tu email personal

        if (superAdminRepo.findByEmail(adminEmail).isEmpty()) {
            com.taller.backend.modules.backoffice.model.SuperAdmin superAdmin = new com.taller.backend.modules.backoffice.model.SuperAdmin();
            superAdmin.setEmail(adminEmail);
            superAdmin.setPassword(passwordEncoder.encode("superadmin123"));
            superAdmin.setNombre("Gabriel Isidro");

            superAdminRepo.save(superAdmin);

            System.out.println("=========================================================");
            System.out.println(" SUPER ADMIN CREADO EXITOSAMENTE");
            System.out.println(" Email: " + adminEmail);
            System.out.println(" Password: superadmin123");
            System.out.println("=========================================================");
        }

        // Initialize Plans
        if (planRepository.count() == 0) {
            System.out.println("No se encontraron planes, creando los planes por defecto...");

            planRepository.save(new com.taller.backend.modules.backoffice.model.PlanSuscripcion(null,
                    com.taller.backend.modules.backoffice.model.TipoPlan.BASE,
                    com.taller.backend.modules.backoffice.model.FrecuenciaPlan.MENSUAL, 5000.0, "Plan Base Mensual",
                    true));
            planRepository.save(new com.taller.backend.modules.backoffice.model.PlanSuscripcion(null,
                    com.taller.backend.modules.backoffice.model.TipoPlan.BASE,
                    com.taller.backend.modules.backoffice.model.FrecuenciaPlan.ANUAL, 50000.0, "Plan Base Anual",
                    true));
            planRepository.save(new com.taller.backend.modules.backoffice.model.PlanSuscripcion(null,
                    com.taller.backend.modules.backoffice.model.TipoPlan.PRO,
                    com.taller.backend.modules.backoffice.model.FrecuenciaPlan.MENSUAL, 20000.0, "Plan Pro Mensual",
                    true));
            planRepository.save(new com.taller.backend.modules.backoffice.model.PlanSuscripcion(null,
                    com.taller.backend.modules.backoffice.model.TipoPlan.PRO,
                    com.taller.backend.modules.backoffice.model.FrecuenciaPlan.ANUAL, 200000.0, "Plan Pro Anual",
                    true));

            System.out.println("Planes inicializados correctamente.");
        }
    }
}
