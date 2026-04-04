package com.celfinder.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

@Document(collection = "productos")
public class Producto implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    private String nombre;
    private String categoria;
    private float precio;
    private String marca;
    private String descripcion;
    private String vendedorId; 
    private LocalDateTime fechaPublicacion;
    private String estadoVenta; 
    private String imagenBase64;
    private String estado; // Estado del producto: nuevo, usado, etc.
    private java.util.Map<String, String> especificaciones; // Especificaciones dinámicas según categoría
    private int stock = 1;
    private Integer descuento; // Porcentaje de 0 a 100
    private String compradorId; 

    public float getPrecioFinal() {
        if (descuento == null || descuento <= 0) return precio;
        return precio * (1 - (descuento / 100.0f));
    }

    public Integer getDescuento() { return descuento; }
    public void setDescuento(Integer descuento) { this.descuento = descuento; }
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    public String getDescripcion() {
        return descripcion;
    }
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    public String getVendedorId() {
        return vendedorId;
    }
    public void setVendedorId(String vendedorId) {
        this.vendedorId = vendedorId;
    }
    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }
    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }
    public String getEstadoVenta() {
        return estadoVenta;
    }
    public void setEstadoVenta(String estadoVenta) {
        this.estadoVenta = estadoVenta;
    }
    public String getImagenBase64() {
        return imagenBase64;
    }
    public void setImagenBase64(String imagenBase64) {
        this.imagenBase64 = imagenBase64;
    }

    public String getCategoria() {
        return categoria;
    }
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
    public float getPrecio() {
        return precio;
    }
    public void setPrecio(float precio) {
        this.precio = precio;
    }
    public String getMarca() {
        return marca;
    }
    public void setMarca(String marca) {
        this.marca = marca;
    }
    
    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public java.util.Map<String, String> getEspecificaciones() {
        return especificaciones;
    }
    public void setEspecificaciones(java.util.Map<String, String> especificaciones) {
        this.especificaciones = especificaciones;
    }

    public String getCompradorId() {
        return compradorId;
    }

    public void setCompradorId(String compradorId) {
        this.compradorId = compradorId;
    }

    public Producto clonarParaVenta(String compradorId) {
        Producto clon = new Producto();
        clon.setNombre(this.nombre);
        clon.setCategoria(this.categoria);
        clon.setPrecio(this.precio);
        clon.setMarca(this.marca);
        clon.setDescripcion(this.descripcion);
        clon.setVendedorId(this.vendedorId);
        clon.setFechaPublicacion(this.fechaPublicacion);
        clon.setEstadoVenta(com.celfinder.util.EstadoProducto.VENDIDO);
        clon.setImagenBase64(this.imagenBase64);
        clon.setEstado(this.estado);
        clon.setEspecificaciones(this.especificaciones != null ? new java.util.HashMap<>(this.especificaciones) : null);
        clon.setStock(1);
        clon.setDescuento(this.descuento);
        clon.setCompradorId(compradorId);
        return clon;
    }
}