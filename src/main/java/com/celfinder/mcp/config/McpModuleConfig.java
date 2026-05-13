package com.celfinder.mcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración principal del Módulo MCP (Model Context Protocol).
 * 
 * Este módulo es COMPLETAMENTE INDEPENDIENTE del resto de la aplicación.
 * Se activa/desactiva con la propiedad mcp.enabled=true/false.
 * 
 * El @ComponentScan asegura que todas las clases del paquete mcp
 * (tools, registry, service, controller) se registren automáticamente.
 * 
 * Módulo: MCP - Model Context Protocol para MongoDB
 * Tipo: Nuevo módulo agregado (no modifica archivos existentes)
 */
@Configuration
@ConditionalOnProperty(name = "mcp.enabled", havingValue = "true", matchIfMissing = false)
@ComponentScan(basePackages = "com.celfinder.mcp")
public class McpModuleConfig {

    private static final Logger log = LoggerFactory.getLogger(McpModuleConfig.class);

    public McpModuleConfig() {
        log.info("============================================================");
        log.info("  MÓDULO MCP (Model Context Protocol) - ACTIVADO");
        log.info("  Endpoint base: /mcp/**");
        log.info("  Este es un módulo independiente para consultas MongoDB vía IA");
        log.info("============================================================");
    }
}
