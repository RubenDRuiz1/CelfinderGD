package com.celfinder.mcp.tools;

import com.celfinder.Procesos.ProductActionService;
import com.celfinder.Procesos.ProductQueryService;
import com.celfinder.Model.Producto;
import com.celfinder.mcp.model.McpToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool MCP para que el Administrador modifique datos de un producto.
 */
@Component
public class ModificarProductoTool implements McpTool {

    @Autowired
    private ProductActionService productActionService;

    @Autowired
    private ProductQueryService productQueryService;

    @Override
    public String getName() {
        return "modificarProducto";
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(getName(),
                "Permite al administrador modificar el precio o estado de un producto.")
                .addParam("productoId", "ID del producto", true)
                .addParam("nuevoPrecio", "Nuevo precio numérico (ej: 450000)", false)
                .addParam("nuevoEstado", "Nuevo estado: 'disponible', 'agotado'", false);
    }

    @Override
    public Object execute(Map<String, String> params) {
        String id = params.get("productoId");
        if (id == null) throw new IllegalArgumentException("productoId es requerido.");

        Producto p = productQueryService.obtenerProductoPorId(id);
        if (p == null) throw new IllegalArgumentException("Producto no encontrado.");

        if (params.containsKey("nuevoPrecio")) {
            p.setPrecio(Float.parseFloat(params.get("nuevoPrecio")));
        }
        if (params.containsKey("nuevoEstado")) {
            p.setEstado(params.get("nuevoEstado"));
        }

        productActionService.actualizarProducto(p);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("producto", p.getNombre());
        response.put("nuevoPrecio", p.getPrecio());
        return response;
    }

    @Override
    public String getRequiredRole() {
        return "ROLE_ADMIN";
    }
}
