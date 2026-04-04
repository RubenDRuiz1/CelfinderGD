package com.celfinder.Controller;

import com.celfinder.Model.Producto;
import com.celfinder.Procesos.ComparadorProducto;
import com.celfinder.Procesos.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/comparar")
public class ComparisonController {

    private final VentaService ventaService;
    private final ComparadorProducto comparadorProducto;

    @Autowired
    public ComparisonController(VentaService ventaService, ComparadorProducto comparadorProducto) {
        this.ventaService = ventaService;
        this.comparadorProducto = comparadorProducto;
    }

    /**
     * Muestra la página para seleccionar el primer producto a comparar.
     * @param model Modelo para pasar datos a la vista.
     * @return Nombre de la vista.
     */
    @GetMapping("/seleccionar-primero")
    public String mostrarSeleccionPrimero(Model model) {
        model.addAttribute("productos", ventaService.obtenerProductosEnVenta());
        return "seleccionarPrimero";
    }

    /**
     * Muestra la página para seleccionar el segundo producto a comparar.
     * @param idPrimerProducto ID del primer producto seleccionado.
     * @param model Modelo para pasar datos a la vista.
     * @return Nombre de la vista.
     */
    @GetMapping("/seleccionar-segundo/{idPrimerProducto}")
    public String mostrarSeleccionSegundo(@PathVariable String idPrimerProducto, Model model) {
        Producto producto1 = ventaService.obtenerProductoPorId(idPrimerProducto);
        if (producto1 == null) {
            model.addAttribute("error", "Producto no encontrado.");
            return "error";
        }
        model.addAttribute("producto1", producto1);
        model.addAttribute("productos", ventaService.buscarProductosExcluyendoId(null, null, null, null, idPrimerProducto));
        return "seleccionarSegundo";
    }

    /**
     * Realiza la comparación de los dos productos seleccionados y muestra los resultados.
     * @param idPrimerProducto ID del primer producto.
     * @param idSegundoProducto ID del segundo producto.
     * @param model Modelo para pasar datos a la vista.
     * @return Nombre de la vista.
     */
    @GetMapping("/resultado/{idPrimerProducto}/{idSegundoProducto}")
    public String mostrarResultadoComparacion(@PathVariable String idPrimerProducto, @PathVariable String idSegundoProducto, Model model) {
        Producto producto1 = ventaService.obtenerProductoPorId(idPrimerProducto);
        Producto producto2 = ventaService.obtenerProductoPorId(idSegundoProducto);

        if (producto1 == null || producto2 == null) {
            model.addAttribute("error", "Uno o ambos productos no existen.");
            return "error";
        }

        comparadorProducto.registrarProductos(producto1, producto2);
        model.addAttribute("producto1", producto1);
        model.addAttribute("producto2", producto2);
        model.addAttribute("resultados", comparadorProducto.getResultadosComparacion());
        return "resultadoComparacion";
    }
}