package com.celfinder.mcp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Definición descriptiva de una herramienta MCP.
 * 
 * Se usa para que los clientes MCP (IA, frontends, etc.) puedan
 * descubrir qué tools están disponibles y qué parámetros aceptan.
 * 
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
public class McpToolDefinition {

    /** Nombre único de la herramienta (ej: "buscarProductos") */
    private String name;

    /** Descripción legible de lo que hace la herramienta */
    private String description;

    /** Lista de parámetros que acepta */
    private List<McpParamDefinition> parameters = new ArrayList<>();

    public McpToolDefinition() {}

    public McpToolDefinition(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /** Método builder para agregar parámetros de forma fluida */
    public McpToolDefinition addParam(String paramName, String paramDescription, boolean required) {
        this.parameters.add(new McpParamDefinition(paramName, paramDescription, required));
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<McpParamDefinition> getParameters() {
        return parameters;
    }

    public void setParameters(List<McpParamDefinition> parameters) {
        this.parameters = parameters;
    }

    /**
     * Definición de un parámetro individual de una tool MCP.
     */
    public static class McpParamDefinition {

        private String name;
        private String description;
        private boolean required;

        public McpParamDefinition() {}

        public McpParamDefinition(String name, String description, boolean required) {
            this.name = name;
            this.description = description;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }
    }
}
