package com.celfinder.Controller;

import com.celfinder.Model.Producto;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.CategoriaService;
import com.celfinder.Procesos.ProductActionService;
import com.celfinder.Procesos.ProductQueryService;
import com.celfinder.util.ImagenUtil;
import com.celfinder.util.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Map;

/**
 * Controlador de gestión de productos (CRUD de vendedor).
 * Rutas: /publicar, /editar/{id}, /eliminar/{id}
 */
@Controller
@RequestMapping("/ventas")
public class VentaGestionController {

    private static final Logger logger = LoggerFactory.getLogger(VentaGestionController.class);

    private final ProductQueryService productQueryService;
    private final ProductActionService productActionService;
    private final CategoriaService categoriaService;
    private final ImagenUtil imagenUtil;

    public VentaGestionController(ProductQueryService productQueryService,
                                  ProductActionService productActionService,
                                  CategoriaService categoriaService,
                                  ImagenUtil imagenUtil) {
        this.productQueryService = productQueryService;
        this.productActionService = productActionService;
        this.categoriaService = categoriaService;
        this.imagenUtil = imagenUtil;
    }

    // ---------------------------------------------------------------
    // PUBLICAR
    // ---------------------------------------------------------------

    @GetMapping("/publicar")
    public String mostrarFormularioPublicacion(Model model,
                                               Authentication authentication,
                                               RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated() || !hasRequiredRole(authentication)) {
            redirectAttributes.addFlashAttribute("error",
                    "Solo los vendedores o administradores pueden publicar productos.");
            return "redirect:/ventas/listar";
        }
        model.addAttribute("producto", new Producto());
        model.addAttribute("categoriasAgrupadas", categoriaService.obtenerCategoriasAgrupadas());
        return "publicarproducto";
    }

    @PostMapping("/publicar")
    public String publicarProducto(@ModelAttribute Producto producto,
                                    @RequestParam("imagen") MultipartFile imagen,
                                    @RequestParam(value = "imagenesAdicionales", required = false) MultipartFile[] imagenesAdicionales,
                                    @RequestParam Map<String, String> allParams,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre del producto es obligatorio.");
            }
            if (producto.getPrecio() < 30000) {
                throw new IllegalArgumentException("El precio mínimo de publicación es de $30.000 COP.");
            }
            if (producto.getStock() < 0) {
                throw new IllegalArgumentException("El stock no puede ser negativo.");
            }
            if (producto.getEstado() == null || producto.getEstado().trim().isEmpty()) {
                throw new IllegalArgumentException("El estado del producto es obligatorio.");
            }

            // Asegurar que no traiga un ID previo si es una publicación nueva de este formulario
            producto.setId(null); 
            Usuario vendedor = requireAuthenticatedWithRole(authentication);
            producto.setVendedorId(vendedor.getId());

            if (imagenUtil.tieneContenido(imagen)) {
                producto.setImagenBase64(imagenUtil.convertirABase64(imagen));
            }

            logger.info("Intentando publicar producto: {} - Precio: {} - Vendedor: {}", 
                    producto.getNombre(), producto.getPrecio(), vendedor.getId());

            boolean isStacked = productActionService.prepararYPublicarProducto(producto, vendedor, allParams);
            if (isStacked) {
                redirectAttributes.addFlashAttribute("mensaje", "¡Stock actualizado! El producto ya existía en tu inventario y hemos sumado las nuevas unidades.");
            } else {
                redirectAttributes.addFlashAttribute("mensaje", "Producto publicado correctamente.");
            }
            return "redirect:/ventas/listar";

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar la imagen: " + e.getMessage());
            return "redirect:/ventas/publicar";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/publicar";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error inesperado al publicar el producto: " + e.getMessage());
            return "redirect:/ventas/publicar";
        }
    }

    // ---------------------------------------------------------------
    // EDITAR
    // ---------------------------------------------------------------

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicion(@PathVariable String id,
                                            Model model,
                                            Authentication authentication,
                                            RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = requireAuthenticated(authentication);

            Producto producto = productQueryService.obtenerProductoPorId(id);
            if (producto == null) {
                redirectAttributes.addFlashAttribute("error", "El producto no existe.");
                return "redirect:/ventas/listar";
            }
            if (!usuario.getId().equals(producto.getVendedorId())
                    && !usuario.getRoles().contains("ROLE_ADMIN")) {
                redirectAttributes.addFlashAttribute("error",
                        "Solo el vendedor o un administrador pueden editar este producto.");
                return "redirect:/ventas/detalle/" + id;
            }

            model.addAttribute("producto", producto);
            return "editarProducto";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al cargar el formulario de edición: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    @PostMapping("/editar/{id}")
    public String editarProducto(@PathVariable String id,
                                  @ModelAttribute("producto") Producto producto,
                                  @RequestParam(value = "imagen", required = false) MultipartFile imagen,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = requireAuthenticated(authentication);
            
            logger.debug("Recibida petición de editar producto ID: {}. Imagen size: {}", 
                          id, (imagen != null ? imagen.getSize() : "NULL"));

            String imagenBase64 = null;
            if (imagenUtil.tieneContenido(imagen)) {
                imagenBase64 = imagenUtil.convertirABase64(imagen);
            }

            productActionService.prepararYActualizarProducto(id, producto, usuario, imagenBase64);
            redirectAttributes.addFlashAttribute("mensaje", "Producto actualizado exitosamente.");
            return "redirect:/ventas/detalle/" + id;

        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Error al procesar la imagen: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al editar el producto: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    // ---------------------------------------------------------------
    // ELIMINAR
    // ---------------------------------------------------------------

    @PostMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable String id,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = requireAuthenticated(authentication);
            Producto producto = productQueryService.obtenerProductoPorId(id);

            if (producto == null) {
                throw new IllegalStateException("El producto no existe.");
            }
            if (!usuario.getId().equals(producto.getVendedorId())
                    && !usuario.getRoles().contains("ROLE_ADMIN")) {
                throw new IllegalStateException(
                        "Solo el vendedor o un administrador pueden eliminar este producto.");
            }

            productActionService.eliminarProducto(id);
            redirectAttributes.addFlashAttribute("mensaje", "Producto eliminado correctamente.");
            return "redirect:/ventas/listar";

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Error al eliminar el producto: " + e.getMessage());
            return "redirect:/ventas/detalle/" + id;
        }
    }

    // ---------------------------------------------------------------
    // HELPERS PRIVADOS
    // ---------------------------------------------------------------

    private Usuario requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Debes estar autenticado para realizar esta acción.");
        }
        return (Usuario) authentication.getPrincipal();
    }

    private Usuario requireAuthenticatedWithRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || !hasRequiredRole(authentication)) {
            throw new IllegalStateException(
                    "Solo los vendedores o administradores pueden realizar esta acción.");
        }
        return (Usuario) authentication.getPrincipal();
    }

    private boolean hasRequiredRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(r -> r.getAuthority().equals(Roles.ROLE_VENDEDOR)
                            || r.getAuthority().equals(Roles.ROLE_ADMIN));
    }
}
