package com.celfinder.Procesos;

import com.celfinder.Model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Fachada delgada de ventas.
 *
 * <p>Esta clase ya NO contiene lógica de negocio propia. Delega cada llamada
 * al servicio especializado correspondiente, garantizando compatibilidad
 * total con los controladores existentes sin modificarlos.</p>
 *
 * <pre>
 *  Lógica real → servicio especializado
 *  ─────────────────────────────────────────────────────
 *  Consultas de producto   → ProductQueryService
 *  Acciones sobre producto → ProductActionService
 *  Compras / solicitudes   → PurchaseService
 *  Comparaciones           → ComparisonEngine
 *  Reseñas                 → ReviewManager
 * </pre>
 */
@Service
public class VentaService {

    // ---------------------------------------------------------------
    // Servicios especializados inyectados
    // ---------------------------------------------------------------

    private final ProductQueryService  productQueryService;
    private final ProductActionService productActionService;
    private final PurchaseService      purchaseService;
    private final ComparisonEngine     comparisonEngine;
    private final ReviewManager        reviewManager;

    @Autowired
    public VentaService(ProductQueryService  productQueryService,
                        ProductActionService productActionService,
                        PurchaseService      purchaseService,
                        ComparisonEngine     comparisonEngine,
                        ReviewManager        reviewManager) {
        this.productQueryService  = productQueryService;
        this.productActionService = productActionService;
        this.purchaseService      = purchaseService;
        this.comparisonEngine     = comparisonEngine;
        this.reviewManager        = reviewManager;
    }

    // ===================================================================
    // DELEGACIONES → ProductQueryService
    // ===================================================================

    public Producto obtenerProductoPorId(String id) {
        return productQueryService.obtenerProductoPorId(id);
    }

    public List<Producto> obtenerProductosEnVenta() {
        return productQueryService.obtenerProductosEnVenta();
    }

    public List<Producto> obtenerProductosPorVendedor(String vendedorId) {
        return productQueryService.obtenerProductosPorVendedor(vendedorId);
    }

    public List<Producto> buscarProductos(String nombre, String estado,
                                          Float precioMin, Float precioMax) {
        return productQueryService.buscarProductos(nombre, estado, precioMin, precioMax);
    }

    public List<Producto> buscarProductosExcluyendoId(String nombre, String estado,
                                                       Float precioMin, Float precioMax,
                                                       String idExcluir) {
        return productQueryService.buscarProductosExcluyendoId(nombre, estado, precioMin, precioMax, idExcluir);
    }

    public List<Producto> obtenerProductosSimilares(String id, String nombre) {
        return productQueryService.obtenerProductosSimilares(id, nombre);
    }

    public List<Producto> paginarProductos(List<Producto> todos, int page, int itemsPerPage) {
        return productQueryService.paginarProductos(todos, page, itemsPerPage);
    }

    // ===================================================================
    // DELEGACIONES → ProductActionService
    // ===================================================================

    public void publicarProducto(Producto producto) {
        productActionService.publicarProducto(producto);
    }

    public void prepararYPublicarProducto(Producto producto, Usuario vendedor,
                                          Map<String, String> allParams) {
        productActionService.prepararYPublicarProducto(producto, vendedor, allParams);
    }

    public void actualizarProducto(Producto producto) {
        productActionService.actualizarProducto(producto);
    }

    public Producto prepararYActualizarProducto(String id, Producto datosNuevos,
                                                Usuario usuario, String imagenBase64) {
        return productActionService.prepararYActualizarProducto(id, datosNuevos, usuario, imagenBase64);
    }

    public void eliminarProducto(String id) {
        productActionService.eliminarProducto(id);
    }

    public void registrarVisualizacion(String usuarioId, String productoId) {
        productActionService.registrarVisualizacion(usuarioId, productoId);
    }

    public boolean productoEstaEnCarrito(String productoId) {
        return productActionService.productoEstaEnCarrito(productoId);
    }

    public boolean productoTieneSolicitudPendiente(String productoId) {
        return productActionService.productoTieneSolicitudPendiente(productoId);
    }

    public boolean productoEstaDisponible(String productoId) {
        return productActionService.productoEstaDisponible(productoId);
    }

    // ===================================================================
    // DELEGACIONES → PurchaseService
    // ===================================================================

    public Solicitud obtenerSolicitudPorId(String id) {
        return purchaseService.obtenerSolicitudPorId(id);
    }

    public void validarCompra(String usuarioId, String productoId) {
        purchaseService.validarCompra(usuarioId, productoId);
    }

    public void crearSolicitudCompra(Solicitud solicitud) {
        purchaseService.crearSolicitudCompra(solicitud);
    }

    public Solicitud crearSolicitudDesdeCompra(Usuario comprador, String productoId,
                                               String direccion, String correo, String contacto) {
        return purchaseService.crearSolicitudDesdeCompra(comprador, productoId, direccion, correo, contacto);
    }

    public void gestionarSolicitudCompra(String solicitudId, String accion, String descripcionVendedor) {
        purchaseService.gestionarSolicitudCompra(solicitudId, accion, descripcionVendedor);
    }

    public List<Solicitud> obtenerSolicitudesPorVendedor(String vendedorId) {
        return purchaseService.obtenerSolicitudesPorVendedor(vendedorId);
    }

    public List<Solicitud> obtenerHistorialCompras(String compradorId) {
        return purchaseService.obtenerHistorialCompras(compradorId);
    }

    public List<Solicitud> obtenerHistorialVentas(String vendedorId) {
        return purchaseService.obtenerHistorialVentas(vendedorId);
    }

    public List<Map<String, Object>> obtenerHistorialComprasCompleto(String compradorId) {
        return purchaseService.obtenerHistorialComprasCompleto(compradorId);
    }

    public List<Map<String, Object>> obtenerHistorialVentasCompleto(String vendedorId) {
        return purchaseService.obtenerHistorialVentasCompleto(vendedorId);
    }

    public Map<String, Object> procesarCompraConTarjeta(String productoId, String compradorId,
                                                        String email, String cardNumber,
                                                        String cardHolder, String expiryMonth,
                                                        String expiryYear, String cvv,
                                                        String ipAddress) {
        return purchaseService.procesarCompraConTarjeta(
                productoId, compradorId, email, cardNumber,
                cardHolder, expiryMonth, expiryYear, cvv, ipAddress);
    }

    // ===================================================================
    // DELEGACIONES → ComparisonEngine
    // ===================================================================

    public List<String> compararConOtroProducto(String id, String idOtroProducto) {
        return comparisonEngine.compararConOtroProducto(id, idOtroProducto);
    }

    public List<String> compararConMedia(String id) {
        return comparisonEngine.compararConMedia(id);
    }

    // ===================================================================
    // DELEGACIONES → ReviewManager
    // ===================================================================

    public List<Reseña> obtenerReseñasPorProducto(String productoId) {
        return reviewManager.obtenerReseñasPorProducto(productoId);
    }

    public void guardarReseña(Reseña reseña) {
        reviewManager.guardarReseña(reseña);
    }

    public boolean usuarioComproProducto(String usuarioId, String productoId) {
        return reviewManager.usuarioComproProducto(usuarioId, productoId);
    }

    public Reseña crearYGuardarReseña(String productoId, Usuario usuario,
                                      String nombreUsuario, String titulo,
                                      String comentario, int puntuacion) {
        return reviewManager.crearYGuardarReseña(productoId, usuario, nombreUsuario, titulo, comentario, puntuacion);
    }

    public String votarReseña(String reseñaId, boolean util) {
        return reviewManager.votarReseña(reseñaId, util);
    }
}