package com.celfinder.mcp.tools;

import com.celfinder.Model.Producto;
import com.celfinder.mcp.model.McpToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tool MCP: Buscar Productos en MongoDB.
 * 
 * Permite buscar productos por nombre, marca, categoría o combinación.
 * Soporta filtros opcionales de precio mínimo/máximo y estado.
 * 
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
@Component
public class BuscarProductosTool implements McpTool {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${mcp.max-results:100}")
    private int maxResults;

    @Override
    public String getName() {
        return "buscarProductos";
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition("buscarProductos",
                "Busca productos en la base de datos MongoDB por nombre, marca, categoría o texto libre.")
                .addParam("query", "Texto de búsqueda (nombre, marca o término general)", true)
                .addParam("categoria", "Filtrar por categoría específica", false)
                .addParam("marca", "Filtrar por marca específica", false)
                .addParam("precioMin", "Precio mínimo (número)", false)
                .addParam("precioMax", "Precio máximo (número)", false)
                .addParam("estado", "Estado del producto: nuevo, usado, etc.", false)
                .addParam("limit", "Número máximo de resultados (default: mcp.max-results)", false);
    }

    @Override
    public Object execute(Map<String, String> params) {
        String queryText = params.getOrDefault("query", "").trim();
        String categoria = params.get("categoria");
        String marca = params.get("marca");
        String precioMin = params.get("precioMin");
        String precioMax = params.get("precioMax");
        String estado = params.get("estado");
        int limit = parseLimit(params.get("limit"));

        List<Criteria> criterios = new ArrayList<>();

        // Búsqueda por texto libre en nombre y descripción
        if (!queryText.isEmpty()) {
            String regex = Pattern.quote(queryText);
            criterios.add(new Criteria().orOperator(
                    Criteria.where("nombre").regex(regex, "i"),
                    Criteria.where("descripcion").regex(regex, "i"),
                    Criteria.where("marca").regex(regex, "i")
            ));
        }

        // Filtro por categoría exacta
        if (categoria != null && !categoria.isBlank()) {
            criterios.add(Criteria.where("categoria").regex("^" + Pattern.quote(categoria) + "$", "i"));
        }

        // Filtro por marca exacta
        if (marca != null && !marca.isBlank()) {
            criterios.add(Criteria.where("marca").regex("^" + Pattern.quote(marca) + "$", "i"));
        }

        // Filtro por rango de precio
        if (precioMin != null || precioMax != null) {
            Criteria precioCriteria = Criteria.where("precio");
            if (precioMin != null) {
                precioCriteria = precioCriteria.gte(Float.parseFloat(precioMin));
            }
            if (precioMax != null) {
                precioCriteria = precioCriteria.lte(Float.parseFloat(precioMax));
            }
            criterios.add(precioCriteria);
        }

        // Filtro por estado
        if (estado != null && !estado.isBlank()) {
            criterios.add(Criteria.where("estado").regex("^" + Pattern.quote(estado) + "$", "i"));
        }

        Query query;
        if (criterios.isEmpty()) {
            query = new Query().limit(limit);
        } else {
            query = new Query(new Criteria().andOperator(criterios.toArray(new Criteria[0]))).limit(limit);
        }

        // Excluir imagenBase64 para reducir tamaño de respuesta
        query.fields().exclude("imagenBase64");

        List<Producto> resultados = mongoTemplate.find(query, Producto.class);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalEncontrados", resultados.size());
        resultado.put("productos", resultados.stream().map(this::productoToMap).collect(Collectors.toList()));
        return resultado;
    }

    @Override
    public String getRequiredRole() {
        return "ROLE_USER";
    }

    private Map<String, Object> productoToMap(Producto p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("nombre", p.getNombre());
        map.put("marca", p.getMarca());
        map.put("categoria", p.getCategoria());
        map.put("precio", p.getPrecio());
        map.put("precioFinal", p.getPrecioFinal());
        map.put("descuento", p.getDescuento());
        map.put("estado", p.getEstado());
        map.put("estadoVenta", p.getEstadoVenta());
        map.put("stock", p.getStock());
        map.put("vendedorId", p.getVendedorId());
        map.put("fechaPublicacion", p.getFechaPublicacion() != null ? p.getFechaPublicacion().toString() : null);
        return map;
    }

    private int parseLimit(String limitStr) {
        if (limitStr == null) return maxResults;
        try {
            int val = Integer.parseInt(limitStr);
            return Math.min(val, maxResults);
        } catch (NumberFormatException e) {
            return maxResults;
        }
    }
}
