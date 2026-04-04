package com.celfinder.Controller;

import com.celfinder.Model.CarritoItem;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.CarritoService;
import com.celfinder.Procesos.PurchaseService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.stream.Collectors;
import com.celfinder.Model.Carrito;
import com.celfinder.Model.HistorialCarrito;

import java.util.List;

@Controller
@RequestMapping("/ventas/carrito")
public class CarritoController {

    private final CarritoService carritoService;
    private final PurchaseService purchaseService;
    private final MongoTemplate mongoTemplate;

    public CarritoController(CarritoService carritoService, PurchaseService purchaseService, MongoTemplate mongoTemplate) {
        this.carritoService = carritoService;
        this.purchaseService = purchaseService;
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/ver")
    public String verCarrito(Authentication auth, Model model) {
        if (!isAutenticado(auth)) {
            return "redirect:/usuarios/login";
        }
        
        List<CarritoItem> items = carritoService.obtenerItems();
        model.addAttribute("productos", items);
        model.addAttribute("total", carritoService.obtenerTotal());
        model.addAttribute("contador", carritoService.getContador());
        
        return "carrito";
    }

    @PostMapping("/agregar/{id}")
    public String agregarAlCarrito(@PathVariable String id, 
                                   @RequestParam(defaultValue = "1") int cantidad,
                                   Authentication auth, 
                                   RedirectAttributes redirect) {
        if (!isAutenticado(auth)) {
            return "redirect:/usuarios/login";
        }
        
        try {
            Usuario usuario = (Usuario) auth.getPrincipal();
            // Intentar agregar como producto
            try {
                carritoService.agregarAlCarrito(usuario.getId(), id, cantidad);
                redirect.addFlashAttribute("mensaje", "Producto agregado al carrito (" + cantidad + " unidades).");
            } catch (IllegalArgumentException e) {
                // Si no es producto, intentar como combo
                carritoService.agregarComboAlCarrito(usuario.getId(), id);
                redirect.addFlashAttribute("mensaje", "Combo agregado al carrito.");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/ventas/detalle/" + id;
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarDelCarrito(@PathVariable String id, Authentication auth, RedirectAttributes redirect) {
        if (!isAutenticado(auth)) {
            return "redirect:/usuarios/login";
        }
        
        carritoService.eliminarDelCarrito(id);
        redirect.addFlashAttribute("mensaje", "Producto eliminado del carrito.");
        return "redirect:/ventas/carrito/ver";
    }

    @PostMapping("/confirmar")
    public String confirmarCarrito(Authentication auth, RedirectAttributes redirect) {
        if (!isAutenticado(auth)) {
            return "redirect:/usuarios/login";
        }
        
        List<CarritoItem> items = carritoService.obtenerItems();
        if (items.isEmpty()) {
            redirect.addFlashAttribute("error", "El carrito está vacío.");
            return "redirect:/ventas/carrito/ver";
        }
        
        Usuario usuario = (Usuario) auth.getPrincipal();
        int procesados = 0;
        
        try {
            for (CarritoItem item : items) {
                if (item.isCombo()) {
                    // Para combos, creamos una solicitud por cada producto individual
                    for (String pId : item.getProductoIds()) {
                        purchaseService.crearSolicitudDesdeCompra(
                                usuario, 
                                pId, 
                                usuario.getCiudad() + ", " + usuario.getDepartamento(), 
                                usuario.getEmail(), 
                                usuario.getTelefono() != null ? usuario.getTelefono() : "No especificado"
                        );
                    }
                } else {
                    // Producto individual
                    purchaseService.crearSolicitudDesdeCompra(
                            usuario, 
                            item.getProductoId(), 
                            usuario.getCiudad() + ", " + usuario.getDepartamento(), 
                            usuario.getEmail(), 
                            usuario.getTelefono() != null ? usuario.getTelefono() : "No especificado"
                    );
                }
                procesados++;
            }
            
            // Guardar en colecciones "carritos" e "historial_carritos"
            List<String> todosLosProductosIds = items.stream()
                .flatMap(item -> {
                    if (item.isCombo()) return item.getProductoIds().stream();
                    else return java.util.stream.Stream.of(item.getProductoId());
                })
                .collect(Collectors.toList());

            if (!todosLosProductosIds.isEmpty()) {
                Carrito carritoDb = new Carrito();
                carritoDb.setUsuarioId(usuario.getId());
                carritoDb.setProductoIds(todosLosProductosIds);
                mongoTemplate.save(carritoDb);

                HistorialCarrito historial = new HistorialCarrito();
                historial.setUsuarioId(usuario.getId());
                historial.setProductoIds(todosLosProductosIds);
                mongoTemplate.save(historial);
            }

            carritoService.limpiarCarrito();
            redirect.addFlashAttribute("mensaje", "¡Éxito! Se han generado " + procesados + " solicitudes de compra.");
            return "redirect:/ventas/historial-compras";
            
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al procesar el carrito: " + e.getMessage());
            return "redirect:/ventas/carrito/ver";
        }
    }

    private boolean isAutenticado(Authentication auth) {
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Usuario;
    }
}