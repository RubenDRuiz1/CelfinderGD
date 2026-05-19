package com.celfinder.mcp.tools;

import com.celfinder.Procesos.ProductActionService;
import com.celfinder.mcp.model.McpToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EliminarProductoTool implements McpTool {

    @Autowired
    private ProductActionService productActionService;

    @Override
    public String getName() {
        return "eliminarProducto";
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(getName(),
                "Elimina permanentemente un producto del catálogo usando su ID de MongoDB.")
                .addParam("productoId", "ID del producto a eliminar", true);
    }

    @Override
    public Object execute(Map<String, String> params) {
        String id = params.get("productoId");
        if (id == null || id.isBlank()) throw new IllegalArgumentException("productoId es requerido.");

        productActionService.eliminarProducto(id.trim());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("mensaje", "El producto " + id.trim() + " ha sido eliminado del catálogo correctamente.");
        return response;
    }

    @Override
    public String getRequiredRole() {
        return "ROLE_ADMIN";
    }
}
