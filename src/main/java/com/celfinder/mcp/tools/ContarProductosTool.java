package com.celfinder.mcp.tools;

import com.celfinder.Model.Producto;
import com.celfinder.mcp.model.McpToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Tool MCP: Contar Productos en MongoDB.
 * 
 * Retorna conteos de productos filtrados por categoría,
 * marca, vendedor o estado de venta.
 * 
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
@Component
public class ContarProductosTool implements McpTool {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "contarProductos";
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition("contarProductos",
                "Cuenta productos en la base de datos, opcionalmente filtrados por categoría, marca, vendedor o estado.")
                .addParam("categoria", "Filtrar conteo por categoría", false)
                .addParam("marca", "Filtrar conteo por marca", false)
                .addParam("vendedorId", "Filtrar conteo por ID del vendedor", false)
                .addParam("estadoVenta", "Filtrar por estado de venta: disponible, vendido, etc.", false);
    }

    @Override
    public Object execute(Map<String, String> params) {
        String categoria = params.get("categoria");
        String marca = params.get("marca");
        String vendedorId = params.get("vendedorId");
        String estadoVenta = params.get("estadoVenta");

        Query query = new Query();

        if (categoria != null && !categoria.isBlank()) {
            query.addCriteria(Criteria.where("categoria").regex("^" + Pattern.quote(categoria) + "$", "i"));
        }
        if (marca != null && !marca.isBlank()) {
            query.addCriteria(Criteria.where("marca").regex("^" + Pattern.quote(marca) + "$", "i"));
        }
        if (vendedorId != null && !vendedorId.isBlank()) {
            query.addCriteria(Criteria.where("vendedorId").is(vendedorId));
        }
        if (estadoVenta != null && !estadoVenta.isBlank()) {
            query.addCriteria(Criteria.where("estadoVenta").regex("^" + Pattern.quote(estadoVenta) + "$", "i"));
        }

        long total = mongoTemplate.count(query, Producto.class);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("total", total);
        resultado.put("filtrosAplicados", params);
        return resultado;
    }

    @Override
    public String getRequiredRole() {
        return "ROLE_USER";
    }
}
