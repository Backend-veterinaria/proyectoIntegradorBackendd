package com.example.proyectoIntegrador.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta al crear un propietario o restablecer su contraseña (CIT-06). La contraseña temporal se
 * muestra solo en esta respuesta, para que el personal se la entregue al tutor; no se puede consultar
 * después.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientTemporaryPassword {
    private ClientInfo cliente;
    private String passwordTemporal;
}
