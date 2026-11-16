package com.example.proyectoIntegrador.controllers;


import io.swagger.v3.oas.annotations.tags.Tag;
import com.example.proyectoIntegrador.models.DataUserDTO;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/integrador/selectors")
@Tag(name = "Listas de selección", description = "Estados, veterinarios activos, razas y roles")
public interface SelectorsController {

    @GetMapping("/All")
    @ResponseBody
    ResponseEntity<ResponseGeneric> getListSelectors(@ModelAttribute @Valid DataUserDTO loginDTO, HttpServletRequest request);

}
