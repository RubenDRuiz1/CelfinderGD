package com.celfinder.Procesos;

import com.celfinder.Model.*;
import com.celfinder.util.EstadoProducto;
import com.celfinder.util.EstadoSolicitud;
import com.celfinder.util.TipoSolicitud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio de compras.
 * Responsabilidad única: validar, crear y gestionar solicitudes de compra,
 * así como los historiales de compra y venta.
 */
@Service
public class PurchaseService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseService.class);

    private final MongoTemplate mongoTemplate;
    private final ProductQueryService productQueryService;
    private final ProductActionService productActionService;
    private final UsuarioService usuarioService;

    @Autowired
    public PurchaseService(MongoTemplate mongoTemplate,
                           ProductQueryService productQueryService,
                           ProductActionService productActionService,
                           UsuarioService usuarioService) {
        this.mongoTemplate = mongoTemplate;
        this.productQueryService = productQueryService;
        this.productActionService = productActionService;
        this.usuarioService = usuarioService;
    }

    // ---------------------------------------------------------------
    // Consultas de solicitudes
    // ---------------------------------------------------------------

    public Solicitud obtenerSolicitudPorId(String id) {
        return mongoTemplate.findById(id, Solicitud.class, "solicitudes");
    }

    public List<Solicitud> obtenerSolicitudesPorVendedor(String vendedorId) {
        return mongoTemplate.find(
                new Query(Criteria.where("vendedorId").is(vendedorId)
                        .and("estado").in(EstadoSolicitud.PENDIENTE, EstadoSolicitud.AUTORIZADA)
                        .and("tipoSolicitud").is(TipoSolicitud.COMPRA)),
                Solicitud.class, "solicitudes");
    }

    public List<Solicitud> obtenerHistorialCompras(String compradorId) {
        return mongoTemplate.find(
                new Query(Criteria.where("usuarioId").is(compradorId)
                        .and("tipoSolicitud").is(TipoSolicitud.COMPRA)),
                Solicitud.class, "solicitudes");
    }

    public List<Solicitud> obtenerHistorialVentas(String vendedorId) {
        return mongoTemplate.find(
                new Query(Criteria.where("vendedorId").is(vendedorId)
                        .and("tipoSolicitud").is(TipoSolicitud.COMPRA)),
                Solicitud.class, "solicitudes");
    }

    // ---------------------------------------------------------------
    // Historial enriquecido (a prueba de null)
    // ---------------------------------------------------------------

    public List<Map<String, Object>> obtenerHistorialComprasCompleto(String compradorId) {
        Query query = new Query(Criteria.where("usuarioId").is(compradorId)
                .and("tipoSolicitud").is("compra"));
        query.with(Sort.by(Sort.Direction.DESC, "fechaSolicitud"));

        List<Solicitud> solicitudes = mongoTemplate.find(query, Solicitud.class, "solicitudes");
        List<Map<String, Object>> historial = new ArrayList<>();

        for (Solicitud s : solicitudes) {
            Map<String, Object> item = new HashMap<>();
            item.put("solicitud", s);

            if (s.getProductoId() != null && !s.getProductoId().trim().isEmpty()) {
                Producto producto = productQueryService.obtenerProductoPorId(s.getProductoId());
                if (producto != null) {
                    item.put("producto", producto);
                    item.put("imagenBase64", producto.getImagenBase64());
                    item.put("precio", producto.getPrecio());
                } else {
                    item.put("productoEliminado", true);
                    item.put("nombreProducto", s.getNombreProducto() + " (producto eliminado)");
                }
            } else {
                item.put("productoEliminado", true);
                item.put("nombreProducto", s.getNombreProducto() + " (sin ID)");
            }

            String nombreVendedor = "Vendedor desconocido";
            String vendedorId = s.getVendedorId();
            if (vendedorId != null && !vendedorId.trim().isEmpty()) {
                Usuario vendedor = usuarioService.obtenerUsuarioPorId(vendedorId);
                if (vendedor != null) {
                    nombreVendedor = vendedor.getNombreUsuario();
                }
            }
            item.put("nombreVendedor", nombreVendedor);

            historial.add(item);
        }
        return historial;
    }

    public List<Map<String, Object>> obtenerHistorialVentasCompleto(String vendedorId) {
        Query query = new Query(Criteria.where("vendedorId").is(vendedorId)
                .and("tipoSolicitud").is("compra"));
        query.with(Sort.by(Sort.Direction.DESC, "fechaSolicitud"));

        List<Solicitud> solicitudes = mongoTemplate.find(query, Solicitud.class, "solicitudes");
        List<Map<String, Object>> historial = new ArrayList<>();

        for (Solicitud s : solicitudes) {
            Map<String, Object> item = new HashMap<>();
            item.put("solicitud", s);

            if (s.getProductoId() != null && !s.getProductoId().trim().isEmpty()) {
                Producto producto = productQueryService.obtenerProductoPorId(s.getProductoId());
                if (producto != null) {
                    item.put("producto", producto);
                    item.put("imagenBase64", producto.getImagenBase64());
                    item.put("precio", producto.getPrecio());
                }
            }

            item.put("nombreComprador",    s.getNombreComprador()    != null ? s.getNombreComprador()    : "Anónimo");
            item.put("correoComprador",    s.getCorreoComprador()    != null ? s.getCorreoComprador()    : "No disponible");
            item.put("direccionComprador", s.getDireccionComprador() != null ? s.getDireccionComprador() : "No disponible");

            historial.add(item);
        }
        return historial;
    }

    // ---------------------------------------------------------------
    // Validación de compra
    // ---------------------------------------------------------------

    public void validarCompra(String usuarioId, String productoId) {
        Producto p = productQueryService.obtenerProductoPorId(productoId);
        if (p == null) throw new IllegalStateException("Producto no existe.");
        if (!EstadoProducto.DISPONIBLE.equals(p.getEstadoVenta())) throw new IllegalStateException("Producto no disponible.");
        if (p.getVendedorId().equals(usuarioId)) throw new IllegalStateException("No puedes comprar tu propio producto.");
        if (productActionService.productoTieneSolicitudPendiente(productoId)) throw new IllegalStateException("Producto ya tiene una solicitud pendiente.");
        if (productActionService.productoEstaEnCarrito(productoId)) throw new IllegalStateException("Producto está en el carrito de alguien.");
    }

    // ---------------------------------------------------------------
    // Creación de solicitudes
    // ---------------------------------------------------------------

    public void crearSolicitudCompra(Solicitud solicitud) {
        validarCompra(solicitud.getUsuarioId(), solicitud.getProductoId());

        Producto producto = productQueryService.obtenerProductoPorId(solicitud.getProductoId());
        if (producto == null || !EstadoProducto.DISPONIBLE.equals(producto.getEstadoVenta())) {
            throw new IllegalStateException("El producto no está disponible para la compra.");
        }
        if (!producto.getVendedorId().equals(solicitud.getVendedorId())) {
            throw new IllegalStateException("El vendedor especificado no coincide con el vendedor del producto.");
        }

        solicitud.setTipoSolicitud("compra");
        solicitud.setEstado("pendiente");
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setNombreProducto(producto.getNombre());
        mongoTemplate.save(solicitud, "solicitudes");
    }

    /**
     * Construye y persiste una Solicitud a partir de los datos del formulario de compra.
     */
    public Solicitud crearSolicitudDesdeCompra(Usuario comprador, String productoId,
                                               String direccion, String correo, String contacto) {
        Producto producto = productQueryService.obtenerProductoPorId(productoId);
        if (producto == null || !EstadoProducto.DISPONIBLE.equals(producto.getEstadoVenta())) {
            throw new IllegalStateException("El producto ya no está disponible.");
        }
        if (producto.getVendedorId().equals(comprador.getId())) {
            throw new IllegalStateException("No puedes comprar tu propio producto.");
        }

        Solicitud solicitud = new Solicitud();
        solicitud.setProductoId(productoId);
        solicitud.setNombreProducto(producto.getNombre());
        solicitud.setUsuarioId(comprador.getId());
        solicitud.setNombreComprador(comprador.getNombreUsuario());
        solicitud.setVendedorId(producto.getVendedorId());
        solicitud.setDireccionComprador(direccion);
        solicitud.setCorreoComprador(correo);
        solicitud.setNumeroContactoComprador(contacto);
        solicitud.setTipoSolicitud(TipoSolicitud.COMPRA);
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());

        crearSolicitudCompra(solicitud);
        return solicitud;
    }

    // ---------------------------------------------------------------
    // Gestión de solicitudes
    // ---------------------------------------------------------------

    public void gestionarSolicitudCompra(String solicitudId, String accion, String descripcionVendedor) {
        Solicitud solicitud = obtenerSolicitudPorId(solicitudId);
        if (solicitud == null || !TipoSolicitud.COMPRA.equals(solicitud.getTipoSolicitud())) {
            throw new IllegalArgumentException("Solicitud inválida.");
        }
        if (!EstadoSolicitud.PENDIENTE.equals(solicitud.getEstado())) {
            throw new IllegalStateException("La solicitud ya fue gestionada.");
        }

        solicitud.setEstado(accion.equals("autorizar") ? EstadoSolicitud.AUTORIZADA : EstadoSolicitud.RECHAZADA);
        solicitud.setDescripcionVendedor(descripcionVendedor != null ? descripcionVendedor : "");
        mongoTemplate.save(solicitud, "solicitudes");

        if (EstadoSolicitud.AUTORIZADA.equals(solicitud.getEstado())) {
            Producto producto = productQueryService.obtenerProductoPorId(solicitud.getProductoId());
            if (producto != null && (EstadoProducto.DISPONIBLE.equals(producto.getEstadoVenta()) || producto.getStock() > 0)) {
                producto.setStock(producto.getStock() - 1);
                if (producto.getStock() <= 0) {
                    producto.setEstadoVenta(EstadoProducto.AGOTADO);
                }
                mongoTemplate.save(producto, "productos");
                
                // Generar copia inmutable de la venta para el comprador
                Producto clon = producto.clonarParaVenta(solicitud.getUsuarioId());
                mongoTemplate.save(clon, "productos");
                
                // Enlazar la solicitud con el clon
                solicitud.setProductoId(clon.getId());
                mongoTemplate.save(solicitud, "solicitudes");
            }
        }

        // Notificación al comprador
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(solicitud.getUsuarioId());
        notificacion.setMensaje(accion.equals("autorizar")
                ? "Tu compra de '" + solicitud.getNombreProducto() + "' fue autorizada."
                : "Tu compra de '" + solicitud.getNombreProducto() + "' fue rechazada.");
        notificacion.setFecha(LocalDateTime.now());
        mongoTemplate.save(notificacion, "notificaciones");
    }

    public void guardarSolicitud(Solicitud solicitud) {
        mongoTemplate.save(solicitud, "solicitudes");
    }

    // ---------------------------------------------------------------
    // Simulación de pago con tarjeta
    // ---------------------------------------------------------------

    public Map<String, Object> procesarCompraConTarjeta(String productoId, String compradorId,
                                                        String email, String cardNumber,
                                                        String cardHolder, String expiryMonth,
                                                        String expiryYear, String cvv,
                                                        String ipAddress) {
        validarCompra(compradorId, productoId);

        Producto producto = productQueryService.obtenerProductoPorId(productoId);
        float valor = producto.getPrecio();
        String referenceCode = "compra_" + productoId + "_" + System.currentTimeMillis();

        String[] estados = {"APPROVED", "PENDING", "REJECTED"};
        String estado = estados[new Random().nextInt(estados.length)];

        if ("REJECTED".equals(estado)) {
            throw new RuntimeException("Compra rechazada por error genérico.");
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("transactionId",   "TXN_" + System.currentTimeMillis());
        resultado.put("referenceCode",   referenceCode);
        resultado.put("status",          estado);
        resultado.put("amount",          valor);
        resultado.put("producto",        producto.getNombre());
        resultado.put("compradorEmail",  email);

        return resultado;
    }
}
