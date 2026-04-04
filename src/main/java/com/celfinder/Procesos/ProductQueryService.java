package com.celfinder.Procesos;

import com.celfinder.Model.Producto;
import com.celfinder.util.EstadoProducto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Servicio de consulta de productos.
 * Responsabilidad única: leer y buscar productos en la base de datos.
 */
@Service
public class ProductQueryService {

    private static final Logger logger = LoggerFactory.getLogger(ProductQueryService.class);

    private final MongoTemplate mongoTemplate;

    @Autowired
    public ProductQueryService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    // ---------------------------------------------------------------
    // Consultas básicas
    // ---------------------------------------------------------------

    public Producto obtenerProductoPorId(String id) {
        return mongoTemplate.findById(id, Producto.class, "productos");
    }

    public List<Producto> obtenerProductosEnVenta() {
        return mongoTemplate.find(
                new Query(Criteria.where("estadoVenta").is(EstadoProducto.DISPONIBLE)),
                Producto.class, "productos");
    }

    public List<Producto> obtenerProductosPorVendedor(String vendedorId) {
        return mongoTemplate.find(
                new Query(Criteria.where("vendedorId").is(vendedorId)
                        .and("estadoVenta").is(EstadoProducto.DISPONIBLE)),
                Producto.class, "productos");
    }

    // ---------------------------------------------------------------
    // Búsqueda con filtros
    // ---------------------------------------------------------------

    public List<Producto> buscarProductos(String nombre, String estado,
                                          Float precioMin, Float precioMax) {
        Query query = buildSearchQuery(nombre, estado, precioMin, precioMax, null);
        return mongoTemplate.find(query, Producto.class, "productos");
    }

    public List<Producto> buscarProductosExcluyendoId(String nombre, String estado,
                                                       Float precioMin, Float precioMax,
                                                       String idExcluir) {
        Query query = buildSearchQuery(nombre, estado, precioMin, precioMax, idExcluir);
        return mongoTemplate.find(query, Producto.class, "productos");
    }

    public List<Producto> obtenerProductosSimilares(String id, String nombre) {
        Query query = new Query(Criteria.where("nombre").regex(Pattern.quote(nombre), "i")
                .and("id").ne(id)
                .and("estadoVenta").is(EstadoProducto.DISPONIBLE));
        return mongoTemplate.find(query, Producto.class, "productos");
    }

    // ---------------------------------------------------------------
    // Paginación en memoria
    // ---------------------------------------------------------------

    public List<Producto> paginarProductos(List<Producto> todos, int page, int itemsPerPage) {
        int totalItems = todos.size();
        int start = Math.min(page * itemsPerPage, totalItems);
        int end   = Math.min(start + itemsPerPage, totalItems);
        return todos.subList(start, end);
    }

    // ---------------------------------------------------------------
    // Helpers privados
    // ---------------------------------------------------------------

    private Query buildSearchQuery(String nombre, String estado,
                                    Float precioMin, Float precioMax, String idExcluir) {
        List<Criteria> criteria = new ArrayList<>();

        if (nombre != null && !nombre.isEmpty()) {
            criteria.add(Criteria.where("nombre").regex(Pattern.quote(nombre), "i"));
        }
        if (estado != null && !estado.isEmpty()) {
            criteria.add(Criteria.where("estado").is(estado));
        }
        if (precioMin != null) criteria.add(Criteria.where("precio").gte(precioMin));
        if (precioMax != null) criteria.add(Criteria.where("precio").lte(precioMax));
        criteria.add(Criteria.where("estadoVenta").is(EstadoProducto.DISPONIBLE));

        if (idExcluir != null) {
            criteria.add(Criteria.where("id").ne(idExcluir));
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }
        return query;
    }
}
