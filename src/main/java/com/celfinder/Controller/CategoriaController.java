package com.celfinder.Controller;

import com.celfinder.Model.Categoria;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.CategoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador para gestionar categorías dinámicas
 */
@Controller
@RequestMapping("/admin/categorias")
public class CategoriaController {

    private static final Logger logger = LoggerFactory.getLogger(CategoriaController.class);
    
    private final CategoriaService categoriaService;

    @Autowired
    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    /**
     * Listar todas las categorías
     */
    @GetMapping
    public String listarCategorias(Model model) {
        try {
            List<Categoria> categorias = categoriaService.obtenerTodasLasCategorias();
            model.addAttribute("categorias", categorias);
            return "admin/listarCategorias";
        } catch (Exception e) {
            logger.error("Error al listar categorías", e);
            model.addAttribute("error", "Error al cargar las categorías: " + e.getMessage());
            model.addAttribute("categorias", new ArrayList<>());
            return "admin/listarCategorias";
        }
    }

    /**
     * Mostrar formulario para crear nueva categoría
     */
    @GetMapping("/nueva")
    public String mostrarFormularioNuevaCategoria(Model model) {
        model.addAttribute("categoria", new Categoria());
        model.addAttribute("especificacion", new Categoria.EspecificacionCategoria());
        return "admin/crearCategoria";
    }

    /**
     * Crear nueva categoría
     */
    @PostMapping("/crear")
    public String crearCategoria(@ModelAttribute Categoria categoria,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            if (authentication != null && authentication.isAuthenticated()) {
                Usuario usuario = (Usuario) authentication.getPrincipal();
                categoria.setCreadaPor(usuario.getId());
            }
            
            categoriaService.crearCategoria(categoria);
            redirectAttributes.addFlashAttribute("mensaje", "Categoría creada exitosamente");
            return "redirect:/admin/categorias";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/categorias/nueva";
        } catch (Exception e) {
            logger.error("Error al crear categoría", e);
            redirectAttributes.addFlashAttribute("error", "Error al crear la categoría: " + e.getMessage());
            return "redirect:/admin/categorias/nueva";
        }
    }

    /**
     * Mostrar formulario para editar categoría
     */
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditarCategoria(@PathVariable String id, Model model) {
        try {
            Categoria categoria = categoriaService.obtenerCategoriaPorId(id);
            if (categoria == null) {
                model.addAttribute("error", "Categoría no encontrada");
                return "redirect:/admin/categorias";
            }
            model.addAttribute("categoria", categoria);
            return "admin/editarCategoria";
        } catch (Exception e) {
            logger.error("Error al cargar categoría", e);
            model.addAttribute("error", "Error al cargar la categoría: " + e.getMessage());
            return "redirect:/admin/categorias";
        }
    }

    /**
     * Actualizar categoría
     */
    @PostMapping("/actualizar")
    public String actualizarCategoria(@ModelAttribute Categoria categoria,
                                     RedirectAttributes redirectAttributes) {
        try {
            categoriaService.actualizarCategoria(categoria);
            redirectAttributes.addFlashAttribute("mensaje", "Categoría actualizada exitosamente");
            return "redirect:/admin/categorias";
        } catch (Exception e) {
            logger.error("Error al actualizar categoría", e);
            redirectAttributes.addFlashAttribute("error", "Error al actualizar la categoría: " + e.getMessage());
            return "redirect:/admin/categorias/editar/" + categoria.getId();
        }
    }

    /**
     * Cambiar estado de categoría (activar/desactivar)
     */
    @PostMapping("/cambiar-estado/{id}")
    public String cambiarEstadoCategoria(@PathVariable String id,
                                        @RequestParam boolean activa,
                                        RedirectAttributes redirectAttributes) {
        try {
            categoriaService.cambiarEstadoCategoria(id, activa);
            redirectAttributes.addFlashAttribute("mensaje", "Estado de categoría actualizado");
            return "redirect:/admin/categorias";
        } catch (Exception e) {
            logger.error("Error al cambiar estado de categoría", e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/categorias";
        }
    }

    /**
     * Eliminar categoría
     */
    @PostMapping("/eliminar/{id}")
    public String eliminarCategoria(@PathVariable String id,
                                   RedirectAttributes redirectAttributes) {
        try {
            categoriaService.eliminarCategoria(id);
            redirectAttributes.addFlashAttribute("mensaje", "Categoría eliminada exitosamente");
            return "redirect:/admin/categorias";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/categorias";
        } catch (Exception e) {
            logger.error("Error al eliminar categoría", e);
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la categoría: " + e.getMessage());
            return "redirect:/admin/categorias";
        }
    }

    /**
     * API para obtener especificaciones de una categoría (AJAX)
     */
    @GetMapping("/api/especificaciones/{categoriaId}")
    @ResponseBody
    public List<Categoria.EspecificacionCategoria> obtenerEspecificacionesCategoria(@PathVariable String categoriaId) {
        try {
            Categoria categoria = categoriaService.obtenerCategoriaPorId(categoriaId);
            if (categoria != null) {
                return categoria.getEspecificaciones();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error al obtener especificaciones", e);
            return new ArrayList<>();
        }
    }

    /**
     * API para obtener especificaciones por nombre de categoría (AJAX)
     */
    @GetMapping("/api/especificaciones-nombre/{nombre}")
    @ResponseBody
    public List<Categoria.EspecificacionCategoria> obtenerEspecificacionesPorNombre(@PathVariable String nombre) {
        try {
            Categoria categoria = categoriaService.obtenerCategoriaPorNombre(nombre);
            if (categoria != null) {
                return categoria.getEspecificaciones();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error al obtener especificaciones", e);
            return new ArrayList<>();
        }
    }
}
