package com.celfinder.Procesos;

import com.celfinder.Model.Envio;
import com.celfinder.Model.Notificacion;
import com.celfinder.Model.Solicitud;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
public class EnvioService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AdminService adminService;

    public Envio crearEnvio(String solicitudId) {
        Solicitud solicitud = mongoTemplate.findById(solicitudId, Solicitud.class);
        if (solicitud == null || !"autorizada".equals(solicitud.getEstado())) {
            throw new IllegalStateException("Solicitud no autorizada para envío.");
        }

        if (mongoTemplate.exists(new Query(Criteria.where("solicitudId").is(solicitudId)), Envio.class)) {
            throw new IllegalStateException("El envío ya fue creado para esta solicitud.");
        }

        Envio envio = new Envio();
        envio.setSolicitudId(solicitudId);
        envio.setVendedorId(solicitud.getVendedorId());
        envio.setCompradorId(solicitud.getUsuarioId());
        envio.setCodigoSeguimiento(generarCodigoSeguimiento());
        envio.setEstado("preparando");
        envio.setFechaEstimada(LocalDate.now().plusDays(5));
        envio.setFechaActualizacion(LocalDateTime.now());

        mongoTemplate.save(envio);

        notificarCambio(envio, "Tu envío está siendo preparado.");

        return envio;
    }

    public Envio actualizarEstadoEnvio(String solicitudId, String vendedorId, String nuevoEstado) {
        Envio envio = mongoTemplate.findOne(new Query(Criteria.where("solicitudId").is(solicitudId)), Envio.class);
        if (envio == null) {
            throw new IllegalStateException("Envío no encontrado.");
        }
        if (!envio.getVendedorId().equals(vendedorId)) {
            throw new IllegalStateException("Solo el vendedor puede actualizar el envío.");
        }

        List<String> estadosValidos = List.of("preparando", "enviado", "en camino", "entregado");
        if (!estadosValidos.contains(nuevoEstado)) {
            throw new IllegalArgumentException("Estado inválido.");
        }

        envio.setEstado(nuevoEstado);
        envio.setFechaActualizacion(LocalDateTime.now());
        mongoTemplate.save(envio);

        notificarCambio(envio, "Tu envío ahora está: " + nuevoEstado);

        return envio;
    }

    public Envio obtenerEnvioPorSolicitud(String solicitudId) {
        return mongoTemplate.findOne(new Query(Criteria.where("solicitudId").is(solicitudId)), Envio.class);
    }

    public List<Envio> obtenerEnviosPorUsuario(String usuarioId) {
        return mongoTemplate.find(new Query(
            new Criteria().orOperator(
                Criteria.where("vendedorId").is(usuarioId),
                Criteria.where("compradorId").is(usuarioId)
            )
        ), Envio.class);
    }

    private String generarCodigoSeguimiento() {
        return "CF" + (100000 + new Random().nextInt(900000));
    }

    private void notificarCambio(Envio envio, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(envio.getCompradorId());
        notificacion.setMensaje(mensaje + " Código: " + envio.getCodigoSeguimiento());
        notificacion.setFecha(LocalDateTime.now());
        adminService.crearNotificacion(notificacion);
    }
}