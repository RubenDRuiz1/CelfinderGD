package com.celfinder.mcp.model;

/**
 * DTO de respuesta del Módulo MCP (Model Context Protocol).
 * 
 * Formato estándar para todas las respuestas MCP, incluyendo
 * estado de éxito/error, datos retornados y metadatos.
 * 
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
public class McpToolResponse {

    /** Indica si la ejecución de la tool fue exitosa */
    private boolean success;

    /** Nombre de la herramienta que generó esta respuesta */
    private String toolName;

    /** Datos retornados por la herramienta (estructura varía por tool) */
    private Object data;

    /** Mensaje de error (null si success = true) */
    private String errorMessage;

    /** Tiempo de ejecución en milisegundos */
    private long executionTimeMs;

    public McpToolResponse() {}

    /** Fábrica para respuestas exitosas */
    public static McpToolResponse ok(String toolName, Object data, long executionTimeMs) {
        McpToolResponse response = new McpToolResponse();
        response.success = true;
        response.toolName = toolName;
        response.data = data;
        response.executionTimeMs = executionTimeMs;
        return response;
    }

    /** Fábrica para respuestas de error */
    public static McpToolResponse error(String toolName, String errorMessage) {
        McpToolResponse response = new McpToolResponse();
        response.success = false;
        response.toolName = toolName;
        response.errorMessage = errorMessage;
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}
