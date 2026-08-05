package com.taller.backend.repository;

import com.taller.backend.model.Cliente;
import com.taller.backend.model.Taller;
import com.taller.backend.model.Vehiculo; // La clase Vehiculo que representa la entidad de tu base de datos
import org.springframework.data.jpa.repository.JpaRepository; // El repositorio que te permite interactuar con la base de datos para la entidad Vehiculo, proporciona métodos CRUD básicos sin necesidad de implementarlos tú mismo
import org.springframework.stereotype.Repository; // Anotación que le dice a Spring que esta interfaz es un repositorio, lo que la hace elegible para ser inyectada en otras partes de tu aplicación
import java.util.Optional; // Importa la clase Optional para manejar valores que pueden ser nulos, como cuando buscas un Vehiculo por patente y no lo encuentras
import java.util.List;

@Repository // Anotación que combina las dos anteriores
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> { // Extiende JpaRepository, lo que le da acceso a métodos CRUD básicos para la entidad Vehiculo, con Long como tipo de ID
    Optional <Vehiculo> findByPatente(String patente); // Método personalizado que busca un Vehiculo por su patente, devuelve un Optional<Vehiculo> que puede contener un Vehiculo o estar vacío si no se encuentra
    List<Vehiculo> findByTallerId(Long tallerId);
    List<Vehiculo> findByTaller(Taller taller);
    void deleteByTallerId(Long tallerId);
}
