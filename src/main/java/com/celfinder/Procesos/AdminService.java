package com.celfinder.Procesos;

import com.celfinder.Model.Solicitud;
import com.celfinder.Model.Notificacion;
import com.celfinder.Model.Producto;
import com.celfinder.Model.Usuario;
import com.celfinder.util.ConfiguracionApp;
import com.celfinder.util.EstadoSolicitud;
import com.celfinder.util.Roles;
import com.celfinder.util.TipoSolicitud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    
    private final MongoTemplate mongoTemplate;

    @Autowired
    public AdminService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Page<Usuario> obtenerTodosLosUsuarios(Pageable pageable) {
        Query query = new Query();
        long total = mongoTemplate.count(query, Usuario.class, "usuarios");
        query.with(pageable);
        List<Usuario> usuarios = mongoTemplate.find(query, Usuario.class, "usuarios");
        return new PageImpl<>(usuarios, pageable, total);
    }

    public Page<Producto> obtenerTodosLosProductos(Pageable pageable) {
        Query query = new Query();
        long total = mongoTemplate.count(query, Producto.class, "productos");
        query.with(pageable);
        List<Producto> productos = mongoTemplate.find(query, Producto.class, "productos");
        return new PageImpl<>(productos, pageable, total);
    }

    public Page<Producto> buscarProductos(String nombre, String estadoVenta, Pageable pageable) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (nombre != null && !nombre.isEmpty()) {
            criteriaList.add(Criteria.where("nombre").regex(Pattern.quote(nombre), "i"));
        }
        if (estadoVenta != null && !estadoVenta.isEmpty()) {
            criteriaList.add(Criteria.where("estadoVenta").is(estadoVenta));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, Producto.class, "productos");
        query.with(pageable);
        List<Producto> productos = mongoTemplate.find(query, Producto.class, "productos");
        return new PageImpl<>(productos, pageable, total);
    }

    public void eliminarProducto(String id) {
        Producto producto = mongoTemplate.findById(id, Producto.class, "productos");
        if (producto == null) {
            throw new IllegalArgumentException("Producto no encontrado: " + id);
        }
        Query solicitudesQuery = new Query(Criteria.where("productoId").is(id).and("estado").is(EstadoSolicitud.PENDIENTE));
        if (mongoTemplate.exists(solicitudesQuery, Solicitud.class, "solicitudes")) {
            throw new IllegalStateException("No se puede eliminar el producto porque tiene solicitudes pendientes.");
        }
        mongoTemplate.remove(producto, "productos");
    }

    public Usuario obtenerUsuarioPorId(String id) {
        return mongoTemplate.findById(id, Usuario.class, "usuarios");
    }

    public void asignarRol(String usuarioId, String rol) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        if (!rol.equals(Roles.ROLE_VENDEDOR) && !rol.equals(Roles.ROLE_ADMIN)) {
            throw new IllegalArgumentException("Rol inválido. Use ROLE_VENDEDOR o ROLE_ADMIN.");
        }
        if (usuario.getRoles().contains(rol)) {
            throw new IllegalArgumentException("El usuario ya tiene el rol " + rol);
        }
        usuario.getRoles().add(rol);
        mongoTemplate.save(usuario, "usuarios");
    }

    public void removerRol(String usuarioId, String rol) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        if (!rol.equals("ROLE_VENDEDOR") && !rol.equals("ROLE_ADMIN")) {
            throw new IllegalArgumentException("Rol inválido. Use ROLE_VENDEDOR o ROLE_ADMIN.");
        }
        if (rol.equals("ROLE_USER")) {
            throw new IllegalArgumentException("No se puede remover el rol ROLE_USER.");
        }
        if (!usuario.getRoles().contains(rol)) {
            throw new IllegalArgumentException("El usuario no tiene el rol " + rol);
        }
        usuario.getRoles().remove(rol);
        mongoTemplate.save(usuario, "usuarios");
    }

    public void desactivarUsuario(String usuarioId) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        if (ConfiguracionApp.CUENTA_INACTIVA.equals(usuario.getEstadoCuenta())) {
            throw new IllegalStateException("El usuario ya está desactivado.");
        }
        usuario.setEstadoCuenta(ConfiguracionApp.CUENTA_INACTIVA);
        mongoTemplate.save(usuario, "usuarios");
    }

    public void reactivarUsuario(String usuarioId) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        if (ConfiguracionApp.CUENTA_ACTIVA.equals(usuario.getEstadoCuenta())) {
            throw new IllegalStateException("El usuario ya está activo.");
        }
        usuario.setEstadoCuenta(ConfiguracionApp.CUENTA_ACTIVA);
        mongoTemplate.save(usuario, "usuarios");
    }

    public void eliminarUsuario(String usuarioId) {
        Usuario usuario = obtenerUsuarioPorId(usuarioId);
        if (usuario == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }
        if (!"desactivada".equals(usuario.getEstadoCuenta())) {
            throw new IllegalStateException("Solo se pueden eliminar usuarios desactivados.");
        }
        Query productosQuery = new Query(Criteria.where("vendedorId").is(usuarioId));
        if (mongoTemplate.exists(productosQuery, Producto.class, "productos")) {
            throw new IllegalStateException("No se puede eliminar el usuario porque tiene productos registrados.");
        }
        Query solicitudesQuery = new Query(Criteria.where("usuarioId").is(usuarioId)
                .orOperator(Criteria.where("vendedorId").is(usuarioId))
                .and("tipoSolicitud").in("compra", "vendedor"));
        if (mongoTemplate.exists(solicitudesQuery, Solicitud.class, "solicitudes")) {
            throw new IllegalStateException("No se puede eliminar el usuario porque tiene solicitudes asociadas.");
        }
        mongoTemplate.remove(usuario, "usuarios");
    }

    public void crearSolicitudVendedor(Solicitud solicitud) {
        Query query = new Query(Criteria.where("usuarioId").is(solicitud.getUsuarioId())
                .and("estado").is(EstadoSolicitud.PENDIENTE)
                .and("tipoSolicitud").is(TipoSolicitud.VENDEDOR));
        if (mongoTemplate.exists(query, Solicitud.class, "solicitudes")) {
            throw new IllegalStateException("Ya existe una solicitud pendiente para este usuario.");
        }
        solicitud.setTipoSolicitud(TipoSolicitud.VENDEDOR);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);
        mongoTemplate.save(solicitud, "solicitudes");
    }

    public List<Solicitud> obtenerSolicitudesVendedorPendientes() {
        Query query = new Query(Criteria.where("estado").is(EstadoSolicitud.PENDIENTE)
                .and("tipoSolicitud").is(TipoSolicitud.VENDEDOR));
        return mongoTemplate.find(query, Solicitud.class, "solicitudes");
    }

    public Solicitud obtenerSolicitudVendedorPorId(String id) {
        Query query = new Query(Criteria.where("_id").is(id)
                .and("tipoSolicitud").is(TipoSolicitud.VENDEDOR));
        return mongoTemplate.findOne(query, Solicitud.class, "solicitudes");
    }

    public void gestionarSolicitudVendedor(String solicitudId, String accion, String comentarioAdmin) {
        Solicitud solicitud = obtenerSolicitudVendedorPorId(solicitudId);
        if (solicitud == null) {
            throw new IllegalArgumentException("La solicitud no existe.");
        }
        if (!solicitud.getEstado().equals(EstadoSolicitud.PENDIENTE)) {
            throw new IllegalStateException("La solicitud ya ha sido gestionada.");
        }
        solicitud.setEstado(accion.equals("aprobar") ? EstadoSolicitud.APROBADA : EstadoSolicitud.RECHAZADA);
        solicitud.setComentarioAdmin(comentarioAdmin != null ? comentarioAdmin : "");
        solicitud.setFechaRespuesta(LocalDateTime.now());
        if (accion.equals("aprobar")) {
            asignarRol(solicitud.getUsuarioId(), Roles.ROLE_VENDEDOR);
        }
        mongoTemplate.save(solicitud, "solicitudes");
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(solicitud.getUsuarioId());
        notificacion.setMensaje("Tu solicitud para ser vendedor ha sido " + (accion.equals("aprobar") ? "aprobada" : "rechazada") + ". Comentario: " + (comentarioAdmin != null ? comentarioAdmin : "Ninguno"));
        notificacion.setFecha(LocalDateTime.now());
        crearNotificacion(notificacion);
    }

    public List<Solicitud> obtenerHistorialSolicitudesVendedor(String usuarioId) {
        Query query = new Query(Criteria.where("usuarioId").is(usuarioId)
                .and("tipoSolicitud").is(TipoSolicitud.VENDEDOR));
        return mongoTemplate.find(query, Solicitud.class, "solicitudes");
    }

    public void crearNotificacion(Notificacion notificacion) {
        notificacion.setFecha(LocalDateTime.now());
        mongoTemplate.save(notificacion, "notificaciones");
    }

    public void crearNotificacionParaTodos(String mensaje) {
        List<Usuario> usuarios = mongoTemplate.findAll(Usuario.class, "usuarios");
        if (usuarios.isEmpty()) {
            throw new IllegalStateException("No hay usuarios registrados.");
        }
        for (Usuario usuario : usuarios) {
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(usuario.getId());
            notificacion.setMensaje(mensaje);
            notificacion.setFecha(LocalDateTime.now());
            mongoTemplate.save(notificacion, "notificaciones");
        }
    }

    public List<Notificacion> obtenerNotificacionesPorUsuario(String usuarioId) {
        Query query = new Query(Criteria.where("usuarioId").is(usuarioId)).with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "fecha"));
        return mongoTemplate.find(query, Notificacion.class, "notificaciones");
    }

    public long contarNotificacionesNoLeidas(String usuarioId) {
        Query query = new Query(Criteria.where("usuarioId").is(usuarioId).and("leida").is(false));
        return mongoTemplate.count(query, Notificacion.class, "notificaciones");
    }

    public void marcarComoLeida(String notificacionId) {
        Notificacion notif = mongoTemplate.findById(notificacionId, Notificacion.class, "notificaciones");
        if (notif != null) {
            notif.setLeida(true);
            mongoTemplate.save(notif, "notificaciones");
        }
    }

    public void marcarTodasComoLeidas(String usuarioId) {
        Query query = new Query(Criteria.where("usuarioId").is(usuarioId).and("leida").is(false));
        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update().set("leida", true);
        mongoTemplate.updateMulti(query, update, Notificacion.class, "notificaciones");
    }
}