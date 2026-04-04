package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "mensajes")
public class Mensaje {
    @Id
    private String id = UUID.randomUUID().toString();

    private String solicitudId;      // chat por solicitud
    private String emisorId;         // usuario que envía
    private String receptorId;       // usuario que recibe
    private String contenido;        // texto plano
    private LocalDateTime fecha = LocalDateTime.now();
    private boolean leido = false;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getSolicitudId() {
		return solicitudId;
	}
	public void setSolicitudId(String solicitudId) {
		this.solicitudId = solicitudId;
	}
	public String getEmisorId() {
		return emisorId;
	}
	public void setEmisorId(String emisorId) {
		this.emisorId = emisorId;
	}
	public String getReceptorId() {
		return receptorId;
	}
	public void setReceptorId(String receptorId) {
		this.receptorId = receptorId;
	}
	public String getContenido() {
		return contenido;
	}
	public void setContenido(String contenido) {
		this.contenido = contenido;
	}
	public LocalDateTime getFecha() {
		return fecha;
	}
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
	public boolean isLeido() {
		return leido;
	}
	public void setLeido(boolean leido) {
		this.leido = leido;
	}

  
}