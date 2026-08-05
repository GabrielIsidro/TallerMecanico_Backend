package com.taller.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.taller.backend.model.ItemOrden;
import com.taller.backend.model.OrdenTrabajo;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] generarOrdenPdf(OrdenTrabajo orden) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20);
            Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 12);
            Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            // Title
            Paragraph title = new Paragraph("Orden de Trabajo #" + orden.getId(), fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Info Taller & Cliente
            document.add(new Paragraph("Taller: " + orden.getVehiculo().getCliente().getTaller().getNombre(), fontBold));
            document.add(new Paragraph("Fecha: " + orden.getFechaIngreso().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontNormal));
            document.add(new Paragraph("Cliente: " + orden.getVehiculo().getCliente().getNombreCliente(), fontNormal));
            document.add(new Paragraph("Vehículo: " + orden.getVehiculo().getMarca() + " " + orden.getVehiculo().getModelo() + " - " + orden.getVehiculo().getPatente(), fontNormal));
            document.add(new Paragraph("Estado: " + orden.getEstado().name(), fontNormal));
            
            Paragraph space = new Paragraph(" ");
            space.setSpacingAfter(15);
            document.add(space);

            // Details
            document.add(new Paragraph("Detalles de Trabajo:", fontSubtitle));
            document.add(new Paragraph(orden.getDescripcion() != null ? orden.getDescripcion() : "N/A", fontNormal));
            document.add(space);

            // Table of items
            if (orden.getItems() != null && !orden.getItems().isEmpty()) {
                document.add(new Paragraph("Servicios y Repuestos:", fontSubtitle));
                document.add(new Paragraph(" ", fontNormal));

                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{4f, 1f, 2f, 2f});

                String[] headers = {"Descripción", "Cant", "Precio Unit.", "Subtotal"};
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Paragraph(header, fontBold));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }

                for (ItemOrden item : orden.getItems()) {
                    String desc = item.getTipoServicio() != null ? item.getTipoServicio().getDescripcion() : "Servicio";
                    double unitPrice = item.getSubtotal() != null && item.getCantidad() != null && item.getCantidad() > 0 ? item.getSubtotal() / item.getCantidad() : 0.0;
                    
                    table.addCell(new Paragraph(desc, fontNormal));
                    table.addCell(new Paragraph(String.valueOf(item.getCantidad()), fontNormal));
                    table.addCell(new Paragraph("$" + String.format("%.2f", unitPrice), fontNormal));
                    table.addCell(new Paragraph("$" + (item.getSubtotal() != null ? item.getSubtotal() : "0.0"), fontNormal));
                }
                document.add(table);
            }

            document.add(space);
            Paragraph total = new Paragraph("Costo Total Estimado: $" + orden.getCostoTotal(), fontSubtitle);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }
}
