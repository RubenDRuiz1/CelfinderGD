package com.celfinder.mcp.tools;

import com.celfinder.Model.Producto;
import com.celfinder.mcp.model.McpToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Tool MCP: Listar Categorías y Marcas disponibles.
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
@Component
public class ListarCategoriasTool implements McpTool {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "listarCategorias";
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition("listarCategorias",
                "Lista categorías y/o marcas distintas disponibles en productos.")
                .addParam("tipo", "Qué listar: 'categorias', 'marcas' o 'ambas' (default: ambas)", false);
    }

    @Override
    public Object execute(Map<String, String> params) {
        String tipo = params.getOrDefault("tipo", "ambas").toLowerCase();
        Map<String, Object> resultado = new LinkedHashMap<>();

        if ("categorias".equals(tipo) || "ambas".equals(tipo)) {
            List<String> categorias = mongoTemplate
                    .findDistinct(new Query(), "categoria", Producto.class, String.class)
                    .stream().filter(Objects::nonNull).sorted().collect(Collectors.toList());
            resultado.put("categorias", categorias);
            resultado.put("totalCategorias", categorias.size());
        }

        if ("marcas".equals(tipo) || "ambas".equals(tipo)) {
            List<String> marcas = mongoTemplate
                    .findDistinct(new Query(), "marca", Producto.class, String.class)
                    .stream().filter(Objects::nonNull).sorted().collect(Collectors.toList());
            resultado.put("marcas", marcas);
            resultado.put("totalMarcas", marcas.size());
        }

        return resultado;
    }

    @Override
    public String getRequiredRole() {
        return "ROLE_USER";
    }
}
