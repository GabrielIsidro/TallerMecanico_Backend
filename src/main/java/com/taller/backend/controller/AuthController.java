package com.taller.backend.controller;

import com.taller.backend.dto.AuthRequest;
import com.taller.backend.dto.AuthResponse;
import com.taller.backend.dto.RegistroRequestDTO;
import com.taller.backend.model.RolUsuario;
import com.taller.backend.model.EstadoSuscripcion;
import com.taller.backend.model.Taller;
import com.taller.backend.model.Usuario;
import com.taller.backend.repository.UsuarioRepository;
import com.taller.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    // ---> Inyectamos el repo para poder buscar los datos reales del usuario
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.taller.backend.service.EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        // 0. Verificamos si el usuario existe para dar un mensaje específico
        Usuario usuarioReal = usuarioRepository.findByEmail(authRequest.getEmail()).orElse(null);
        if (usuarioReal == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("La cuenta no existe. Verifica tu correo electrónico.");
        }

        try {
            // 1. El guardia verifica si la contraseña es correcta
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getPassword())
            );
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Contraseña incorrecta.");
        }

        // 2. Si pasó la validación de clave, buscamos sus datos básicos para armar el token
        final UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.getEmail());

        // =========================================================
        // 2.5. LA GUILLOTINA: Verificamos el estado de la suscripción
        // =========================================================
        Taller suTaller = usuarioReal.getTaller();
        
        // Si tiene taller (es decir, NO es el SuperAdmin), revisamos si pagó
        if (suTaller != null) {
            EstadoSuscripcion estado = suTaller.getEstadoSuscripcion();
            
            if (estado == EstadoSuscripcion.SUSPENDIDA || estado == EstadoSuscripcion.VENCIDA) {
                // Le cortamos el rostro con un error 403 (Prohibido)
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Acceso denegado: La cuenta de tu taller se encuentra " + estado + ". Por favor, regulariza tu pago.");
            }
        }
        // =========================================================

        // 3. Si llegó hasta acá (está al día o es SuperAdmin), fabricamos la pulserita VIP
        final String jwt = jwtUtil.generateToken(userDetails);

        // 4. Se la entregamos a React
        return ResponseEntity.ok(new AuthResponse(jwt));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistroRequestDTO request) {
        // 1. Verificamos si el email ya existe
        if (usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("El email ya se encuentra registrado.");
        }

        // 2. Creamos el nuevo usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setEmail(request.getEmail());
        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setNombre(request.getNombre());
        nuevoUsuario.setApellido(request.getApellido());
        nuevoUsuario.setRol(RolUsuario.ADMIN_TALLER); // Rol por defecto, luego el admin puede cambiarlo

        usuarioRepository.save(nuevoUsuario);

        return ResponseEntity.ok("Usuario registrado exitosamente");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "El correo es obligatorio."));
        }

        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            // Retornamos error en lugar de OK para asegurar que el correo insertado es de una cuenta existente
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "No existe ninguna cuenta asociada a este correo electrónico."));
        }

        // Generar contraseña aleatoria de 8 caracteres
        String nuevaClave = java.util.UUID.randomUUID().toString().substring(0, 8);
        usuario.setPassword(passwordEncoder.encode(nuevaClave));
        usuarioRepository.save(usuario);

        // Enviar email
        String asunto = "Recuperación de Contraseña - TuTaller SaaS";
        String texto = "Hola " + usuario.getNombre() + ",\n\n"
                + "Se ha solicitado un blanqueo de contraseña para tu cuenta.\n"
                + "Tu nueva contraseña temporal es: " + nuevaClave + "\n\n"
                + "Te recomendamos iniciar sesión y cambiar esta contraseña desde la sección 'Mi Perfil'.\n\n"
                + "Saludos,\nEl equipo de TuTaller";

        // Usamos CompletableFuture para no trabar la respuesta HTTP
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                emailService.enviarEmail(email, asunto, texto);
            } catch (Exception e) {
                System.err.println("Error enviando email de recuperación: " + e.getMessage());
            }
        });

        return ResponseEntity.ok(java.util.Map.of("mensaje", "¡Contraseña restablecida! Revisa tu bandeja de entrada o spam."));
    }
}