package com.celfinder.Procesos;

import com.celfinder.Model.CarritoItem;
import com.celfinder.Model.Combo;
import com.celfinder.Model.Producto;
import com.celfinder.util.EstadoProducto;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de Carrito de Compras.
 * Guardado en la Sesión del Usuario (@SessionScope) para persistencia mientras navega.
 */
@Service
@SessionScope
public class CarritoService implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<CarritoItem> items = new ArrayList<>();

    private final ProductQueryService productQueryService;
    private final ComboService comboService;

    public CarritoService(ProductQueryService productQueryService, ComboService comboService) {
        this.productQueryService = productQueryService;
        this.comboService = comboService;
    }

    /**
     * Agrega un producto al carrito previa validación.
     */
    public void agregarAlCarrito(String usuarioId, String productoId, int cantidad) {
        Producto producto = productQueryService.obtenerProductoPorId(productoId);

        if (producto == null) {
            throw new IllegalArgumentException("El producto no existe.");
        }

        // Validación: Estado disponible
        if (!"disponible".equals(producto.getEstadoVenta())) {
            throw new IllegalStateException("El producto " + producto.getNombre() + " ya no está disponible.");
        }

        // Validación: No es propio
        if (producto.getVendedorId().equals(usuarioId)) {
            throw new IllegalStateException("No puedes agregar tu propio producto al carrito.");
        }

        // Verificar si ya está en el carrito para incrementar cantidad
        for (CarritoItem item : items) {
            if (item.getProductoId().equals(productoId)) {
                item.setCantidad(item.getCantidad() + cantidad);
                return;
            }
        }

        CarritoItem nuevoItem = new CarritoItem(producto);
        nuevoItem.setCantidad(cantidad);
        items.add(nuevoItem);
    }

    /**
     * Agrega un combo al carrito.
     */
    public void agregarComboAlCarrito(String usuarioId, String comboId) {
        Combo combo = comboService.obtenerPorId(comboId);
        if (combo == null) {
            throw new IllegalArgumentException("El combo no existe.");
        }

        // Validar que todos los productos estén disponibles
        for (String pId : combo.getProductoIds()) {
            Producto p = productQueryService.obtenerProductoPorId(pId);
            if (p == null || !EstadoProducto.DISPONIBLE.equals(p.getEstadoVenta())) {
                throw new IllegalStateException("Uno de los productos del combo ya no está disponible.");
            }
            if (p.getVendedorId().equals(usuarioId)) {
                throw new IllegalStateException("No puedes comprar combos que incluyan tus propios productos.");
            }
        }

        // Verificar si ya está en el carrito
        boolean existe = items.stream().anyMatch(i -> i.getProductoId().equals(comboId));
        if (existe) {
            throw new IllegalStateException("El combo ya está en el carrito.");
        }

        items.add(new CarritoItem(combo));
    }

    /**
     * Elimina un producto del carrito por su ID.
     */
    public void eliminarDelCarrito(String productoId) {
        items.removeIf(item -> item.getProductoId().equals(productoId));
    }

    /**
     * Obtiene todos los ítems actuales.
     */
    public List<CarritoItem> obtenerItems() {
        return items;
    }

    /**
     * Calcula el monto total del carrito.
     */
    public double obtenerTotal() {
        return items.stream().mapToDouble(CarritoItem::getPrecioTotal).sum();
    }

    /**
     * Limpia el carrito por completo.
     */
    public void limpiarCarrito() {
        items.clear();
    }

    /**
     * Devuelve el número total de unidades en el carrito.
     */
    public int getContador() {
        return items.stream().mapToInt(CarritoItem::getCantidad).sum();
    }
}