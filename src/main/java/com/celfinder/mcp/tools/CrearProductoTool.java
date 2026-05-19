package com.celfinder.mcp.tools;

import com.celfinder.Model.Producto;
import com.celfinder.mcp.model.McpToolDefinition;
import com.celfinder.util.EstadoProducto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CrearProductoTool implements McpTool {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public String getName() {
        return "crearProducto";
    }

    @Override
    public McpToolDefinition getDefinition() {
        return new McpToolDefinition(getName(),
                "Crea un nuevo producto en el catálogo de CelFinder.")
                .addParam("nombre", "Nombre del producto", true)
                .addParam("marca", "Marca del producto", true)
                .addParam("categoria", "Categoría (Celular, Accesorio, etc.)", true)
                .addParam("precio", "Precio en pesos colombianos", true)
                .addParam("stock", "Cantidad disponible (default: 1)", false)
                .addParam("estado", "Estado: nuevo o usado (default: nuevo)", false)
                .addParam("descripcion", "Descripción del producto", false)
                .addParam("vendedorId", "ID del usuario vendedor (usa el del admin si se omite)", false);
    }

    @Override
    public Object execute(Map<String, String> params) {
        String nombre = params.get("nombre");
        String marca = params.get("marca");
        String categoria = params.get("categoria");
        String precioStr = params.get("precio");

        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre es requerido.");
        if (marca == null || marca.isBlank()) throw new IllegalArgumentException("marca es requerido.");
        if (categoria == null || categoria.isBlank()) throw new IllegalArgumentException("categoria es requerido.");
        if (precioStr == null || precioStr.isBlank()) throw new IllegalArgumentException("precio es requerido.");

        Producto p = new Producto();
        p.setNombre(nombre.trim());
        p.setMarca(marca.trim());
        p.setCategoria(categoria.trim());
        p.setPrecio(Float.parseFloat(precioStr));
        p.setStock(params.containsKey("stock") ? Integer.parseInt(params.get("stock")) : 1);
        p.setEstado(params.getOrDefault("estado", "nuevo"));
        p.setDescripcion(params.getOrDefault("descripcion", ""));
        p.setVendedorId(params.get("vendedorId"));
        p.setFechaPublicacion(LocalDateTime.now());
        p.setEstadoVenta(EstadoProducto.DISPONIBLE);

        mongoTemplate.save(p, "productos");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", p.getId());
        response.put("nombre", p.getNombre());
        response.put("marca", p.getMarca());
        response.put("categoria", p.getCategoria());
        response.put("precio", p.getPrecio());
        response.put("stock", p.getStock());
        response.put("estadoVenta", p.getEstadoVenta());
        response.put("mensaje", "Producto creado exitosamente en el catálogo.");
        return response;
    }

    @Override
    public String getRequiredRole() {
        return "ROLE_ADMIN";
    }
}
