package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "carritos")
public class Carrito {
    @Id
    private String id = UUID.randomUUID().toString();
    private String usuarioId;
    private List<String> productoIds = new ArrayList<>();
    private LocalDateTime fechaCreacion = LocalDateTime.now();
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
	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}
	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

    
    
    
}