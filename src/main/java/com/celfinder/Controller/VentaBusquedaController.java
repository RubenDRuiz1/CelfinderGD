package com.celfinder.Controller;

import com.celfinder.Model.*;
import com.celfinder.Procesos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controlador de catálogo y búsqueda de productos.
 * Rutas: /listar, /buscar, /detalle/{id}
 */
@Controller
@RequestMapping("/ventas")
public class VentaBusquedaController {

    private static final Logger logger = LoggerFactory.getLogger(VentaBusquedaController.class);
    private static final int ITEMS_PER_PAGE = 20;

    private final ProductQueryService productQueryService;
    private final ProductActionService productActionService;
    private final RecomendacionService recomendacionService;
    private final ReviewManager reviewManager;
    private final UsuarioService usuarioService;
    private final ComboService comboService;
    private final PurchaseService purchaseService;

    public VentaBusquedaController(ProductQueryService productQueryService,
                                   ProductActionService productActionService,
                                   RecomendacionService recomendacionService,
                                   ReviewManager reviewManager,
                                   UsuarioService usuarioService,
                                   ComboService comboService,
                                   PurchaseService purchaseService) {
        this.productQueryService = productQueryService;
        this.productActionService = productActionService;
        this.recomendacionService = recomendacionService;
        this.reviewManager = reviewManager;
        this.usuarioService = usuarioService;
        this.comboService = comboService;
        this.purchaseService = purchaseService;
    }

    // ---------------------------------------------------------------
    // LISTAR
    // ---------------------------------------------------------------

    @GetMapping("/listar")
    public String listarProductos(@RequestParam(defaultValue = "0") int page,
                                   Model model,
                                   Authentication authentication) {
        return manejarProductos(null, null, null, null, page, model, authentication);
    }

    // ---------------------------------------------------------------
    // BUSCAR
    // ---------------------------------------------------------------

    @GetMapping("/buscar")
    public String buscarProductos(@RequestParam(required = false) String nombre,
                                   @RequestParam(required = false) String estado,
                                   @RequestParam(required = false) Float precioMin,
                                   @RequestParam(required = false) Float precioMax,
                                   @RequestParam(defaultValue = "0") int page,
                                   Model model,
                                   Authentication authentication) {
        return manejarProductos(nombre, estado, precioMin, precioMax, page, model, authentication);
    }

    // ---------------------------------------------------------------
    // DETALLE
    // ---------------------------------------------------------------

    @GetMapping("/detalle/{id}")
    public String mostrarDetalleProducto(@PathVariable String id,
                                          Model model,
                                          Authentication authentication) {
        try {
            Producto producto = productQueryService.obtenerProductoPorId(id);
            if (producto == null) {
                model.addAttribute("error", "El producto no existe.");
                return "redirect:/ventas/listar";
            }
            model.addAttribute("producto", producto);

            List<Producto> otrosProductos = productQueryService.obtenerProductosEnVenta();
            model.addAttribute("otrosProductos", otrosProductos != null ? otrosProductos : new ArrayList<>());

            List<Producto> similares = productQueryService.obtenerProductosSimilares(id, producto.getNombre());
            if (similares != null && !similares.isEmpty()) {
                Collections.shuffle(similares);
                similares = similares.subList(0, Math.min(similares.size(), 5));
            }

            Map<String, String> nombresVendedores = new HashMap<>();
            for (Producto similar : similares) {
                String nombre = usuarioService.obtenerNombreUsuarioPorId(similar.getVendedorId());
                nombresVendedores.put(similar.getId(), nombre != null ? nombre : "Desconocido");
            }

            model.addAttribute("similares", similares != null ? similares : new ArrayList<>());
            model.addAttribute("nombresVendedores", nombresVendedores);

            List<Reseña> reseñas = reviewManager.obtenerReseñasPorProducto(id);
            model.addAttribute("reseñas", reseñas != null ? reseñas : new ArrayList<>());
            model.addAttribute("stats", reviewManager.getStats(id));

            model.addAttribute("nombreVendedor",
                    usuarioService.obtenerNombreUsuarioPorId(producto.getVendedorId()));

            // Check if current user has purchased the product (for review button)
            boolean userHasPurchased = false;
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof Usuario) {
                Usuario usuario = (Usuario) authentication.getPrincipal();
                userHasPurchased = reviewManager.usuarioComproProducto(usuario.getId(), id);
            }
            model.addAttribute("userHasPurchased", userHasPurchased);

            // Obtener combos recomendados que incluyen este producto
            List<Combo> combosRecomendados = comboService.obtenerCombosPorProducto(id);
            model.addAttribute("combosRecomendados", combosRecomendados != null ? combosRecomendados : new ArrayList<>());

            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof Usuario) {
                Usuario usuario = (Usuario) authentication.getPrincipal();
                model.addAttribute("userRoles", usuario.getRoles());
                model.addAttribute("userId", usuario.getId());
                productActionService.registrarVisualizacion(usuario.getId(), id);
            } else {
                model.addAttribute("userRoles", new ArrayList<>());
                model.addAttribute("userId", null);
            }

