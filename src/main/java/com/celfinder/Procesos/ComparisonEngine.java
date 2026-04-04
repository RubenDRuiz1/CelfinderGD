package com.celfinder.Procesos;

import com.celfinder.Model.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Motor de comparación de productos.
 * Responsabilidad única: comparar un producto con otro o con la media del mercado.
 */
@Service
public class ComparisonEngine {

    private static final Logger logger = LoggerFactory.getLogger(ComparisonEngine.class);

    private final ProductQueryService productQueryService;
    private final ComparadorProducto comparadorProducto;

    @Autowired
    public ComparisonEngine(ProductQueryService productQueryService,
                            ComparadorProducto comparadorProducto) {
        this.productQueryService = productQueryService;
        this.comparadorProducto = comparadorProducto;
    }

    // ---------------------------------------------------------------
    // Comparación directa entre dos productos
    // ---------------------------------------------------------------

    public List<String> compararConOtroProducto(String id, String idOtroProducto) {
        Producto producto1 = productQueryService.obtenerProductoPorId(id);
        Producto producto2 = productQueryService.obtenerProductoPorId(idOtroProducto);
        if (producto1 == null || producto2 == null) {
            throw new IllegalArgumentException("Uno o ambos productos no existen.");
        }
        return comparadorProducto.compararConAI(producto1, producto2);
    }

    // ---------------------------------------------------------------
    // Comparación con la media del mercado
    // ---------------------------------------------------------------

    public List<String> compararConMedia(String id) {
        Producto producto = productQueryService.obtenerProductoPorId(id);
        if (producto == null) {
            throw new IllegalArgumentException("El producto no existe.");
        }

        List<Producto> todosProductos = productQueryService.obtenerProductosEnVenta();
        if (todosProductos.isEmpty()) {
            throw new IllegalStateException("No hay productos disponibles para calcular la media.");
        }

        double sumPrecio = 0;
        StringBuilder descripciones = new StringBuilder();
        int count = todosProductos.size();

        for (Producto p : todosProductos) {
            sumPrecio += p.getPrecio();
            if (p.getDescripcion() != null) {
                descripciones.append(p.getDescripcion()).append("; ");
            }
        }

        Producto media = new Producto();
        media.setNombre("Producto Media");
        media.setMarca("Media");
        media.setCategoria(producto.getCategoria());
        media.setPrecio((float) (sumPrecio / count));
        media.setEstadoVenta("disponible");
        media.setDescripcion(descripciones.length() > 0
                ? descripciones.toString()
                : "Promedio de productos disponibles.");

        return comparadorProducto.compararConAI(producto, media);
    }
}
