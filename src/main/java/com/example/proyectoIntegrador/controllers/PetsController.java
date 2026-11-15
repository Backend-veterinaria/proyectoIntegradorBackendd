package com.example.proyectoIntegrador.controllers;

import com.example.proyectoIntegrador.models.DataMascotaDTO;
import com.example.proyectoIntegrador.models.DataUserDTO;
import com.example.proyectoIntegrador.models.PetUpdateDTO;
import com.example.proyectoIntegrador.models.StatusDTO;
import com.example.proyectoIntegrador.models.ResponseGeneric;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/integrador/pets")
public interface PetsController {

    @PostMapping("/Addpets")
    @ResponseBody
    ResponseEntity<ResponseGeneric> addPets(@Valid @RequestBody DataMascotaDTO dataMascotaDTO, HttpServletRequest request);


    @GetMapping("/getListPets")
    @ResponseBody
    ResponseEntity<ResponseGeneric> getListPets(@ModelAttribute @Valid DataUserDTO dataUserDTO,
                                                @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive,
                                                HttpServletRequest request);

    /**
     * Edita una mascota (CIT-02). El cliente solo sus mascotas; el personal, cualquiera.
     */
    @PutMapping("/{mascotaId}")
    @ResponseBody
    ResponseEntity<ResponseGeneric> updatePet(@PathVariable("mascotaId") int mascotaId, @Valid @RequestBody PetUpdateDTO dto);

    /**
     * Activa o inactiva una mascota ({"activo": false} para inactivar) (CIT-02).
     */
    @PatchMapping("/{mascotaId}/status")
    @ResponseBody
    ResponseEntity<ResponseGeneric> changeStatus(@PathVariable("mascotaId") int mascotaId, @Valid @RequestBody StatusDTO dto);

}
