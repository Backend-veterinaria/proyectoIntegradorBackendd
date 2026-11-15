package com.example.proyectoIntegrador.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    /**
     * Codificador de contraseñas BCrypt (RNF-01). El hash resultante tiene 60 caracteres,
     * por lo que la columna user_credentials.password_hash debe admitir al menos esa longitud.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
