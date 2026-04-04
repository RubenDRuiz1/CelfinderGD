package com.celfinder.Controller;

import com.celfinder.Model.Solicitud;
import com.celfinder.Model.Notificacion;
import com.celfinder.Model.Producto;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.AdminService;
import com.celfinder.Procesos.VentaService;
import com.celfinder.util.ConfiguracionApp;
import com.celfinder.util.Roles;
import com.celfinder.util.TipoSolicitud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final AdminService adminService;
    private final VentaService ventaService;

    @Autowired
    public AdminController(AdminService adminService, VentaService ventaService) {
        this.adminService = adminService;
        this.ventaService = ventaService;
    }

    @GetMapping
    public String mostrarPanelAdmin() {
        return "adminPanel";
    }

    @GetMapping("/usuarios")
    public String listarUsuarios(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                Model model) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Usuario> usuariosPage = adminService.obtenerTodosLosUsuarios(pageable);
            model.addAttribute("usuarios", usuariosPage.getContent());
            model.addAttribute("currentPage", usuariosPage.getNumber());
            model.addAttribute("totalPages", usuariosPage.getTotalPages());
            model.addAttribute("totalItems", usuariosPage.getTotalElements());
            model.addAttribute("pageSize", size);
            return "listarUsuarios";
        } catch (Exception e) {
            model.addAttribute("error", "Error al listar los usuarios: " + e.getMessage());
            model.addAttribute("usuarios", new ArrayList<>());
            return "listarUsuarios";
        }
    }

    @PostMapping("/desactivar-usuario/{id}")
    public String desactivarUsuario(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            adminService.desactivarUsuario(id);
            redirectAttributes.addFlashAttribute("mensaje", "Usuario desactivado correctamente.");
            return "redirect:/admin/usuarios";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al desactivar el usuario: " + e.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    @PostMapping("/reactivar-usuario/{id}")
    public String reactivarUsuario(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            adminService.reactivarUsuario(id);
            redirectAttributes.addFlashAttribute("mensaje", "Usuario reactivado correctamente.");
            return "redirect:/admin/usuarios";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al reactivar el usuario: " + e.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    @PostMapping("/eliminar-usuario/{id}")
    public String eliminarUsuario(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            adminService.eliminarUsuario(id);
            redirectAttributes.addFlashAttribute("mensaje", "Usuario eliminado correctamente.");
            return "redirect:/admin/usuarios";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el usuario: " + e.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    @GetMapping("/productos")
    public String listarProductos(@RequestParam(required = false) String nombre,
                                 @RequestParam(required = false) String estadoVenta,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Model model) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Producto> productosPage;
            if ((nombre != null && !nombre.isEmpty()) || (estadoVenta != null && !estadoVenta.isEmpty())) {
                productosPage = adminService.buscarProductos(nombre, estadoVenta, pageable);
            } else {
                productosPage = adminService.obtenerTodosLosProductos(pageable);
            }
            model.addAttribute("productos", productosPage.getContent());
            model.addAttribute("currentPage", productosPage.getNumber());
            model.addAttribute("totalPages", productosPage.getTotalPages());
            model.addAttribute("totalItems", productosPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("adminService", adminService);
            model.addAttribute("nombre", nombre);
            model.addAttribute("estadoVenta", estadoVenta);
            return "listarProductosAdmin";
        } catch (Exception e) {
            model.addAttribute("error", "Error al listar los productos: " + e.getMessage());
            model.addAttribute("productos", new ArrayList<>());
            return "listarProductosAdmin";
        }
    }

    @PostMapping("/eliminar-producto/{id}")
    public String eliminarProducto(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            adminService.eliminarProducto(id);
            redirectAttributes.addFlashAttribute("mensaje", "Producto eliminado correctamente.");
            return "redirect:/admin/productos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar el producto: " + e.getMessage());
            return "redirect:/admin/productos";
        }
    }

    @GetMapping("/asignar-rol/{id}")
    public String mostrarFormularioAsignarRol(@PathVariable String id, Model model) {
        try {
            Usuario usuario = adminService.obtenerUsuarioPorId(id);
            if (usuario == null) {
                model.addAttribute("error", "Usuario no encontrado.");
                return "redirect:/admin/usuarios";
            }
            model.addAttribute("usuario", usuario);
            return "asignarRol";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el formulario: " + e.getMessage());
            return "redirect:/admin/usuarios";
        }
    }

    @PostMapping("/asignar-rol/{id}")
    public String asignarRol(@PathVariable String id, @RequestParam String rol, RedirectAttributes redirectAttributes) {
        try {
            adminService.asignarRol(id, rol);
            redirectAttributes.addFlashAttribute("mensaje", "Rol asignado correctamente.");
            return "redirect:/admin/usuarios";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al asignar el rol: " + e.getMessage());
            return "redirect:/admin/asignar-rol/" + id;
        }
    }

    @PostMapping("/remover-rol/{id}")
    public String removerRol(@PathVariable String id, @RequestParam String rol, RedirectAttributes redirectAttributes) {
        try {
            adminService.removerRol(id, rol);
            redirectAttributes.addFlashAttribute("mensaje", "Rol removido correctamente.");
            return "redirect:/admin/usuarios";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al remover el rol: " + e.getMessage());
            return "redirect:/admin/asignar-rol/" + id;
        }
    }

    @GetMapping("/solicitar-vendedor")
    public String mostrarFormularioSolicitudVendedor(Model model) {
        model.addAttribute("solicitud", new Solicitud());
        return "formularioSolicitudVendedor";
    }

    @PostMapping("/solicitar-vendedor")
    public String procesarSolicitudVendedor(@ModelAttribute Solicitud solicitud,
                                           Authentication authentication,
                                           RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new IllegalStateException("Debes estar autenticado para enviar una solicitud.");
            }
            Usuario usuario = (Usuario) authentication.getPrincipal();
            solicitud.setUsuarioId(usuario.getId());
            if (usuario.getRoles().contains(Roles.ROLE_VENDEDOR)) {
                throw new IllegalStateException("Ya eres vendedor.");
            }
            if (solicitud.getNombreCompleto() == null || solicitud.getNombreCompleto().trim().isEmpty()) {
                throw new IllegalArgumentException("El nombre completo es obligatorio.");
            }
            // Validación mejorada de email
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            if (solicitud.getCorreo() == null || !solicitud.getCorreo().matches(emailRegex)) {
                throw new IllegalArgumentException("El correo electrónico no es válido.");
            }
            if (solicitud.getTelefono() == null || !solicitud.getTelefono().matches("\\d{" + ConfiguracionApp.LONGITUD_TELEFONO + "}")) {
                throw new IllegalArgumentException("El número de teléfono debe tener " + ConfiguracionApp.LONGITUD_TELEFONO + " dígitos.");
            }
            if (solicitud.getDireccion() == null || solicitud.getDireccion().trim().isEmpty()) {
                throw new IllegalArgumentException("La dirección es obligatoria.");
            }
            adminService.crearSolicitudVendedor(solicitud);
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud enviada correctamente. Un administrador la revisará.");
            return "redirect:/ventas/listar";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al enviar la solicitud: " + e.getMessage());
            return "redirect:/admin/solicitar-vendedor";
        }
    }

    @GetMapping("/solicitudes-vendedor")
    public String listarSolicitudesVendedor(Model model) {
        try {
            List<Solicitud> solicitudes = adminService.obtenerSolicitudesVendedorPendientes();
            model.addAttribute("solicitudes", solicitudes != null ? solicitudes : new ArrayList<>());
            model.addAttribute("adminService", adminService);
            return "listarSolicitudesVendedor";
        } catch (Exception e) {
            model.addAttribute("error", "Error al listar las solicitudes: " + e.getMessage());
            model.addAttribute("solicitudes", new ArrayList<>());
            return "listarSolicitudesVendedor";
        }
    }

    @PostMapping("/gestionar-solicitud-vendedor/{id}")
    public String gestionarSolicitudVendedor(@PathVariable String id,
                                            @RequestParam String accion,
                                            @RequestParam(required = false) String comentarioAdmin,
                                            RedirectAttributes redirectAttributes) {
        try {
            adminService.gestionarSolicitudVendedor(id, accion, comentarioAdmin);
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud " + (accion.equals("aprobar") ? "aprobada" : "rechazada") + " correctamente.");
            return "redirect:/admin/solicitudes-vendedor";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al gestionar la solicitud: " + e.getMessage());
            return "redirect:/admin/solicitudes-vendedor";
        }
    }

    @GetMapping("/crear-notificacion")
    public String mostrarFormularioCrearNotificacion(Model model) {
        model.addAttribute("notificacion", new Notificacion());
        return "crearNotificacion";
    }

    @PostMapping("/crear-notificacion")
    public String crearNotificacion(@ModelAttribute Notificacion notificacion,
                                   @RequestParam(required = false) boolean enviarATodos,
                                   RedirectAttributes redirectAttributes) {
        try {
            if (notificacion.getMensaje() == null || notificacion.getMensaje().trim().isEmpty()) {
                throw new IllegalArgumentException("El mensaje es obligatorio.");
            }
            if (enviarATodos) {
                adminService.crearNotificacionParaTodos(notificacion.getMensaje());
                redirectAttributes.addFlashAttribute("mensaje", "Notificación enviada a todos los usuarios correctamente.");
            } else {
                if (notificacion.getUsuarioId() == null || notificacion.getUsuarioId().trim().isEmpty()) {
                    throw new IllegalArgumentException("El ID del usuario es obligatorio.");
                }
                if (adminService.obtenerUsuarioPorId(notificacion.getUsuarioId()) == null) {
                    throw new IllegalArgumentException("El usuario especificado no existe.");
                }
                adminService.crearNotificacion(notificacion);
                redirectAttributes.addFlashAttribute("mensaje", "Notificación enviada correctamente.");
            }
            return "redirect:/admin";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/crear-notificacion";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al enviar la notificación: " + e.getMessage());
            return "redirect:/admin/crear-notificacion";
        }
    }
}