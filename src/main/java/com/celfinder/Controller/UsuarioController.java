package com.celfinder.Controller;

import com.celfinder.Model.Producto;
import com.celfinder.Model.Usuario;
import com.celfinder.Procesos.UsuarioService;
import com.celfinder.Procesos.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UsuarioController.class);
    private final UsuarioService usuarioService;
    private final VentaService ventaService;
    private final SecurityContextRepository securityContextRepository;

    @Autowired
    public UsuarioController(UsuarioService usuarioService, VentaService ventaService, SecurityContextRepository securityContextRepository) {
        this.usuarioService = usuarioService;
        this.ventaService = ventaService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "Register";
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        return "Login";
    }

    @PostMapping("/registrar")
    public String registrarUsuario(@ModelAttribute Usuario usuario,
                                 @RequestParam("email1") String email1,
                                 @RequestParam("email2") String email2,
                                 @RequestParam("fechaNacimiento") String fechaNacimiento,
                                 @RequestParam("ciudad") String ciudad,
                                 @RequestParam("departamento") String departamento,
                                 @RequestParam("telefono") String telefono,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (!email1.equals(email2)) {
                redirectAttributes.addFlashAttribute("mensaje", "Los correos electrónicos no coinciden");
                return "redirect:/usuarios/registro";
            }

            if (usuarioService.nombreUsuarioExiste(usuario.getNombreUsuario())) {
                redirectAttributes.addFlashAttribute("mensaje", "El nombre de usuario ya está en uso");
                return "redirect:/usuarios/registro";
            }

            usuario.setEmail(email1);
            
            try {
                usuario.setFechaNacimiento(LocalDate.parse(fechaNacimiento));
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("mensaje", "Formato de fecha de nacimiento inválido");
                return "redirect:/usuarios/registro";
            }

            usuario.setCiudad(ciudad);
            usuario.setDepartamento(departamento);
            usuario.setTelefono(telefono);

            usuarioService.registrarUsuario(usuario);
            redirectAttributes.addFlashAttribute("mensaje", "Usuario registrado correctamente. Ya puedes iniciar sesión.");
            return "redirect:/usuarios/login";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("mensaje", e.getMessage());
            return "redirect:/usuarios/registro";
        } catch (Exception e) {
            System.err.println("Error en registro: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensaje", "Error inesperado durante el registro: " + e.getMessage());
            return "redirect:/usuarios/registro";
        }
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/usuarios/login";
        }
        String nombreUsuario = principal.getName();
        Usuario usuario = usuarioService.obtenerUsuarioPorNombre(nombreUsuario);
        if (usuario == null) {
            return "redirect:/usuarios/login";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("userRoles", usuario.getRoles());
        return "perfil";
    }

    @GetMapping("/perfil/{id}")
    public String mostrarPerfilPorId(@PathVariable String id, Model model, Principal principal) {
        Usuario usuarioMostrado = usuarioService.obtenerUsuarioPorId(id);
        if (usuarioMostrado == null) {
            model.addAttribute("error", "Usuario no encontrado.");
            return "redirect:/ventas/listar";
        }

        model.addAttribute("usuario", usuarioMostrado);
        model.addAttribute("userRoles", usuarioMostrado.getRoles());

        // Si el usuario mostrado es un vendedor o administrador, obtener sus productos en venta
        if (usuarioMostrado.getRoles().contains("ROLE_VENDEDOR") || usuarioMostrado.getRoles().contains("ROLE_ADMIN")) {
            List<Producto> productosEnVenta = ventaService.obtenerProductosPorVendedor(usuarioMostrado.getId());
            model.addAttribute("productosEnVenta", productosEnVenta);
        }

        // Datos del usuario autenticado para la lógica de visualización
        if (principal != null) {
            Usuario authenticatedUser = usuarioService.obtenerUsuarioPorNombre(principal.getName());
            if (authenticatedUser != null) {
                model.addAttribute("currentUserId", authenticatedUser.getId());
                model.addAttribute("currentUserRoles", authenticatedUser.getRoles());
            }
        } else {
            model.addAttribute("currentUserId", null);
            model.addAttribute("currentUserRoles", new java.util.HashSet<>());
        }

        return "perfilPublico";
    }

    @GetMapping("/editar-perfil")
    public String mostrarEditarPerfil(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/usuarios/login";
        }
        String nombreUsuario = principal.getName();
        Usuario usuario = usuarioService.obtenerUsuarioPorNombre(nombreUsuario);
        if (usuario == null) {
            return "redirect:/usuarios/login";
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("userRoles", usuario.getRoles());
        return "editarPerfil";
    }

    @PostMapping("/editar-perfil")
    public String guardarCambiosPerfil(@RequestParam("email") String email,
                                      @RequestParam("ciudad") String ciudad,
                                      @RequestParam("departamento") String departamento,
                                      @RequestParam("telefono") String telefono,
                                      @RequestParam("imagenPerfil") MultipartFile imagenPerfil,
                                      @RequestParam("imagenFondo") MultipartFile imagenFondo,
                                      Principal principal,
                                      jakarta.servlet.http.HttpServletRequest request,
                                      jakarta.servlet.http.HttpServletResponse response,
                                      RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/usuarios/login";
        }
        String nombreUsuario = principal.getName();
        Usuario usuario = usuarioService.obtenerUsuarioPorNombre(nombreUsuario);
        if (usuario == null) {
            logger.error("No se encontró el usuario en la DB para editar: {}", nombreUsuario);
            return "redirect:/usuarios/login";
        }

        logger.info("Guardando perfil para: {}. ID en DB: {}", nombreUsuario, usuario.getId());
        logger.info("Datos recibidos -> Email: {}, Ciudad: {}, Dept: {}, Tel: {}", email, ciudad, departamento, telefono);

        // Manejar imagen de perfil
        String imgPerfilStr = usuario.getImagenPerfil();
        if (!imagenPerfil.isEmpty()) {
            try {
                String encoded = Base64.getEncoder().encodeToString(imagenPerfil.getBytes());
                imgPerfilStr = "data:" + imagenPerfil.getContentType() + ";base64," + encoded;
                logger.debug("Nueva imagen de perfil recibida ({} bytes)", encoded.length());
            } catch (Exception e) {
                logger.error("Error al procesar imagen perfil: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("mensaje", "Error al procesar la imagen de perfil");
                return "redirect:/usuarios/editar-perfil";
            }
        }

        // Manejar imagen de fondo
        String fondoPerfilStr = usuario.getFondoPerfil();
        if (!imagenFondo.isEmpty()) {
            try {
                String encoded = Base64.getEncoder().encodeToString(imagenFondo.getBytes());
                fondoPerfilStr = "data:" + imagenFondo.getContentType() + ";base64," + encoded;
                logger.debug("Nueva imagen de fondo recibida ({} bytes)", encoded.length());
            } catch (Exception e) {
                logger.error("Error al procesar imagen fondo: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("mensaje", "Error al procesar la imagen de fondo");
                return "redirect:/usuarios/editar-perfil";
            }
        }

        // Actualizar perfil del usuario en la base de datos
        try {
            usuarioService.actualizarPerfil(usuario, email, ciudad, departamento, telefono, imgPerfilStr, fondoPerfilStr);
            logger.info("Perfil guardado exitosamente en MongoDB para: {}", nombreUsuario);
        } catch (Exception e) {
            logger.error("Error crítico al guardar perfil en MongoDB: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error técnico al guardar en base de datos");
            return "redirect:/usuarios/editar-perfil";
        }
        
        // RE-FETCH: Obtener el usuario actualizado de la DB para la sesión
        Usuario usuarioActualizado = usuarioService.obtenerUsuarioPorNombre(nombreUsuario);
        
        // Refrescar la sesión de Spring Security para que los cambios se vean en la navbar
        actualizarSesion(usuarioActualizado, request, response);
        
        redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado correctamente");
        return "redirect:/usuarios/perfil";
    }

    /**
     * Actualiza el objeto de autenticación en el SecurityContext para reflejar cambios
     * en tiempo real (como la nueva foto de perfil) en la navbar.
     */
    private void actualizarSesion(Usuario usuarioActualizado, jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null) {
            // Creamos un nuevo token con el usuario actualizado pero manteniendo las mismas credenciales y roles
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                usuarioActualizado, 
                auth.getCredentials(), 
                usuarioActualizado.getAuthorities()
            );
            
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            
            // PERSISTENCIA PARA SPRING SECURITY 6: Guardar el contexto actualizado en la sesión
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
            
            logger.info("Sesión actualizada para el usuario: {}", usuarioActualizado.getNombreUsuario());
        }
    }
}