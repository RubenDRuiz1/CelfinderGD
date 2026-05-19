package com.celfinder.mcp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class McpAuditService {

    private static final Logger log = LoggerFactory.getLogger(McpAuditService.class);

    @Value("${mcp.audit-path:mcp_audit.log}")
    private String auditPath;

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public synchronized void registrar(String toolName, String usuario, String params, boolean exito, long duracionMs, String detalle) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(auditPath, true))) {
            String timestamp = LocalDateTime.now().format(fmt);
            String estado = exito ? "OK" : "ERROR";
            String linea = String.format("[%s] %s | USUARIO: %s | TOOL: %s | ESTADO: %s | DURACION: %dms | PARAMS: %s | DETALLE: %s",
                    timestamp, estado, usuario, toolName, estado, duracionMs, params, detalle);
            writer.println(linea);
        } catch (Exception e) {
            log.error("Error escribiendo auditoría MCP: {}", e.getMessage());
        }
    }

    public synchronized String leerTodo() {
        try {
            File f = new File(auditPath);
            if (!f.exists()) return "=== BITÁCORA MCP VACÍA ===";
            return Files.readString(Paths.get(auditPath));
        } catch (Exception e) {
            return "Error leyendo bitácora: " + e.getMessage();
        }
    }

    public synchronized void limpiar() {
        try {
            new FileWriter(auditPath).close();
        } catch (Exception e) {
            log.error("Error limpiando auditoría MCP: {}", e.getMessage());
        }
    }

    public synchronized long contarLineas() {
        try {
            File f = new File(auditPath);
            if (!f.exists()) return 0;
            return Files.lines(Paths.get(auditPath)).count();
        } catch (Exception e) {
            return 0;
        }
    }
}
