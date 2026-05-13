package com.celfinder.mcp.tools;

import com.celfinder.Model.Producto;
import com.celfinder.mcp.model.McpToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Tool MCP: Estadísticas de Ventas desde MongoDB.
 * 
 * Genera reportes de productos más vendidos, totales por categoría,
 * y resúmenes generales de la colección de productos.
 * 
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
@Component
public class EstadisticasVentasTool implements McpTool {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "estadisticasVentas";
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition("estadisticasVentas",
                "Genera estadísticas de ventas: totales, por categoría, por marca, productos vendidos.")
                .addParam("tipo", "Tipo de estadística: 'general', 'porCategoria', 'porMarca', 'vendidos' (default: general)", false)
                .addParam("limit", "Máximo de resultados en rankings (default: 10)", false);
    }

    @Override
    public Object execute(Map<String, String> params) {
        String tipo = params.getOrDefault("tipo", "general").toLowerCase();
        int limit = parseLimit(params.get("limit"), 10);

        switch (tipo) {
            case "porcategoria":
                return estadisticasPorCampo("categoria", limit);
            case "pormarca":
                return estadisticasPorCampo("marca", limit);
            case "vendidos":
                return productosVendidos(limit);
            case "general":
            default:
                return estadisticasGenerales();
        }
    }

    private Map<String, Object> estadisticasGenerales() {
        long totalProductos = mongoTemplate.count(new Query(), Producto.class);
        long productosVendidos = mongoTemplate.count(
                new Query(Criteria.where("estadoVenta").is("vendido")), Producto.class);
        long productosDisponibles = mongoTemplate.count(
                new Query(Criteria.where("estadoVenta").is("disponible")), Producto.class);

        List<String> categorias = mongoTemplate.findDistinct(
                new Query(), "categoria", Producto.class, String.class);
        List<String> marcas = mongoTemplate.findDistinct(
                new Query(), "marca", Producto.class, String.class);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalProductos", totalProductos);
        resultado.put("productosVendidos", productosVendidos);
        resultado.put("productosDisponibles", productosDisponibles);
        resultado.put("totalCategorias", categorias.size());
        resultado.put("totalMarcas", marcas.size());
        return resultado;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> estadisticasPorCampo(String campo, int limit) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group(campo).count().as("total"),
                Aggregation.sort(Sort.Direction.DESC, "total"),
                Aggregation.limit(limit)
        );

        AggregationResults<Map> results = mongoTemplate.aggregate(agg, "productos", Map.class);

        List<Map<String, Object>> datos = new ArrayList<>();
        for (Map doc : results.getMappedResults()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put(campo, doc.get("_id"));
            item.put("total", doc.get("total"));
            datos.add(item);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("agrupacion", campo);
        resultado.put("datos", datos);
        return resultado;
    }

    private Map<String, Object> productosVendidos(int limit) {
        Query query = new Query(Criteria.where("estadoVenta").is("vendido"))
                .with(Sort.by(Sort.Direction.DESC, "fechaPublicacion"))
                .limit(limit);

        query.fields().exclude("imagenBase64");

        List<Producto> vendidos = mongoTemplate.find(query, Producto.class);

        List<Map<String, Object>> lista = new ArrayList<>();
        for (Producto p : vendidos) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("nombre", p.getNombre());
            item.put("marca", p.getMarca());
            item.put("categoria", p.getCategoria());
            item.put("precio", p.getPrecio());
            item.put("vendedorId", p.getVendedorId());
            item.put("compradorId", p.getCompradorId());
            lista.add(item);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalVendidos", vendidos.size());
        resultado.put("productos", lista);
        return resultado;
    }

    @Override
    public String getRequiredRole() {
        return "ROLE_ADMIN";
    }

    private int parseLimit(String limitStr, int defaultVal) {
        if (limitStr == null) return defaultVal;
        try {
            return Math.min(Integer.parseInt(limitStr), 100);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