            return "detalleProducto";

        } catch (Exception e) {
            model.addAttribute("error", "Error al mostrar los detalles del producto: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    @GetMapping("/combo/{id}")
    public String mostrarDetalleCombo(@PathVariable String id,
                                       Model model,
                                       Authentication authentication) {
        try {
            Combo combo = comboService.obtenerPorId(id);
            if (combo == null) {
                model.addAttribute("error", "El combo no existe.");
                return "redirect:/ventas/listar";
            }
            model.addAttribute("combo", combo);

            List<Producto> productos = new ArrayList<>();
            for (String pId : combo.getProductoIds()) {
                Producto p = productQueryService.obtenerProductoPorId(pId);
                if (p != null) {
                    productos.add(p);
                }
            }
            model.addAttribute("productos", productos);

            // Ahorro calculado
            double ahorro = combo.getPrecioOriginal() - combo.getPrecioCombo();
            double porcentajeAhorro = (ahorro / combo.getPrecioOriginal()) * 100;
            model.addAttribute("ahorro", ahorro);
            model.addAttribute("porcentajeAhorro", Math.round(porcentajeAhorro));

            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof Usuario) {
                Usuario usuario = (Usuario) authentication.getPrincipal();
                model.addAttribute("userId", usuario.getId());
            }

            return "detalleCombo";

        } catch (Exception e) {
            model.addAttribute("error", "Error al mostrar los detalles del combo: " + e.getMessage());
            return "redirect:/ventas/listar";
        }
    }

    // ---------------------------------------------------------------
    // HELPER PRIVADO: lógica unificada listado/búsqueda con paginación
    // ---------------------------------------------------------------

    private String manejarProductos(String nombre, String estado, Float precioMin, Float precioMax,
                                     int page, Model model, Authentication authentication) {
        try {
            List<Producto> todos;
            if (nombre == null && estado == null && precioMin == null && precioMax == null) {
                todos = productQueryService.obtenerProductosEnVenta();
            } else {
                todos = productQueryService.buscarProductos(nombre, estado, precioMin, precioMax);
            }
            todos.sort(Comparator.comparing(Producto::getFechaPublicacion,
                    Comparator.reverseOrder()));

            int totalItems = todos.size();
            int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
            List<Producto> productos = productQueryService.paginarProductos(todos, page, ITEMS_PER_PAGE);

            model.addAttribute("productos", productos);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalItems", totalItems);
            model.addAttribute("nombre", nombre);
            model.addAttribute("estado", estado);
            model.addAttribute("precioMin", precioMin);
            model.addAttribute("precioMax", precioMax);

            // Cargar combos para el catálogo principal
            List<Combo> todosLosCombos = comboService.obtenerTodos();
            model.addAttribute("combos", todosLosCombos);

            // Get purchased product IDs for the current user to show "Ya lo compraste" badges
            Set<String> purchasedProductIds = new HashSet<>();
            List<Producto> recomendados = new ArrayList<>();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof Usuario) {
                Usuario usuario = (Usuario) authentication.getPrincipal();
                List<Solicitud> historial = purchaseService.obtenerHistorialCompras(usuario.getId());
                for (Solicitud s : historial) {
                    if (com.celfinder.util.EstadoSolicitud.APROBADA.equals(s.getEstado()) || com.celfinder.util.EstadoSolicitud.AUTORIZADA.equals(s.getEstado())) {
                        purchasedProductIds.add(s.getProductoId());
                    }
                }
                model.addAttribute("userRoles", usuario.getRoles());
                model.addAttribute("productosVendedor",
                        productQueryService.obtenerProductosPorVendedor(usuario.getId()));
                recomendados = recomendacionService.recomendarParaUsuario(usuario.getId());
            }
            model.addAttribute("purchasedProductIds", purchasedProductIds);
            model.addAttribute("recomendados", recomendados);

            return "listarProductos";

        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar productos: " + e.getMessage());
            return "listarProductos";
        }
    }
}
