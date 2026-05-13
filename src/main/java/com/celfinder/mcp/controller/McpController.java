package com.celfinder.mcp.controller;

import com.celfinder.mcp.model.McpToolDefinition;
import com.celfinder.mcp.model.McpToolRequest;
import com.celfinder.mcp.model.McpToolResponse;
import com.celfinder.mcp.registry.McpToolRegistry;
import com.celfinder.mcp.service.McpDispatcherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller REST del Módulo MCP (Model Context Protocol).
 * 
 * Expone los endpoints para:
 * - Descubrir tools disponibles (GET /mcp/tools)
 * - Ejecutar una tool (POST /mcp/execute)
 * - Verificar salud del módulo (GET /mcp/health)
 * 
 * Todos los endpoints están protegidos por ROLE_ADMIN en SecurityConfig.
 * 
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
@RestController
@RequestMapping("/mcp")
public class McpController {

    @Autowired
    private McpDispatcherService dispatcher;

    @Autowired
    private McpToolRegistry registry;

    /**
     * GET /mcp/tools
     * Lista todas las herramientas MCP disponibles con sus definiciones.
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> listTools() {
        List<McpToolDefinition> definitions = registry.getAllDefinitions();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("module", "MCP - Model Context Protocol");
        response.put("totalTools", definitions.size());
        response.put("tools", definitions);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /mcp/execute
     * Ejecuta una herramienta MCP con los parámetros proporcionados.
     * 
     * Body ejemplo:
     * {
     *   "toolName": "buscarProductos",
     *   "params": { "query": "samsung", "precioMax": "500000" }
     * }
     */
    @PostMapping("/execute")
    public ResponseEntity<McpToolResponse> execute(@RequestBody McpToolRequest request) {
        McpToolResponse response = dispatcher.dispatch(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * GET /mcp/health
     * Verifica que el módulo MCP está activo y funcional.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("module", "MCP - Model Context Protocol para MongoDB");
        status.put("status", "UP");
        status.put("toolsRegistradas", registry.size());
        status.put("toolsDisponibles", registry.getToolNames());
        status.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }
}
