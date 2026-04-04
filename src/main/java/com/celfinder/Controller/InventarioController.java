package com.celfinder.Controller;

import com.celfinder.Model.Producto;
import com.celfinder.Model.Usuario;
import org.bson.types.ObjectId;   // ← IMPORT OBLIGATORIO
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/mi-inventario")
public class InventarioController {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public InventarioController(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping
    public String mostrarInventario(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión.");
            return "redirect:/login";
        }

        Usuario usuario = (Usuario) authentication.getPrincipal();

        if (!usuario.getRoles().contains("ROLE_VENDEDOR")) {
            redirectAttributes.addFlashAttribute("error", "Solo los vendedores pueden ver su inventario.");
            return "redirect:/ventas/listar";
        }

        String idUsuarioString = usuario.getId();
        System.out.println("ID del usuario logueado (String): " + idUsuarioString);

        Pageable pageable = PageRequest.of(page, size);

        Criteria searchCriteria = new Criteria().orOperator(
                Criteria.where("vendedorId").is(idUsuarioString),
                Criteria.where("vendedorId").is(new ObjectId(idUsuarioString))
        );

        Query query = new Query(searchCriteria);
        query.with(pageable);

        List<Producto> productos = mongoTemplate.find(query, Producto.class, "productos");

        long total = mongoTemplate.count(new Query(searchCriteria), "productos");

        Page<Producto> pagina = new PageImpl<>(productos, pageable, total);

        // DEBUG para que veas en consola cuántos trae
        System.out.println("¡PRODUCTOS ENCONTRADOS!: " + total);

        model.addAttribute("productos", pagina.getContent());
        model.addAttribute("page", pagina.getNumber());
        model.addAttribute("totalPages", pagina.getTotalPages());
        model.addAttribute("totalElements", pagina.getTotalElements());
        model.addAttribute("nombreVendedor", usuario.getNombreUsuario());

        return "miInventario";
    }
}