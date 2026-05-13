package com.celfinder.mcp.model;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO de petición del Módulo MCP (Model Context Protocol).
 * 
 * Representa una solicitud para ejecutar una herramienta MCP específica
 * con sus parámetros correspondientes.
 * 
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
public class McpToolRequest {

    /** Nombre de la herramienta MCP a ejecutar (ej: "buscarProductos") */
    private String toolName;

    /** Parámetros de la herramienta como pares clave-valor */
    private Map<String, String> params = new HashMap<>();

    public McpToolRequest() {}

    public McpToolRequest(String toolName, Map<String, String> params) {
        this.toolName = toolName;
        this.params = params != null ? params : new HashMap<>();
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params != null ? params : new HashMap<>();
    }

    @Override
    public String toString() {
        return "McpToolRequest{toolName='" + toolName + "', params=" + params + "}";
    }
}
