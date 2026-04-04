package com.celfinder.Procesos;

import com.celfinder.Model.*;
import com.celfinder.util.EstadoSolicitud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestor de reseñas.
 * Responsabilidad única: guardar, consultar y votar reseñas de productos.
 */
@Service
public class ReviewManager {

    private static final Logger logger = LoggerFactory.getLogger(ReviewManager.class);

    private final MongoTemplate mongoTemplate;
    private final ProductQueryService productQueryService;

    @Autowired
    public ReviewManager(MongoTemplate mongoTemplate,
                         ProductQueryService productQueryService) {
        this.mongoTemplate = mongoTemplate;
        this.productQueryService = productQueryService;
    }

    // ---------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------

    public List<Reseña> obtenerReseñasPorProducto(String productoId) {
        return mongoTemplate.find(
                new Query(Criteria.where("productoId").is(productoId)),
                Reseña.class, "reseñas");
    }

    public boolean usuarioComproProducto(String usuarioId, String productoId) {
        return mongoTemplate.exists(
                new Query(Criteria.where("usuarioId").is(usuarioId)
                        .and("productoId").is(productoId)
                        .and("estado").is(EstadoSolicitud.APROBADA)),
                Solicitud.class);
    }

    public ProductRatingStats getStats(String productoId) {
        List<Reseña> reseñas = obtenerReseñasPorProducto(productoId);
        int total = reseñas.size();
        if (total == 0) {
            Map<Integer, Double> zeroStats = new HashMap<>();
            for (int i = 1; i <= 5; i++) zeroStats.put(i, 0.0);
            return new ProductRatingStats(0.0, 0, zeroStats);
        }

        double sum = reseñas.stream().mapToInt(Reseña::getPuntuacion).sum();
        double avg = sum / total;

        Map<Integer, Long> counts = reseñas.stream()
                .collect(Collectors.groupingBy(Reseña::getPuntuacion, Collectors.counting()));

        Map<Integer, Double> percentages = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            percentages.put(i, (counts.getOrDefault(i, 0L) * 100.0) / total);
        }

        return new ProductRatingStats(avg, total, percentages);
    }

    // ---------------------------------------------------------------
    // Escritura
    // ---------------------------------------------------------------

    public void guardarReseña(Reseña reseña) {
        reseña.setFecha(LocalDateTime.now());
        mongoTemplate.save(reseña, "reseñas");
    }

    /**
     * Valida los datos del formulario, construye la reseña y la persiste.
     *
     * @return la reseña ya persistida
     */
    public Reseña crearYGuardarReseña(String productoId, Usuario usuario,
                                      String nombreUsuario, String titulo,
                                      String comentario, int puntuacion,
                                      List<String> fotos) {
        Producto producto = productQueryService.obtenerProductoPorId(productoId);
        if (producto == null) {
            throw new IllegalStateException("El producto no existe.");
        }
        if (titulo == null || titulo.trim().isEmpty()
                || comentario == null || comentario.trim().isEmpty()) {
            throw new IllegalArgumentException("Título y comentario son obligatorios.");
        }
        if (puntuacion < 1 || puntuacion > 5) {
            throw new IllegalArgumentException("La puntuación debe estar entre 1 y 5.");
        }

        Reseña reseña = new Reseña();
        reseña.setProductoId(productoId);
        reseña.setUsuarioId(usuario.getId());
        reseña.setNombreUsuario(nombreUsuario);
        reseña.setTitulo(titulo.trim());
        reseña.setComentario(comentario.trim());
        reseña.setPuntuacion(puntuacion);
        reseña.setCompraVerificada(usuarioComproProducto(usuario.getId(), productoId));
        reseña.setFotosBase64(fotos);

        guardarReseña(reseña);
        return reseña;
    }

    /**
     * Fallback for existing calls without photos.
     */
    public Reseña crearYGuardarReseña(String productoId, Usuario usuario,
                                      String nombreUsuario, String titulo,
                                      String comentario, int puntuacion) {
        return crearYGuardarReseña(productoId, usuario, nombreUsuario, titulo, comentario, puntuacion, new java.util.ArrayList<>());
    }

    /**
     * Registra un voto (útil / inútil) sobre una reseña.
     *
     * @return el productoId de la reseña para redirección
     */
    @SuppressWarnings("null")
    public String votarReseña(String reseñaId, boolean util) {
        Reseña reseña = mongoTemplate.findById(reseñaId, Reseña.class);
        if (reseña == null) {
            throw new IllegalArgumentException("Reseña no encontrada.");
        }
        if (util) {
            reseña.setVotosUtiles(reseña.getVotosUtiles() + 1);
        } else {
            reseña.setVotosInutiles(reseña.getVotosInutiles() + 1);
        }
        mongoTemplate.save(reseña);
        return reseña.getProductoId();
    }
}
