package com.celfinder.mcp.tools;

import com.celfinder.Model.Usuario;
import com.celfinder.mcp.model.McpToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tool MCP: Buscar Usuarios en MongoDB.
 * Acceso restringido a ADMIN vía SecurityConfig.
 * Módulo: MCP - Model Context Protocol para MongoDB
 */
@Component
public class BuscarUsuariosTool implements McpTool {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${mcp.max-results:100}")
    private int maxResults;

    @Override
    public String getName() {
        return "buscarUsuarios";
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition("buscarUsuarios",
                "Busca usuarios por email, nombre o rol. Solo accesible por ADMIN.")
                .addParam("query", "Texto de búsqueda (email o nombreUsuario)", false)
                .addParam("rol", "Filtrar por rol: ROLE_USER, ROLE_VENDEDOR, ROLE_ADMIN", false)
                .addParam("limit", "Máximo de resultados", false);
    }

    @Override
    public Object execute(Map<String, String> params) {
        String queryText = params.getOrDefault("query", "").trim();
        String rol = params.get("rol");
        int limit = parseLimit(params.get("limit"));

        List<Criteria> criterios = new ArrayList<>();

        if (!queryText.isEmpty()) {
            String regex = Pattern.quote(queryText);
            criterios.add(new Criteria().orOperator(
                    Criteria.where("email").regex(regex, "i"),
                    Criteria.where("nombreUsuario").regex(regex, "i")
            ));
        }

        if (rol != null && !rol.isBlank()) {
            criterios.add(Criteria.where("roles").is(rol));
        }

        Query query;
        if (criterios.isEmpty()) {
            query = new Query().limit(limit);
        } else {
            query = new Query(new Criteria().andOperator(criterios.toArray(new Criteria[0]))).limit(limit);
        }

        // Excluir contrasena e imágenes por seguridad y tamaño
        query.fields().exclude("contrasena").exclude("imagenPerfil").exclude("fondoPerfil");

        List<Usuario> usuarios = mongoTemplate.find(query, Usuario.class);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("totalEncontrados", usuarios.size());
        resultado.put("usuarios", usuarios.stream().map(this::usuarioToMap).collect(Collectors.toList()));
        return resultado;
    }

    @Override
    public String getRequiredRole() {
        return "ROLE_ADMIN";
    }

    private Map<String, Object> usuarioToMap(Usuario u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("nombreUsuario", u.getNombreUsuario());
        map.put("email", u.getEmail());
        map.put("roles", u.getRoles());
        map.put("ciudad", u.getCiudad());
        map.put("departamento", u.getDepartamento());
        map.put("estadoCuenta", u.getEstadoCuenta());
        map.put("fechaCreacion", u.getFechaCreacion() != null ? u.getFechaCreacion().toString() : null);
        return map;
    }

    private int parseLimit(String limitStr) {
        if (limitStr == null) return maxResults;
        try {
            return Math.min(Integer.parseInt(limitStr), maxResults);
        } catch (NumberFormatException e) {
            return maxResults;
        }
    }
}
