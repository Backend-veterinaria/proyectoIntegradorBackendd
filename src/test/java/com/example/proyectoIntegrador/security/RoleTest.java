package com.example.proyectoIntegrador.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleTest {

    @ParameterizedTest
    @CsvSource({
            "Administrador, ADMIN",
            "ADMIN, ADMIN",
            "Veterinario, VETERINARIO",
            "Médico veterinario, VETERINARIO",
            "medico, VETERINARIO",
            "Cliente, CLIENTE",
            "Recepcionista, CLIENTE",
    })
    void reconoceElRolPorLaDescripcion(String description, Role expected) {
        assertEquals(expected, Role.fromDescription(description));
    }

    @ParameterizedTest
    @NullSource
    void descripcionNulaEsCliente(String description) {
        assertEquals(Role.CLIENTE, Role.fromDescription(description));
    }

    @ParameterizedTest
    @CsvSource({"ADMIN, ADMIN", "VETERINARIO, VETERINARIO", "CLIENTE, CLIENTE", "SUPERUSER, CLIENTE"})
    void fromNameConValorDesconocidoEsCliente(String name, Role expected) {
        assertEquals(expected, Role.fromName(name));
    }
}
