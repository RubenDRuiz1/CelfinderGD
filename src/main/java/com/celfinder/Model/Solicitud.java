package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "solicitudes")
public class Solicitud {

    @Id
    private String id;
    private String tipoSolicitud; // e.g., "compra" or "vendedor"

    private String usuarioId; 
    private String estado; 
    private LocalDateTime fechaSolicitud;
    private String productoId; // Replaces celularId for "compra" type
    private String nombreProducto; // Replaces nombreCelular for "compra" type
    private String nombreComprador; // For "compra" type
    private String vendedorId; // For "compra" type
    private String direccionComprador; // For "compra" type
    private String correoComprador; // For "compra" type
    private String numeroContactoComprador; // For "compra" type
    private String descripcionVendedor; // For "compra" type
    private String nombreCompleto; // For "vendedor" type
    private String correo; // For "vendedor" type
    private String telefono; // For "vendedor" type
    private String direccion; // For "vendedor" type
    private String motivo; // For "vendedor" type
    private String comentarioAdmin; // For "vendedor" type
    private LocalDateTime fechaRespuesta; // For "vendedor" type
    private String estadoSeguimiento; // PENDIENTE, PREPARANDO, ENVIADO, ENTREGADO

    public Solicitud() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTipoSolicitud() {
        return tipoSolicitud;
    }

    public void setTipoSolicitud(String tipoSolicitud) {
        this.tipoSolicitud = tipoSolicitud;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(String usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String productoId) {
        this.productoId = productoId;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getNombreComprador() {
        return nombreComprador;
    }

    public void setNombreComprador(String nombreComprador) {
        this.nombreComprador = nombreComprador;
    }

    public String getVendedorId() {
        return vendedorId;
    }

    public void setVendedorId(String vendedorId) {
        this.vendedorId = vendedorId;
    }

    public String getDireccionComprador() {
        return direccionComprador;
    }

    public void setDireccionComprador(String direccionComprador) {
        this.direccionComprador = direccionComprador;
    }

    public String getCorreoComprador() {
        return correoComprador;
    }

    public void setCorreoComprador(String correoComprador) {
        this.correoComprador = correoComprador;
    }

    public String getNumeroContactoComprador() {
        return numeroContactoComprador;
    }

    public void setNumeroContactoComprador(String numeroContactoComprador) {
        this.numeroContactoComprador = numeroContactoComprador;
    }

    public String getDescripcionVendedor() {
        return descripcionVendedor;
    }

    public void setDescripcionVendedor(String descripcionVendedor) {
        this.descripcionVendedor = descripcionVendedor;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getComentarioAdmin() {
        return comentarioAdmin;
    }

    public void setComentarioAdmin(String comentarioAdmin) {
        this.comentarioAdmin = comentarioAdmin;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(LocalDateTime fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    public String getEstadoSeguimiento() {
        return estadoSeguimiento;
    }

    public void setEstadoSeguimiento(String estadoSeguimiento) {
        this.estadoSeguimiento = estadoSeguimiento;
    }

    public void setFecha(LocalDateTime now) {
        this.fechaSolicitud = now;
    }
}