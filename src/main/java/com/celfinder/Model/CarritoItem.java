package com.celfinder.Model;

import java.io.Serializable;

/**
 * POJO para representar un ítem en el carrito de compras (Sesión).
 */
public class CarritoItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String productoId;
    private String nombre;
    private float precio;
    private String vendedorId;
    private String imagenBase64;
    private boolean combo;
    private java.util.List<String> productoIds;
    private int cantidad = 1;

    public CarritoItem() {}

    public CarritoItem(Producto producto) {
        this.productoId = producto.getId();
        this.nombre = producto.getNombre();
        this.precio = producto.getPrecioFinal();
        this.vendedorId = producto.getVendedorId();
        this.imagenBase64 = producto.getImagenBase64();
        this.combo = false;
    }

    public CarritoItem(Combo combo) {
        this.productoId = combo.getId();
        this.nombre = combo.getNombre() + " (Combo)";
        this.precio = (float) combo.getPrecioCombo();
        this.vendedorId = "admin_combo"; // Combos don't have a single seller necessarily
        this.combo = true;
        this.productoIds = combo.getProductoIds();
    }

    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String productoId) {
        this.productoId = productoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public float getPrecio() {
        return precio;
    }

    public void setPrecio(float precio) {
        this.precio = precio;
    }

    public String getVendedorId() {
        return vendedorId;
    }

    public void setVendedorId(String vendedorId) {
        this.vendedorId = vendedorId;
    }

    public String getImagenBase64() {
        return imagenBase64;
    }

    public void setImagenBase64(String imagenBase64) {
        this.imagenBase64 = imagenBase64;
    }

    public boolean isCombo() {
        return combo;
    }

    public void setCombo(boolean combo) {
        this.combo = combo;
    }

    public java.util.List<String> getProductoIds() {
        return productoIds;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    // --- MÉTODOS DE CONVENIENCIA PARA THYMELEAF ---
    public String getId() {
        return productoId;
    }

    public float getPrecioTotal() {
        return precio * cantidad;
    }
}
