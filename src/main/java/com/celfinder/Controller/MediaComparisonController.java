package com.celfinder.Controller;

import com.celfinder.Model.Producto;
import com.celfinder.Procesos.ComparadorProducto;
import com.celfinder.Procesos.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/comparar/media")
public class MediaComparisonController {

    private final VentaService ventaService;
    private final ComparadorProducto comparadorProducto;

    @Autowired
    public MediaComparisonController(VentaService ventaService, ComparadorProducto comparadorProducto) {
        this.ventaService = ventaService;
        this.comparadorProducto = comparadorProducto;
    }

    /**
     * Inicia la comparación de un producto con la media desde el detalle del producto.
     * @param idProducto ID del producto a comparar.
     * @param model Modelo para pasar datos a la vista.
     * @return Nombre de la vista.
     */
    @GetMapping("/detalle/{idProducto}")
    public String iniciarComparacionConMedia(@PathVariable String idProducto, Model model) {
        Producto producto = ventaService.obtenerProductoPorId(idProducto);
        if (producto == null) {
            model.addAttribute("error", "Producto no encontrado.");
            return "error";
        }

        // Simular un "producto promedio" basado en otros productos similares
        Producto productoPromedio = crearProductoPromedio(idProducto);
        if (productoPromedio == null) {
            model.addAttribute("error", "No hay suficientes datos para calcular la media.");
            return "error";
        }

        // Realizar la comparación con ComparadorProducto
        comparadorProducto.registrarProductos(producto, productoPromedio);
        model.addAttribute("producto", producto);
        model.addAttribute("productoPromedio", productoPromedio);
        model.addAttribute("resultados", comparadorProducto.getResultadosComparacion());
        return "resultadomedia";
    }

    /**
     * Crea un "producto promedio" basado en la media de otros productos similares.
     * @param idProductoExcluir ID del producto a excluir del cálculo.
     * @return Producto simulado con valores promedio.
     */
    private Producto crearProductoPromedio(String idProductoExcluir) {
        List<Producto> productosSimilares = ventaService.obtenerProductosEnVenta();
        if (productosSimilares == null || productosSimilares.isEmpty()) {
            return null;
        }

        // Excluir el producto actual del cálculo
        productosSimilares.removeIf(p -> p.getId().equals(idProductoExcluir));

        if (productosSimilares.isEmpty()) {
            return null;
        }

        // Calcular promedios simples (puedes ajustar según las características)
        double precioPromedio = productosSimilares.stream()
                .mapToDouble(Producto::getPrecio)
                .average()
                .orElse(0.0);
        String nombrePromedio = "Producto Promedio (" + productosSimilares.size() + " similares)";
        String estadoPromedio = "disponible"; // Asumimos que todos están disponibles
        String descripcionPromedio = "Promedio de productos similares en el mercado al 25/09/2025";

        Producto productoPromedio = new Producto();
        productoPromedio.setNombre(nombrePromedio);
        productoPromedio.setPrecio((float) precioPromedio);
        productoPromedio.setEstadoVenta(estadoPromedio);
        productoPromedio.setDescripcion(descripcionPromedio);
        // Otros campos (marca, categoría) se pueden dejar como N/A o calcular según necesidad

        return productoPromedio;
    }
}