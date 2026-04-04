package com.celfinder.Procesos;

import com.celfinder.Model.Categoria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestionar categorías dinámicas
 */
@Service
public class CategoriaService {

    private static final Logger logger = LoggerFactory.getLogger(CategoriaService.class);
    
    private final MongoTemplate mongoTemplate;

    @Autowired
    public CategoriaService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Obtener todas las categorías activas
     */
    public List<Categoria> obtenerCategoriasActivas() {
        Query query = new Query(Criteria.where("activa").is(true));
        return mongoTemplate.find(query, Categoria.class, "categorias");
    }

    /**
     * Obtener todas las categorías (incluyendo inactivas)
     */
    public List<Categoria> obtenerTodasLasCategorias() {
        return mongoTemplate.findAll(Categoria.class, "categorias");
    }

    /**
     * Obtener categoría por nombre
     */
    public Categoria obtenerCategoriaPorNombre(String nombre) {
        Query query = new Query(Criteria.where("nombre").is(nombre));
        return mongoTemplate.findOne(query, Categoria.class, "categorias");
    }

    /**
     * Obtener categoría por ID
     */
    public Categoria obtenerCategoriaPorId(String id) {
        return mongoTemplate.findById(id, Categoria.class, "categorias");
    }

    /**
     * Crear nueva categoría
     */
    public void crearCategoria(Categoria categoria) {
        // Validar que no exista ya una categoría con ese nombre
        Categoria existente = obtenerCategoriaPorNombre(categoria.getNombre());
        if (existente != null) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
        }

        categoria.setFechaCreacion(LocalDateTime.now());
        categoria.setActiva(true);
        
        mongoTemplate.save(categoria, "categorias");
        logger.info("Categoría creada: {}", categoria.getNombre());
    }

    /**
     * Actualizar categoría existente
     */
    public void actualizarCategoria(Categoria categoria) {
        if (categoria.getId() == null) {
            throw new IllegalArgumentException("El ID de la categoría es obligatorio para actualizar");
        }

        Categoria existente = obtenerCategoriaPorId(categoria.getId());
        if (existente == null) {
            throw new IllegalArgumentException("La categoría no existe");
        }

        mongoTemplate.save(categoria, "categorias");
        logger.info("Categoría actualizada: {}", categoria.getNombre());
    }

    /**
     * Activar o desactivar categoría
     */
    public void cambiarEstadoCategoria(String id, boolean activa) {
        Categoria categoria = obtenerCategoriaPorId(id);
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría no existe");
        }

        categoria.setActiva(activa);
        mongoTemplate.save(categoria, "categorias");
        logger.info("Categoría {} {}", categoria.getNombre(), activa ? "activada" : "desactivada");
    }

    /**
     * Eliminar categoría
     */
    public void eliminarCategoria(String id) {
        Categoria categoria = obtenerCategoriaPorId(id);
        if (categoria == null) {
            throw new IllegalArgumentException("La categoría no existe");
        }

        // Verificar si hay productos usando esta categoría
        Query query = new Query(Criteria.where("categoria").is(categoria.getNombre()));
        boolean tieneProductos = mongoTemplate.exists(query, "productos");
        
        if (tieneProductos) {
            throw new IllegalStateException("No se puede eliminar la categoría porque hay productos asociados");
        }

        mongoTemplate.remove(categoria, "categorias");
        logger.info("Categoría eliminada: {}", categoria.getNombre());
    }

    /**
     * Obtener categorías agrupadas por grupo
     */
    public java.util.Map<String, List<Categoria>> obtenerCategoriasAgrupadas() {
        List<Categoria> categorias = obtenerCategoriasActivas();
        java.util.Map<String, List<Categoria>> agrupadas = new java.util.HashMap<>();
        
        for (Categoria cat : categorias) {
            String grupo = cat.getGrupo() != null ? cat.getGrupo() : "Otros";
            agrupadas.computeIfAbsent(grupo, k -> new java.util.ArrayList<>()).add(cat);
        }
        
        return agrupadas;
    }
}
