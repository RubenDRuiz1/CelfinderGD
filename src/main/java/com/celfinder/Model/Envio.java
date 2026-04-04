package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "envios")
public class Envio {

    @Id
    private String id = UUID.randomUUID().toString();

    private String solicitudId;        // solicitud de compra
    private String vendedorId;
    private String compradorId;
    private String codigoSeguimiento;  // aleatorio
    private String estado;             // preparando, enviado, en camino, entregado
    private LocalDate fechaEstimada;
    private LocalDateTime fechaActualizacion = LocalDateTime.now();
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
	public String getVendedorId() {
		return vendedorId;
	}
	public void setVendedorId(String vendedorId) {
		this.vendedorId = vendedorId;
	}
	public String getCompradorId() {
		return compradorId;
	}
	public void setCompradorId(String compradorId) {
		this.compradorId = compradorId;
	}
	public String getCodigoSeguimiento() {
		return codigoSeguimiento;
	}
	public void setCodigoSeguimiento(String codigoSeguimiento) {
		this.codigoSeguimiento = codigoSeguimiento;
	}
	public String getEstado() {
		return estado;
	}
	public void setEstado(String estado) {
		this.estado = estado;
	}
	public LocalDate getFechaEstimada() {
		return fechaEstimada;
	}
	public void setFechaEstimada(LocalDate fechaEstimada) {
		this.fechaEstimada = fechaEstimada;
	}
	public LocalDateTime getFechaActualizacion() {
		return fechaActualizacion;
	}
	public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
		this.fechaActualizacion = fechaActualizacion;
	}

    
}