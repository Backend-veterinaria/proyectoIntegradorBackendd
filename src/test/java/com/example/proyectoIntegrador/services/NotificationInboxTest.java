package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.AppointmentEvent;
import com.example.proyectoIntegrador.models.DetailAppointment;
import com.example.proyectoIntegrador.models.NotificationModels.PreferenceItem;
import com.example.proyectoIntegrador.models.NotificationModels.SaveResult;
import com.example.proyectoIntegrador.repository.NotificationRepository;
import com.example.proyectoIntegrador.services.Impl.NotificationServiceimpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Notificaciones guardadas y preferencias (CIT-09).
 */
@ExtendWith(MockitoExtension.class)
class NotificationInboxTest {

    @Mock
    private NotificationRepository repo;

    private NotificationServiceimpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceimpl(repo, new ObjectMapper());
    }

    @Test
    void cadaAvisoSeGuardaConUnMensajeLegible() {
        when(repo.saveIfEnabled(eq(5), eq("cita-cancelada"), anyString(), anyString())).thenReturn(SaveResult.SAVED);

        service.notify("5", "cita-cancelada",
                new AppointmentEvent(100, "Vacunacion", "2026-11-20", "14:30:00", "Cancelada", "Firulais", "Viaje"));

        verify(repo).saveIfEnabled(eq(5), eq("cita-cancelada"),
                eq("Se cancelo la cita \"Vacunacion\" del 2026-11-20 a las 14:30. Motivo: Viaje"), contains("\"agendaId\":100"));
    }

    @Test
    void elMensajeDeCitaCreadaNormalizaFechaYHora() {
        assertEquals("Se agendo tu cita \"Control\" del 2026-11-20 a las 09:00",
                NotificationServiceimplAccess.message("cita-creada", new DetailAppointment("Control", "20261120", "090000")));
    }

    @Test
    void siElGuardadoFallaElAvisoNoRompeNada() {
        when(repo.saveIfEnabled(anyInt(), anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("bd"));

        assertDoesNotThrow(() -> service.notify("5", "cita-creada", new DetailAppointment("Control", "20261120", "090000")));
    }

    @Test
    void usuarioNoNumericoNoSeGuarda() {
        assertDoesNotThrow(() -> service.notify("abc", "cita-creada", new DetailAppointment("x", "20261120", "090000")));
        verify(repo, never()).saveIfEnabled(anyInt(), anyString(), anyString(), anyString());
    }

    @Test
    void laBandejaLimitaLaCantidad() {
        when(repo.countUnread(5)).thenReturn(3);
        when(repo.findByUser(5, true, 100)).thenReturn(List.of());

        assertEquals(3, service.inbox(5, true, 5000).getNoLeidas());
        verify(repo).findByUser(5, true, 100);
    }

    @Test
    void marcarComoLeidaUnaAjenaEsErrorDeNegocio() {
        when(repo.markRead(9, 5)).thenReturn(0);

        BusinessException e = assertThrows(BusinessException.class, () -> service.markRead(5, 9));
        assertEquals("La notificacion no existe", e.getMessage());
    }

    @Test
    void preferenciaDeTipoInexistenteNoSeGuarda() {
        when(repo.typeExists("otro")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.updatePreferences(5, List.of(new PreferenceItem("otro", false))));
        verify(repo, never()).savePreference(anyInt(), anyString(), anyBoolean());
    }

    @Test
    void guardaLasPreferencias() {
        when(repo.typeExists("cita-cancelada")).thenReturn(true);

        service.updatePreferences(5, List.of(new PreferenceItem("cita-cancelada", false)));

        verify(repo).savePreference(5, "cita-cancelada", false);
    }

    /** Acceso al método estático de paquete desde otro paquete de pruebas. */
    private static final class NotificationServiceimplAccess {
        static String message(String event, Object data) {
            try {
                var m = NotificationServiceimpl.class.getDeclaredMethod("message", String.class, Object.class);
                m.setAccessible(true);
                return (String) m.invoke(null, event, data);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
