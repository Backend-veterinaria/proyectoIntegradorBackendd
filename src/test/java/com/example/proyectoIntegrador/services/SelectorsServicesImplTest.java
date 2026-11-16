package com.example.proyectoIntegrador.services;

import com.example.proyectoIntegrador.exceptions.BusinessException;
import com.example.proyectoIntegrador.models.DataSelector;
import com.example.proyectoIntegrador.models.DataUserDTO;
import com.example.proyectoIntegrador.models.selectors.SelectorEstado;
import com.example.proyectoIntegrador.models.selectors.SelectorRaza;
import com.example.proyectoIntegrador.models.selectors.SelectorRols;
import com.example.proyectoIntegrador.models.selectors.SelectorVeterinarian;
import com.example.proyectoIntegrador.repository.SelectorRepository;
import com.example.proyectoIntegrador.services.Impl.SelectorsServicesImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelectorsServicesImplTest {

    @Mock
    private SelectorRepository selectorRepository;

    private SelectorsServicesImpl service;

    @BeforeEach
    void setUp() {
        service = new SelectorsServicesImpl(selectorRepository);
    }

    private void givenEstadosYVeterinarios() {
        when(selectorRepository.getSelectorEstado()).thenReturn(Optional.of(List.of(new SelectorEstado())));
        when(selectorRepository.getSelectorVeterinarian()).thenReturn(Optional.of(List.of(new SelectorVeterinarian())));
    }

    @Test
    void devuelveLasCuatroListas() {
        givenEstadosYVeterinarios();
        when(selectorRepository.getSelectorRaza()).thenReturn(Optional.of(List.of(new SelectorRaza(), new SelectorRaza())));
        when(selectorRepository.getSelectorRols()).thenReturn(Optional.of(List.of(new SelectorRols())));

        DataSelector result = service.getSelectorDetail(new DataUserDTO());

        assertEquals(1, result.getSelectorEstadoList().size());
        assertEquals(1, result.getSelectorVeterinarians().size());
        assertEquals(2, result.getSelectorRazas().size());
        assertEquals(1, result.getSelectorRols().size());
    }

    @Test
    void sinEstadosFallaSinConsultarLoDemas() {
        when(selectorRepository.getSelectorEstado()).thenReturn(Optional.empty());

        BusinessException e = assertThrows(BusinessException.class, () -> service.getSelectorDetail(new DataUserDTO()));
        assertEquals("Error. No hay datos configurados para el estado", e.getMessage());
        verify(selectorRepository, never()).getSelectorVeterinarian();
    }

    @Test
    void sinVeterinariosFalla() {
        when(selectorRepository.getSelectorEstado()).thenReturn(Optional.of(List.of(new SelectorEstado())));
        when(selectorRepository.getSelectorVeterinarian()).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getSelectorDetail(new DataUserDTO()));
    }

    @Test
    void sinRazasFalla() {
        givenEstadosYVeterinarios();
        when(selectorRepository.getSelectorRaza()).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getSelectorDetail(new DataUserDTO()));
    }

    @Test
    void sinRolesFalla() {
        givenEstadosYVeterinarios();
        when(selectorRepository.getSelectorRaza()).thenReturn(Optional.of(List.of(new SelectorRaza())));
        when(selectorRepository.getSelectorRols()).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.getSelectorDetail(new DataUserDTO()));
    }
}
