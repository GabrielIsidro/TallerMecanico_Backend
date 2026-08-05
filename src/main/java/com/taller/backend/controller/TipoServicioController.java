package com.taller.backend.controller;

import com.taller.backend.model.Taller;
import com.taller.backend.model.TipoServicio;
import com.taller.backend.model.Usuario;
import com.taller.backend.repository.TipoServicioRepository;
import com.taller.backend.repository.UsuarioRepository;
import com.taller.backend.service.DataImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/* * @RestController: Le dice a Spring que esta clase es un controlador de una API REST.
 * Automáticamente convierte las respuestas de los métodos a formato JSON.
 */
@RestController
/* * @RequestMapping: Define la ruta base para todos los métodos de esta clase.
 * En este caso, todas las URLs empezarán con "http://localhost:8080/api/servicios"
 */
@RequestMapping("/api/servicios")
/* * @CrossOrigin: Fundamental para conectar con React. 
 * Permite que tu frontend (que corre en otro puerto, ej: 5173) pueda hacerle 
 * peticiones a este backend sin que el navegador lo bloquee por seguridad.
 */
@CrossOrigin(origins = "*")
public class TipoServicioController {

    /* * @Autowired: Inyección de dependencias. Spring crea automáticamente 
     * las instancias del repositorio y el servicio y te las deja listas para usar.
     */
    @Autowired
    private TipoServicioRepository tipoServicioRepository;

    @Autowired
    private DataImportService dataImportService;

    // ---> 1. Inyectamos la base de datos de usuarios
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ---> 2. Función para descubrir de qué taller es la persona que hizo clic
    private Taller getTallerAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return usuario.getTaller();
    }

    // ==========================================
    // ENDPOINT PARA CARGA MASIVA (EXCEL/CSV)
    // ==========================================
    /* * Recibe un archivo binario (MultipartFile) enviado desde el Frontend.
     * La anotación @RequestParam("file") indica que el FormData de React 
     * debe tener un campo llamado "file".
     */
    @PostMapping("/importar")
    public ResponseEntity<String> importarPrecios(@RequestParam("file") MultipartFile file) {
        try {
            // ---> 3. Le pasamos el Taller logueado al servicio de importación
            Taller miTaller = getTallerAutenticado();
            
            // Delegamos la tarea pesada (leer el CSV, parsear datos, guardar en BD) al Service
            // OJO: Vas a tener que agregar este segundo parámetro en DataImportService
            String resultado = dataImportService.importarPrecios(file, miTaller); 
            
            // Si todo sale bien, devolvemos un HTTP 200 (OK) con el mensaje de éxito
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            e.printStackTrace(); // Imprime el error en la consola de Java para debugear
            // Si falla, devolvemos un HTTP 400 (Bad Request) para que React sepa que hubo un error
            return ResponseEntity.badRequest().body("Error al importar: " + e.getMessage());
        }
    }
    
    // ==========================================
    // CRUD BÁSICO DE SERVICIOS
    // ==========================================

    /* * READ: Trae el catálogo completo.
     * Responde a peticiones GET en "/api/servicios".
     */
    @GetMapping
    public List<TipoServicio> listarServicios() {
        // ---> 4. Chau findAll(). Filtramos por el taller logueado
        return tipoServicioRepository.findByTaller(getTallerAutenticado());
    }

    /* * CREATE: Crea un nuevo servicio individual.
     * @RequestBody toma el JSON que manda React y lo convierte en un objeto TipoServicio de Java.
     */
    @PostMapping
    public TipoServicio guardar(@RequestBody TipoServicio tipoServicio){
        // ---> 5. Le estampamos la marca de agua del taller al precio nuevo
        tipoServicio.setTaller(getTallerAutenticado());
        return tipoServicioRepository.save(tipoServicio);
    }

    /* * UPDATE: Actualiza precios o datos de un servicio existente.
     * @PathVariable captura el ID de la URL (Ej: /api/servicios/5 -> id = 5)
     */
    @PutMapping("/{id}")
    public TipoServicio actualizar(@PathVariable Long id, @RequestBody TipoServicio detalles){
        // 1. Buscamos si el servicio existe en la base de datos. Si no, lanzamos un error.
        TipoServicio servicio = tipoServicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con ID: " + id));
        
        // 2. Pisamos los datos viejos con los datos nuevos que vinieron desde React ('detalles')
        servicio.setGrupo(detalles.getGrupo());
        servicio.setDescripcion(detalles.getDescripcion());
        servicio.setPrecioA(detalles.getPrecioA());
        servicio.setPrecioB(detalles.getPrecioB());
        servicio.setPrecioC(detalles.getPrecioC()); 

        // 3. Guardamos el objeto 'servicio' (el cual ya tiene el ID correcto y el Taller asociado).
        return tipoServicioRepository.save(servicio);
    }

    /* * DELETE: Borra un servicio del catálogo.
     */
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id){
        tipoServicioRepository.deleteById(id);
    }

}