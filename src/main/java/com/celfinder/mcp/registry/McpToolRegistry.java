package com.celfinder.mcp.registry;

import com.celfinder.mcp.model.McpToolDefinition;
import com.celfinder.mcp.tools.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registro central de todas las herramientas MCP.
 * 
 * Auto-descubre todos los beans que implementan McpTool
 * y los indexa por nombre para acceso rápido.
 * 
 * Para registrar una nueva tool, basta con crear una clase
 * que implemente McpTool y anotarla con @Component.
 * 
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
@Component
public class McpToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(McpToolRegistry.class);

    private final Map<String, McpTool> toolMap;

    /**
     * Spring inyecta automáticamente todas las implementaciones de McpTool.
     */
    public McpToolRegistry(List<McpTool> tools) {
        this.toolMap = new LinkedHashMap<>();
        for (McpTool tool : tools) {
            toolMap.put(tool.getName(), tool);
            log.info("[MCP] Tool registrada: '{}'", tool.getName());
        }
        log.info("[MCP] Total de tools registradas: {}", toolMap.size());
    }

    /** Obtiene una tool por nombre, o null si no existe */
    public McpTool getTool(String name) {
        return toolMap.get(name);
    }

    /** Verifica si existe una tool con el nombre dado */
    public boolean exists(String name) {
        return toolMap.containsKey(name);
    }

    /** Retorna las definiciones de todas las tools registradas */
    public List<McpToolDefinition> getAllDefinitions() {
        return toolMap.values().stream()
                .map(McpTool::getDefinition)
                .collect(Collectors.toList());
    }

    /** Retorna los nombres de todas las tools registradas */
    public Set<String> getToolNames() {
        return Collections.unmodifiableSet(toolMap.keySet());
    }

    /** Cantidad de tools registradas */
    public int size() {
        return toolMap.size();
    }
}
