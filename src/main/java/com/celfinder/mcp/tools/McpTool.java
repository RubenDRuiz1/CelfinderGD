package com.celfinder.mcp.tools;

import com.celfinder.mcp.model.McpToolDefinition;
import java.util.Map;

/**
 * Interfaz base para todas las herramientas del Módulo MCP.
 * 
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
public interface McpTool {

    /**
     * Nombre único de la herramienta.
     */
    String getName();

    /**
     * Definición completa de la tool.
     */
    McpToolDefinition getDefinition();

    /**
     * Ejecuta la herramienta con los parámetros proporcionados.
     */
    Object execute(Map<String, String> params);

    /**
     * Define el rol mínimo requerido para ejecutar esta herramienta.
     */
    String getRequiredRole();
}
