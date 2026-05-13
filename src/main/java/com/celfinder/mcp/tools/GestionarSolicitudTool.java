package com.celfinder.mcp.tools;

import com.celfinder.Procesos.PurchaseService;
import com.celfinder.mcp.model.McpToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool MCP para que el Administrador gestione solicitudes de compra.
 */
@Component
public class GestionarSolicitudTool implements McpTool {

    @Autowired
    private PurchaseService purchaseService;

    @Override
    public String getName() {
        return "gestionarSolicitud";
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(getName(), 
                "Permite al administrador aprobar o rechazar una solicitud de compra usando su ID.")
                .addParam("solicitudId", "ID de la solicitud/pedido (ej: 60...)", true)
                .addParam("accion", "Acción: 'aprobar' o 'rechazar'", true)
                .addParam("comentario", "Comentario del administrador", false);
    }

    @Override
    public Object execute(Map<String, String> params) {
        String id = params.get("solicitudId");
        String accion = params.get("accion");
        String comentario = params.getOrDefault("comentario", "Gestionado vía Asistente IA");

        if (id == null || accion == null) {
            throw new IllegalArgumentException("solicitudId y accion son requeridos.");
        }

        purchaseService.gestionarSolicitudCompra(id, accion, comentario);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("mensaje", "La solicitud " + id + " ha sido gestionada: " + accion);
        return response;
    }

    @Override
    public String getRequiredRole() {
        return "ROLE_ADMIN";
    }
}
