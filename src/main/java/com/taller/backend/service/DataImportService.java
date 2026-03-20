package com.taller.backend.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.taller.backend.model.TipoServicio;
import com.taller.backend.repository.TipoServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List; // Descomenté esta importación porque la vamos a usar

/*
 * Servicio para importar datos masivos desde un archivo CSV. 
 * Se utiliza para mantener actualizada la lista de precios del taller.
 */
@Service
public class DataImportService {

    @Autowired
    private TipoServicioRepository tipoServicioRepository;

    /*
     * El formato esperado del CSV es:
     * Grupo,Descripcion,PrecioA,PrecioB,PrecioC
     * Ejemplo:
     * FRENOS,Revisión de frenos,$1000,$1200,$1500
     */
    public String importarPrecios(MultipartFile file) throws IOException, CsvValidationException {
        
        // 1. Validación básica para no procesar la nada misma
        if (file.isEmpty()){
            throw new RuntimeException("El archivo está vacío. Por favor, selecciona un CSV válido.");
        }

        // 2. Try-with-resources: Abre los flujos de lectura y se asegura de cerrarlos al terminar
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {
    
            // Leer y descartar la primera línea (el encabezado de las columnas)
            csvReader.readNext();
            
            String[] fila;
            int ignorados = 0;
            
            // OPTIMIZACIÓN: Creamos una lista para juntar todos los servicios en memoria
            List<TipoServicio> serviciosNuevos = new ArrayList<>();

            // 3. Itera fila por fila desde la línea 2
            while ((fila = csvReader.readNext()) != null) {

                // Validación de integridad: Si la fila no tiene las 5 columnas, la saltamos
                if (fila.length < 5) {
                    System.out.println("Salteando línea inválida o incompleta: " + Arrays.toString(fila));
                    ignorados++;
                    continue; 
                }

                TipoServicio servicio = new TipoServicio();

                // Limpieza y asignación de textos (evitando NullPointerExceptions)
                servicio.setGrupo(fila[0] != null ? fila[0].toUpperCase().trim() : "GENERAL");
                servicio.setDescripcion(fila[1] != null ? fila[1].trim() : "Sin descripción");

                // Parseo inteligente de precios usando nuestro método auxiliar
                servicio.setPrecioA(parsearPrecio(fila[2]));
                servicio.setPrecioB(parsearPrecio(fila[3]));
                servicio.setPrecioC(parsearPrecio(fila[4]));

                // Agregamos a la lista en lugar de guardar directamente en la BD
                serviciosNuevos.add(servicio);
            }
            
            // 4. GUARDADO MASIVO (Bulk Insert)
            // Esto es muchísimo más eficiente y rápido para el servidor y la base de datos
            if (!serviciosNuevos.isEmpty()) {
                tipoServicioRepository.saveAll(serviciosNuevos);
            }
            
            // Retornamos un resumen completo de la operación
            return "¡Éxito! Se cargaron " + serviciosNuevos.size() + " servicios correctamente. " +
                   (ignorados > 0 ? ("(Se ignoraron " + ignorados + " líneas con formato incorrecto).") : "");
        }
    }

    /*
     * Método auxiliar para limpiar "basura" del Excel y convertir a Double.
     * Ejemplo de entrada: "$ 1.500,50" -> Salida: 1500.50
     */
    private Double parsearPrecio(String valor) {
        // Si la celda está vacía, asumimos 0.0
        if (valor == null || valor.trim().isEmpty()) {
            return 0.0;
        }

        try {
            // 1. Quitamos el signo peso
            // 2. Quitamos los puntos de separador de miles
            // 3. Cambiamos la coma decimal por punto (formato estándar de Java)
            String limpio = valor.replace("$","").replace(".","").replace(",", ".").trim();
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            // Si alguien escribió texto en vez de números en esa celda, no frenamos todo, guardamos como 0
            return 0.0;
        }
    }
}