package com.celfinder.Procesos;

import com.celfinder.Model.Producto;
import com.celfinder.Model.Usuario;
import com.celfinder.Model.Visualizacion;
import com.celfinder.util.EstadoProducto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio de acciones sobre productos.
 * Responsabilidad única: publicar, actualizar, eliminar y procesar imágenes de productos.
 */
@Service
public class ProductActionService {

    private static final Logger logger = LoggerFactory.getLogger(ProductActionService.class);

    private final MongoTemplate mongoTemplate;
    private final ProductQueryService productQueryService;

    @Autowired
    public ProductActionService(MongoTemplate mongoTemplate,
                                ProductQueryService productQueryService) {
        this.mongoTemplate = mongoTemplate;
        this.productQueryService = productQueryService;
    }

    // ---------------------------------------------------------------
    // Publicación
    // ---------------------------------------------------------------

    public void publicarProducto(Producto producto) {
        producto.setFechaPublicacion(LocalDateTime.now());
        producto.setEstadoVenta(EstadoProducto.DISPONIBLE);
        mongoTemplate.save(producto, "productos");
    }

    /**
     * Asigna vendedor, fecha, estado y especificaciones dinámicas (prefijo "spec_"),
     * luego persiste el producto.
     */
    public boolean prepararYPublicarProducto(Producto producto, Usuario vendedor,
                                             Map<String, String> allParams) {
        // Logic to stack: same vendor, name, brand, category
        Query query = new Query(Criteria.where("vendedorId").is(vendedor.getId())
                .and("nombre").is(producto.getNombre())
                .and("marca").is(producto.getMarca())
                .and("categoria").is(producto.getCategoria()));
        
        Producto existing = mongoTemplate.findOne(query, Producto.class);
        
        if (existing != null) {
            existing.setStock(existing.getStock() + Math.max(1, producto.getStock()));
            existing.setPrecio(producto.getPrecio()); // Update to latest price
            existing.setDescripcion(producto.getDescripcion()); // Update to latest description
            
            // Update image if a new one was provided
            if (producto.getImagenBase64() != null && !producto.getImagenBase64().isEmpty()) {
                existing.setImagenBase64(producto.getImagenBase64());
            }
            
            // Re-process specs
            Map<String, String> especificaciones = new HashMap<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("spec_") && entry.getValue() != null
                        && !entry.getValue().trim().isEmpty()) {
                    especificaciones.put(key.substring(5), entry.getValue().trim());
                }
            }
            existing.setEspecificaciones(especificaciones);
            existing.setEstadoVenta(com.celfinder.util.EstadoProducto.DISPONIBLE);
            mongoTemplate.save(existing, "productos");
            logger.info("STACKED product: Vendor='{}', Name='{}', Brand='{}', Category='{}'. New stock: {}. ID: {}", 
                    vendedor.getId(), existing.getNombre(), existing.getMarca(), existing.getCategoria(), existing.getStock(), existing.getId());
            return true; // Indicates it was stacked
        } else {
            producto.setVendedorId(vendedor.getId());
            producto.setFechaPublicacion(LocalDateTime.now());
            producto.setEstadoVenta(com.celfinder.util.EstadoProducto.DISPONIBLE);
            if (producto.getStock() <= 0) producto.setStock(1);

            Map<String, String> especificaciones = new HashMap<>();
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("spec_") && entry.getValue() != null
                        && !entry.getValue().trim().isEmpty()) {
                    especificaciones.put(key.substring(5), entry.getValue().trim());
                }
            }
            producto.setEspecificaciones(especificaciones);
            mongoTemplate.save(producto, "productos");
            logger.info("NEW product published: Vendor='{}', Name='{}', Brand='{}', Category='{}'. Stock: {}. ID: {}", 
                    vendedor.getId(), producto.getNombre(), producto.getMarca(), producto.getCategoria(), producto.getStock(), producto.getId());
            return false; // Indicates it was created new
        }
    }

    // ---------------------------------------------------------------
    // Actualización
    // ---------------------------------------------------------------

    public void actualizarProducto(Producto producto) {
        if (producto.getId() == null) {
            throw new IllegalArgumentException("El ID del producto es obligatorio para actualizar.");
        }
        Producto existing = productQueryService.obtenerProductoPorId(producto.getId());
        if (existing == null) {
            throw new IllegalStateException("El producto no existe.");
        }
        // Preservar campos inmutables
        producto.setFechaPublicacion(existing.getFechaPublicacion());
        producto.setEstadoVenta(existing.getEstadoVenta());
        producto.setVendedorId(existing.getVendedorId());
        mongoTemplate.save(producto, "productos");
    }

    /**
     * Valida permisos y aplica los cambios editables sobre el producto existente.
     * Procesa imagen Base64 si se proporciona.
     *
     * @return el producto actualizado y persistido
     */
    public Producto prepararYActualizarProducto(String id, Producto datosNuevos,
                                                Usuario usuario, String imagenBase64) {
        Producto existente = productQueryService.obtenerProductoPorId(id);
        if (existente == null) {
            throw new IllegalStateException("El producto no existe.");
        }
        if (!usuario.getId().equals(existente.getVendedorId())
                && !usuario.getRoles().contains("ROLE_ADMIN")) {
            throw new IllegalStateException("Solo el vendedor o un administrador pueden editar este producto.");
        }

        existente.setNombre(datosNuevos.getNombre());
        existente.setPrecio(datosNuevos.getPrecio());
        existente.setEstadoVenta(datosNuevos.getEstadoVenta());
        existente.setDescripcion(datosNuevos.getDescripcion());
        existente.setMarca(datosNuevos.getMarca());
        existente.setCategoria(datosNuevos.getCategoria());
        existente.setDescuento(datosNuevos.getDescuento());
        existente.setStock(datosNuevos.getStock());

        // Procesamiento de imagen Base64
        if (imagenBase64 != null && !imagenBase64.isEmpty()) {
            existente.setImagenBase64(imagenBase64);
        }

        actualizarProducto(existente);
        return existente;
    }

    // ---------------------------------------------------------------
    // Eliminación
    // ---------------------------------------------------------------

    public void eliminarProducto(String id) {
        Producto producto = productQueryService.obtenerProductoPorId(id);
        if (producto == null) {
            throw new IllegalArgumentException("El producto no existe.");
        }
        if (productoTieneSolicitudPendiente(id)) {
            throw new IllegalStateException("No se puede eliminar el producto porque tiene solicitudes pendientes.");
        }
        if (productoEstaEnCarrito(id)) {
            throw new IllegalStateException("Producto está en carrito.");
        }
        mongoTemplate.remove(producto, "productos");
    }

    // ---------------------------------------------------------------
    // Visualizaciones
    // ---------------------------------------------------------------

    public void registrarVisualizacion(String usuarioId, String productoId) {
        Visualizacion v = new Visualizacion();
        v.setUsuarioId(usuarioId);
        v.setProductoId(productoId);
        mongoTemplate.save(v);
    }

    // ---------------------------------------------------------------
    // Helpers de validación de estado
    // ---------------------------------------------------------------

    public boolean productoEstaEnCarrito(String productoId) {
        return mongoTemplate.exists(
                new Query(Criteria.where("productoIds").in(productoId)),
                com.celfinder.Model.Carrito.class);
    }

    public boolean productoTieneSolicitudPendiente(String productoId) {
        return mongoTemplate.exists(
                new Query(Criteria.where("productoId").is(productoId)
                        .and("estado").is(com.celfinder.util.EstadoSolicitud.PENDIENTE)),
                com.celfinder.Model.Solicitud.class);
    }

    public boolean productoEstaDisponible(String productoId) {
        Producto p = productQueryService.obtenerProductoPorId(productoId);
        return p != null && EstadoProducto.DISPONIBLE.equals(p.getEstadoVenta());
    }
}
