package com.example.proyectoIntegrador.repository;

import com.example.proyectoIntegrador.repository.Impl.AgendaCitaRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regresión: desde VET-01 la tabla personal también tiene cliente_id, así que en las consultas que
 * unen agenda_cita con personal la columna debe ir con alias (si no, PostgreSQL da "column reference
 * cliente_id is ambiguous" y el detalle de la cita fallaba).
 */
@ExtendWith(MockitoExtension.class)
class AgendaCitaRepositoryImplTest {

    /** cliente_id sin "alias." delante. */
    private static final Pattern UNQUALIFIED_CLIENTE_ID = Pattern.compile("(?<![\\w.])cliente_id");

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void listaDeCitasUsaAliasEnClienteIdYTraeMascotaYVeterinario() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt())).thenReturn(List.of());

        new AgendaCitaRepositoryImpl(jdbcTemplate).getListAgenda(5);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sql.capture(), any(RowMapper.class), anyInt());
        assertFalse(UNQUALIFIED_CLIENTE_ID.matcher(sql.getValue()).find(), sql.getValue());
        assertTrue(sql.getValue().contains("ac.cliente_id = ?"));
        assertTrue(sql.getValue().contains("ac.medico_id"));
        assertTrue(sql.getValue().contains("mascota_nombre"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void detalleDeCitaUsaAliasEnClienteId() {
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), anyInt(), anyInt())).thenReturn(null);

        new AgendaCitaRepositoryImpl(jdbcTemplate).getDetailAppointment(100, 5);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).queryForObject(sql.capture(), any(RowMapper.class), anyInt(), anyInt());
        assertFalse(UNQUALIFIED_CLIENTE_ID.matcher(sql.getValue()).find(), sql.getValue());
        assertTrue(sql.getValue().contains("ac.cliente_id = ?"));
    }
}
