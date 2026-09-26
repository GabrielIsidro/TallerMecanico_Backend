package com.taller.backend.modules.talleres;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.Cliente;
import com.taller.backend.modules.talleres.model.Vehiculo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JsonTest
class ClienteSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Debe serializar Cliente a JSON sin error de proxy Hibernate ni recursión")
    void debeSerializarClienteSinErrorProxy() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombreCliente("Juan Pérez");
        cliente.setEmail("juan@gmail.com");
        cliente.setTallerId(10L);

        Taller taller = new Taller();
        taller.setId(10L);
        taller.setNombre("Taller Central");
        cliente.setTaller(taller);

        Vehiculo v = new Vehiculo();
        v.setId(5L);
        v.setPatente("AA123BB");
        v.setMarca("Toyota");
        v.setModelo("Corolla");
        v.setCliente(cliente);

        cliente.setVehiculos(new ArrayList<>());
        cliente.getVehiculos().add(v);

        assertDoesNotThrow(() -> {
            String json = objectMapper.writeValueAsString(cliente);
            assertNotNull(json);
        });
    }
}
