package com.celfinder.Controller;

import com.celfinder.Model.Producto;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.ComboService;
import com.celfinder.Procesos.ProductQueryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/combos")
public class ComboController {

    private final ComboService comboService;
    private final ProductQueryService productQueryService;

    public ComboController(ComboService comboService, ProductQueryService productQueryService) {
        this.comboService = comboService;
        this.productQueryService = productQueryService;
    }

    @GetMapping("/ver")
    public String listarCombos(Model model, Authentication auth) {
        if (!isAdminOrSeller(auth)) return "redirect:/";
        
        model.addAttribute("combos", comboService.obtenerTodos());
        model.addAttribute("productos", productQueryService.obtenerProductosEnVenta());
        return "verCombos";
    }

    @GetMapping("/crear")
    public String mostrarFormCrear(Model model, Authentication auth) {
        if (!isAdminOrSeller(auth)) return "redirect:/";
        
        List<Producto> productos = productQueryService.obtenerProductosEnVenta();
        model.addAttribute("productos", productos);
        return "crearCombo"; // I'll merge this into verCombos or a separate one
    }

    @PostMapping("/crear")
    public String crearCombo(@RequestParam String nombre, 
                             @RequestParam List<String> productoIds,
                             Authentication auth,
                             RedirectAttributes redirect) {
        if (!isAdminOrSeller(auth)) return "redirect:/";
        
        try {
            comboService.crearCombo(nombre, productoIds);
            redirect.addFlashAttribute("mensaje", "¡Combo '" + nombre + "' creado con éxito!");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al crear combo: " + e.getMessage());
        }
        return "redirect:/admin/combos/ver";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarCombo(@PathVariable String id, Authentication auth, RedirectAttributes redirect) {
        if (!isAdminOrSeller(auth)) return "redirect:/";
        
        comboService.eliminarCombo(id);
        redirect.addFlashAttribute("mensaje", "Combo eliminado.");
        return "redirect:/admin/combos/ver";
    }

    private boolean isAdminOrSeller(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Usuario)) return false;
        Usuario u = (Usuario) auth.getPrincipal();
        return u.getRoles().contains("ROLE_ADMIN") || u.getRoles().contains("ROLE_VENDEDOR");
    }
}
