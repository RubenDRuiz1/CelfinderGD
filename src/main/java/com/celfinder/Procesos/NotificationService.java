package com.celfinder.Procesos;

import com.celfinder.Model.Notificacion;
import com.celfinder.Model.Producto;
import com.celfinder.Model.Solicitud;
import com.celfinder.Model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Servicio de notificaciones de negocio.
 * Responsabilidad única: crear notificaciones semánticas para los actores
 * involucrados en los flujos de compra/venta, delegando la persistencia a AdminService.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final AdminService adminService;

    public NotificationService(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Notifica a comprador y vendedor cuando se crea una solicitud de compra.
     *
     * @param comprador usuario que inicia la compra
     * @param producto  producto sobre el que se hace la solicitud
     */
    public void notificarCompraSolicitada(Usuario comprador, Producto producto) {
        try {
            Notificacion notifComprador = new Notificacion();
            notifComprador.setUsuarioId(comprador.getId());
            notifComprador.setMensaje("¡Solicitud enviada para '"
                    + producto.getNombre()
                    + "'! Esperando aprobación del vendedor.");
            adminService.crearNotificacion(notifComprador);

            Notificacion notifVendedor = new Notificacion();
            notifVendedor.setUsuarioId(producto.getVendedorId());
            notifVendedor.setMensaje("¡Nueva solicitud de compra! "
                    + comprador.getNombreUsuario()
                    + " quiere comprar '" + producto.getNombre() + "'.");
            notifVendedor.setEnlace("/ventas/gestionar-solicitudes");
            adminService.crearNotificacion(notifVendedor);

        } catch (Exception e) {
            // Las notificaciones no deben interrumpir el flujo principal
            logger.error("Error al crear notificaciones de compra para producto {}: {}",
                    producto.getId(), e.getMessage());
        }
    }

    /**
     * Notifica al comprador cuando el vendedor gestiona (autoriza o rechaza) su solicitud.
     *
     * @param solicitud solicitud gestionada
     * @param accion    "autorizar" o "rechazar"
     */
    public void notificarGestionSolicitud(Solicitud solicitud, String accion) {
        try {
            Notificacion notif = new Notificacion();
            notif.setUsuarioId(solicitud.getUsuarioId());
            notif.setMensaje("autorizar".equals(accion)
                    ? "Tu compra de '" + solicitud.getNombreProducto() + "' fue autorizada."
                    : "Tu compra de '" + solicitud.getNombreProducto() + "' fue rechazada.");
            if ("autorizar".equals(accion)) {
                notif.setEnlace("/ventas/rastreo-pedido/" + solicitud.getId());
            }
            adminService.crearNotificacion(notif);
        } catch (Exception e) {
            logger.error("Error al notificar gestión de solicitud {}: {}",
                    solicitud.getId(), e.getMessage());
        }
    }

    /**
     * Notifica al comprador sobre el cambio de estado de su envío.
     */
    public void notificarCambioEstadoRastreo(Solicitud solicitud, String nuevoEstado) {
        try {
            Notificacion notif = new Notificacion();
            notif.setUsuarioId(solicitud.getUsuarioId());
            notif.setMensaje("Actualización de envío: " + nuevoEstado + " para '" + solicitud.getNombreProducto() + "'.");
            notif.setEnlace("/ventas/rastreo-pedido/" + solicitud.getId());
            adminService.crearNotificacion(notif);
        } catch (Exception e) {
            logger.error("Error al notificar cambio de estado: {}", e.getMessage());
        }
    }

    /**
     * Notifica al vendedor que el cliente tiene una duda.
     */
    public void notificarDudaPedido(String vendedorId, String compradorNombre, String solicitudId) {
        try {
            Notificacion notif = new Notificacion();
            notif.setUsuarioId(vendedorId);
            notif.setMensaje("El cliente " + compradorNombre + " tiene una duda sobre su pedido.");
            notif.setEnlace("/ventas/chat-pedido/" + solicitudId);
            adminService.crearNotificacion(notif);
        } catch (Exception e) {
            logger.error("Error al notificar duda de pedido: {}", e.getMessage());
        }
    }
}
