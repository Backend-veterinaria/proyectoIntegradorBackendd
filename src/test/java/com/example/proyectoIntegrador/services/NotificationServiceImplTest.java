package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.models.DetailAppointment;
import com.example.proyectoIntegrador.services.Impl.NotificationServiceimpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * Notificaciones SSE (RF-13). Sin un servidor real el emisor guarda los eventos hasta que la
 * conexión se inicializa, así que aquí se comprueba el registro de conexiones y que enviar nunca falle.
 */
class NotificationServiceImplTest {

    private final NotificationServiceimpl service = new NotificationServiceimpl();
    private final DetailAppointment cita = new DetailAppointment("Vacunación", "20261120", "143000");

    @Test
    void abrirConexionDevuelveUnEmisorSinTiempoLimite() {
        SseEmitter emitter = service.stablishConnection("5");

        assertNotNull(emitter);
        // 0 = sin tiempo límite: la conexión queda abierta mientras el cliente esté conectado
        assertEquals(0L, emitter.getTimeout());
    }

    @Test
    void notificarAClienteSinConexionNoHaceNada() {
        assertDoesNotThrow(() -> service.notifyAppointmentCreation("99", cita));
    }

    @Test
    void notificarAClienteConectadoNoFalla() {
        service.stablishConnection("5");

        assertDoesNotThrow(() -> service.notifyAppointmentCreation("5", cita));
    }

    @Test
    void reconectarReemplazaLaConexionAnterior() {
        SseEmitter primera = service.stablishConnection("5");
        SseEmitter segunda = service.stablishConnection("5");

        assertNotSame(primera, segunda);
        assertDoesNotThrow(() -> service.notifyAppointmentCreation("5", cita));
    }

    @Test
    void notificarDespuesDeCerrarLaConexionNoFalla() {
        SseEmitter emitter = service.stablishConnection("5");
        emitter.complete();

        assertDoesNotThrow(() -> service.notifyAppointmentCreation("5", cita));
    }
}
