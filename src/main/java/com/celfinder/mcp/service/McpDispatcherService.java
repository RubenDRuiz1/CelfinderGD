package com.celfinder.mcp.service;

import com.celfinder.mcp.model.McpToolRequest;
import com.celfinder.mcp.model.McpToolResponse;
import com.celfinder.mcp.registry.McpToolRegistry;
import com.celfinder.mcp.tools.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Dispatcher central del Módulo MCP.
 * 
 * Recibe peticiones McpToolRequest, localiza la tool correspondiente
 * en el McpToolRegistry, la ejecuta y retorna la respuesta estándar.
 * 
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
@Service
public class McpDispatcherService {

    private static final Logger log = LoggerFactory.getLogger(McpDispatcherService.class);

    @Autowired
    private McpToolRegistry registry;

    /**
     * Ejecuta una herramienta MCP según la petición recibida.
     *
     * @param request petición con toolName y params
     * @return respuesta estándar MCP con resultado o error
     */
    public McpToolResponse dispatch(McpToolRequest request) {
        String toolName = request.getToolName();

        if (toolName == null || toolName.isBlank()) {
            log.warn("[MCP] Petición recibida sin toolName");
            return McpToolResponse.error(null, "El campo 'toolName' es requerido.");
        }

        McpTool tool = registry.getTool(toolName);
        if (tool == null) {
            log.warn("[MCP] Tool no encontrada: '{}'", toolName);
            return McpToolResponse.error(toolName,
                    "Tool '" + toolName + "' no encontrada. Tools disponibles: " + registry.getToolNames());
        }

        // --- VALIDACIÓN DE SEGURIDAD BASADA EN ROLES ---
        if (!verificarPermisos(tool)) {
            log.warn("[MCP] Acceso denegado para la tool '{}'", toolName);
            return McpToolResponse.error(toolName, "Acceso denegado: No tienes el rol necesario (" + tool.getRequiredRole() + ")");
        }

        log.info("[MCP] Ejecutando tool '{}' con params: {}", toolName, request.getParams());
        long inicio = System.currentTimeMillis();

        try {
            Object resultado = tool.execute(request.getParams());
            long duracion = System.currentTimeMillis() - inicio;
            log.info("[MCP] Tool '{}' ejecutada en {}ms", toolName, duracion);
            return McpToolResponse.ok(toolName, resultado, duracion);
        } catch (Exception e) {
            long duracion = System.currentTimeMillis() - inicio;
            log.error("[MCP] Error ejecutando tool '{}' ({}ms): {}", toolName, duracion, e.getMessage(), e);
            return McpToolResponse.error(toolName, "Error al ejecutar: " + e.getMessage());
        }
    }

    /**
     * Verifica si el usuario actual en el SecurityContext tiene el rol requerido por la tool.
     */
    private boolean verificarPermisos(McpTool tool) {
        String rolRequerido = tool.getRequiredRole();
        
        // El rol ROLE_USER es el base en Celfinder
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities().contains(new SimpleGrantedAuthority(rolRequerido));
    }
}
