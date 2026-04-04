package com.celfinder.Procesos;

import com.celfinder.Model.Mensaje;
import com.celfinder.Model.Notificacion;
import com.celfinder.Model.Solicitud;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MensajeService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AdminService adminService;

    public List<Mensaje> obtenerMensajesPorSolicitud(String solicitudId) {
        return mongoTemplate.find(
            new Query(Criteria.where("solicitudId").is(solicitudId))
                .limit(50), Mensaje.class);
    }

    public void enviarMensaje(String solicitudId, String emisorId, String contenido) {
        Solicitud solicitud = mongoTemplate.findById(solicitudId, Solicitud.class);
        if (solicitud == null) return;

        String receptorId = emisorId.equals(solicitud.getUsuarioId()) ? solicitud.getVendedorId() : solicitud.getUsuarioId();

        Mensaje mensaje = new Mensaje();
        mensaje.setSolicitudId(solicitudId);
        mensaje.setEmisorId(emisorId);
        mensaje.setReceptorId(receptorId);
        mensaje.setContenido(contenido);
        mongoTemplate.save(mensaje);

        // Notificar al receptor
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(receptorId);
        notificacion.setMensaje("Tienes un nuevo mensaje sobre tu solicitud de compra.");
        notificacion.setFecha(LocalDateTime.now());
        adminService.crearNotificacion(notificacion);
    }

    public void marcarComoLeido(String mensajeId) {
        Mensaje mensaje = mongoTemplate.findById(mensajeId, Mensaje.class);
        if (mensaje != null) {
            mensaje.setLeido(true);
            mongoTemplate.save(mensaje);
        }
    }
}