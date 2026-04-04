package com.celfinder.Procesos;

import com.celfinder.Model.Combo;
import com.celfinder.Model.Producto;
import com.celfinder.Repository.ComboRepository;
import com.celfinder.util.EstadoProducto;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para la gestión de Combos de Productos.
 */
@Service
public class ComboService {

    private static final double DESCUENTO_MINIMO = 0.15; // 15%

    private final ComboRepository comboRepository;
    private final ProductQueryService productQueryService;
    private final MongoTemplate mongoTemplate;

    public ComboService(ComboRepository comboRepository, 
                        ProductQueryService productQueryService,
                        MongoTemplate mongoTemplate) {
        this.comboRepository = comboRepository;
        this.productQueryService = productQueryService;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Crea un nuevo combo validando productos y aplicando descuento.
     */
    public Combo crearCombo(String nombre, List<String> productoIds) {
        if (productoIds == null || productoIds.size() < 2) {
            throw new IllegalArgumentException("Un combo debe tener al menos 2 productos.");
        }

        double precioOriginal = 0;
        List<Producto> productos = new ArrayList<>();

        for (String id : productoIds) {
            Producto p = productQueryService.obtenerProductoPorId(id);
            if (p == null) {
                throw new IllegalArgumentException("El producto con ID " + id + " no existe.");
            }
            if (!EstadoProducto.DISPONIBLE.equals(p.getEstadoVenta())) {
                throw new IllegalStateException("El producto " + p.getNombre() + " no está disponible.");
            }
            precioOriginal += p.getPrecio();
            productos.add(p);
        }

        // Aplicar descuento del 15% obligatoriamente
        double precioCombo = precioOriginal * (1 - DESCUENTO_MINIMO);

        Combo combo = new Combo(nombre, productoIds, precioOriginal, precioCombo);
        return comboRepository.save(combo);
    }

    public List<Combo> obtenerTodos() {
        return comboRepository.findAll();
    }

    public Combo obtenerPorId(String id) {
        return comboRepository.findById(id).orElse(null);
    }

    public void eliminarCombo(String id) {
        comboRepository.deleteById(id);
    }

    /**
     * Busca combos que contengan un producto específico.
     */
    public List<Combo> obtenerCombosPorProducto(String productoId) {
        Query query = new Query(Criteria.where("productoIds").is(productoId));
        return mongoTemplate.find(query, Combo.class, "combos");
    }
}
