package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "reseñas")
public class Reseña implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id = UUID.randomUUID().toString();

    private String productoId;
    private String usuarioId;
    private String nombreUsuario;
    private String titulo;
    private String comentario;
    private int puntuacion; // 1..5
    private boolean compraVerificada;
    private java.util.List<String> fotosBase64;
    private int votosUtiles = 0;
    private int votosInutiles = 0;
    private LocalDateTime fecha = LocalDateTime.now();

    // Getters y setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String productoId) {
        this.productoId = productoId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public int getPuntuacion() {
        return puntuacion;
    }

    public void setPuntuacion(int puntuacion) {
        this.puntuacion = puntuacion;
    }

    public boolean isCompraVerificada() {
        return compraVerificada;
    }

    public void setCompraVerificada(boolean compraVerificada) {
        this.compraVerificada = compraVerificada;
    }

    public java.util.List<String> getFotosBase64() {
        return fotosBase64;
    }

    public void setFotosBase64(java.util.List<String> fotosBase64) {
        this.fotosBase64 = fotosBase64;
    }

    public int getVotosUtiles() {
        return votosUtiles;
    }

    public void setVotosUtiles(int votosUtiles) {
        this.votosUtiles = votosUtiles;
    }

    public int getVotosInutiles() {
        return votosInutiles;
    }

    public void setVotosInutiles(int votosInutiles) {
        this.votosInutiles = votosInutiles;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }
}
