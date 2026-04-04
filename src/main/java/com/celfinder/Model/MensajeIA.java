package com.celfinder.Model;

public class MensajeIA {
    private String rol;   // "user" o "bot"
    private String texto;

    public MensajeIA(String rol, String texto) {
        this.rol = rol;
        this.texto = texto;
    }

    public String getRol() { return rol; }
    public String getTexto() { return texto; }
}
