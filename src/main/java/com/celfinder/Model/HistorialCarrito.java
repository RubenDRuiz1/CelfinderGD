package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Document(collection = "historial_carritos")
public class HistorialCarrito {

    @Id
    private String id = UUID.randomUUID().toString();

    private String usuarioId;
    private List<String> productoIds;
    private LocalDateTime fecha = LocalDateTime.now();

    // Getters y setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public List<String> getProductoIds() {
        return productoIds;
    }

    public void setProductoIds(List<String> productoIds) {
        this.productoIds = productoIds;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}