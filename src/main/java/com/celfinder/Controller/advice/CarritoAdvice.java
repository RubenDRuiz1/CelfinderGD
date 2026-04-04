package com.celfinder.Controller.advice;

import com.celfinder.Procesos.CarritoService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Provee el contador del carrito a todas las vistas de forma global.
 */
@ControllerAdvice
public class CarritoAdvice {

    private final CarritoService carritoService;

    public CarritoAdvice(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @ModelAttribute("carritoContador")
    public int getCarritoContador() {
        return carritoService.getContador();
    }
}
