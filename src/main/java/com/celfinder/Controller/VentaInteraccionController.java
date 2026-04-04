package com.celfinder.Controller;

import com.celfinder.Model.Producto;
import com.celfinder.Model.Reseña;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.ComparisonEngine;
import com.celfinder.Procesos.ProductQueryService;
import com.celfinder.Procesos.ReviewManager;
import com.celfinder.Procesos.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador de interacciones: comparaciones y reseñas.
 * Rutas: /comparar/{id}, /comparar-media/{id}, /seleccionar-comparacion/{id},
 *        /reseña/{id}, /reseña/votar/{reseñaId}
 */
@Controller
@RequestMapping("/ventas")
public class VentaInteraccionController {

    private static final Logger logger = LoggerFactory.getLogger(VentaInteraccionController.class);
    private static final int ITEMS_PER_PAGE = 20;

    private final ComparisonEngine comparisonEngine;
    private final ProductQueryService productQueryService;
    private final ReviewManager reviewManager;
    private final UsuarioService usuarioService;

    public VentaInteraccionController(ComparisonEngine comparisonEngine,
                                      ProductQueryService productQueryService,
                                      ReviewManager reviewManager,
                                      UsuarioService usuarioService) {
        this.comparisonEngine = comparisonEngine;
        this.productQueryService = productQueryService;
        this.reviewManager = reviewManager;
        this.usuarioService = usuarioService;
    }

    // ---------------------------------------------------------------
    // SELECCIONAR PRODUCTO PARA COMPARACIÓN
    // ---------------------------------------------------------------

    @GetMapping("/seleccionar-comparacion/{id}")
    public String seleccionarProductoParaComparar(@PathVariable String id,
                                                    @RequestParam(required = false) String nombre,
                                                    @RequestParam(required = false) String estado,
                                                    @RequestParam(required = false) Float precioMin,
                                                    @RequestParam(required = false) Float precioMax,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    Model model,
                                                    Authentication authentication,
                                                    RedirectAttributes redirectAttributes) {
        try {
            Producto productoActual = productQueryService.obtenerProductoPorId(id);
            if (productoActual == null) {
                redirectAttributes.addFlashAttribute("error", "El producto no existe.");
                return "redirect:/ventas/listar";
            }

            List<Producto> allProductos = productQueryService.buscarProductosExcluyendoId(
                    nombre, estado, precioMin, precioMax, id);
            int totalItems = allProductos.size();
            int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
            List<Producto> productos = productQueryService.paginarProductos(allProductos, page, ITEMS_PER_PAGE);

            model.addAttribute("producto", productoActual);
            model.addAttribute("productoActual", productoActual);
            model.addAttribute("productos", productos);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalItems", totalItems);
            model.addAttribute("nombre", nombre);
            model.addAttribute("estado", estado);
            model.addAttribute("precioMin", precioMin);
            model.addAttribute("precioMax", precioMax);

            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof Usuario) {
                Usuario usuario = (Usuario) authentication.getPrincipal();
                model.addAttribute("userRoles", usuario.getRoles());
                model.addAttribute("userId", usuario.getId());
            } else {
                model.addAttribute("userRoles", new ArrayList<>());
                model.addAttribute("userId", null);
            }

            return "seleccionarComparacion";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al cargar la selección de comparación: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    // ---------------------------------------------------------------
    // COMPARAR CON OTRO PRODUCTO
    // ---------------------------------------------------------------

    @GetMapping("/comparar/{id}")
    public String compararConOtro(@PathVariable String id,
                                   @RequestParam String idOtroProducto,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        try {
            List<String> resultados = comparisonEngine.compararConOtroProducto(id, idOtroProducto);
            model.addAttribute("resultados", resultados);
            model.addAttribute("producto1", productQueryService.obtenerProductoPorId(id));
            model.addAttribute("producto2", productQueryService.obtenerProductoPorId(idOtroProducto));
            return "resultadoComparacion";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al realizar la comparación: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    // ---------------------------------------------------------------
    // COMPARAR CON MEDIA DEL MERCADO
    // ---------------------------------------------------------------

    @GetMapping("/comparar-media/{id}")
    public String compararConMedia(@PathVariable String id,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        logger.info("Processing compararConMedia for ID: {}", id);
        try {
            List<String> resultados = comparisonEngine.compararConMedia(id);
            model.addAttribute("resultados", resultados);
            model.addAttribute("producto", productQueryService.obtenerProductoPorId(id));
            return "resultadomedia";
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument for ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        } catch (Exception e) {
            logger.error("Error during media comparison for ID {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Error al realizar la comparación con la media: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    // ---------------------------------------------------------------
    // RESEÑAS
    // ---------------------------------------------------------------

    @PostMapping("/reseña/{id}")
    public String guardarReseña(@PathVariable String id,
                                 @RequestParam String titulo,
                                 @RequestParam String comentario,
                                 @RequestParam int puntuacion,
                                 @RequestParam(value = "fotos", required = false) MultipartFile[] fotos,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = requireAuthenticated(authentication);
            String nombreUsuario = usuarioService.obtenerNombreUsuarioPorId(usuario.getId());
            
            List<String> fotosBase64 = new ArrayList<>();
            if (fotos != null) {
                for (MultipartFile foto : fotos) {
                    if (!foto.isEmpty()) {
                        fotosBase64.add(Base64.getEncoder().encodeToString(foto.getBytes()));
                    }
                }
            }
            
            reviewManager.crearYGuardarReseña(id, usuario, nombreUsuario, titulo, comentario, puntuacion, fotosBase64);
            redirectAttributes.addFlashAttribute("mensaje", "Reseña enviada correctamente.");
            return "redirect:/ventas/detalle/" + id;
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al enviar la reseña: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    // AJAX endpoint for sorting reviews
    @GetMapping("/reseñas/{id}/ordenar")
    @ResponseBody
    public List<Reseña> ordenarReseñas(@PathVariable String id, @RequestParam String criterio) {
        List<Reseña> reseñas = reviewManager.obtenerReseñasPorProducto(id);
        
        switch (criterio) {
            case "utilidad":
                reseñas.sort(Comparator.comparing(Reseña::getVotosUtiles).reversed());
                break;
            case "recientes":
                reseñas.sort(Comparator.comparing(Reseña::getFecha).reversed());
                break;
            case "antiguas":
                reseñas.sort(Comparator.comparing(Reseña::getFecha));
                break;
            default:
                break;
        }
        
        return reseñas;
    }

    @PostMapping("/reseña/votar/{reseñaId}")
    public String votarReseña(@PathVariable String reseñaId,
                               @RequestParam boolean util,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/usuarios/login";
        }
        try {
            String productoId = reviewManager.votarReseña(reseñaId, util);
            return "redirect:/ventas/detalle/" + productoId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    // ---------------------------------------------------------------
    // HELPER PRIVADO
    // ---------------------------------------------------------------

    private Usuario requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Debes estar autenticado para realizar esta acción.");
        }
        return (Usuario) authentication.getPrincipal();
    }
}
