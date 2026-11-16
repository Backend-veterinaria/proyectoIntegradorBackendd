package com.example.proyectoIntegrador.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Pruebas de integración (QA-03): recorren la API completa contra un PostgreSQL real y desechable
 * (Docker), creado con sql/schema.sql y src/test/resources/integration/seed.sql. Así se prueban de
 * verdad las consultas SQL de los repositorios, que las pruebas unitarias simulan.
 *
 * <p>Los pasos van en orden y comparten estado (usuarios, mascota, cita), como un usuario real.
 * Si Docker no está disponible, la clase se omite en lugar de fallar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class VeterinaryFlowIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath("sql/schema.sql"), "/docker-entrypoint-initdb.d/01-schema.sql")
            .withCopyFileToContainer(MountableFile.forClasspathResource("integration/seed.sql"), "/docker-entrypoint-initdb.d/02-seed.sql");

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String MANANA = LocalDate.now().plusDays(1).format(YMD);
    private static final String PASADO_MANANA = LocalDate.now().plusDays(2).format(YMD);
    private static final String HOY = LocalDate.now().format(YMD);

    // Estado compartido entre los pasos
    private static String adminToken;
    private static String clienteToken;
    private static int clienteId;
    private static String vetToken;
    private static int medicoId;
    private static int mascotaId;
    private static int agendaId;
    private static int atencionId;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------------------------------------------------------------- utilidades

    private String call(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request).andReturn().getResponse().getContentAsString();
    }

    /** Llama y exige responseCode 0000; devuelve el JSON. */
    private String ok(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        String json = call(request, token, body);
        assertEquals("0000", JsonPath.read(json, "$.responseCode"), json);
        return json;
    }

    private String login(String username, String password) throws Exception {
        String json = ok(post("/integrador/login"), null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        return JsonPath.read(json, "$.token");
    }

    private int estadoId(String descripcion) {
        return jdbcTemplate.queryForObject("SELECT estado_id FROM estado_cita WHERE descripcion = ?", Integer.class, descripcion);
    }

    // ---------------------------------------------------------------- autenticación y usuarios

    @Test
    @Order(1)
    void registroYLoginDeCliente() throws Exception {
        ok(post("/integrador/register"), null, """
                {"nombre":"Ana","apellido":"Gomez","direccion":"Calle 1","telefono":"3001234567",
                 "email":"ana@correo.com","username":"ana","password":"secreta123"}""");

        String json = ok(post("/integrador/login"), null, "{\"username\":\"ana\",\"password\":\"secreta123\"}");
        clienteToken = JsonPath.read(json, "$.token");
        clienteId = Integer.parseInt(JsonPath.read(json, "$.responseObj.idClient"));
        assertEquals("CLIENTE", JsonPath.read(json, "$.responseObj.role"));

        // La contraseña quedó cifrada con BCrypt, no en texto plano
        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM user_credentials WHERE usuario_id = ?", String.class, clienteId);
        assertTrue(hash.startsWith("$2"), hash);
    }

    @Test
    @Order(2)
    void registroDuplicadoYPasswordIncorrecta() throws Exception {
        String duplicado = call(post("/integrador/register"), null, """
                {"nombre":"Ana","apellido":"Gomez","direccion":"Calle 1","telefono":"3001234567",
                 "email":"ana@correo.com","username":"ana","password":"secreta123"}""");
        assertEquals("9994", JsonPath.read(duplicado, "$.responseCode"));

        String malLogin = call(post("/integrador/login"), null, "{\"username\":\"ana\",\"password\":\"incorrecta\"}");
        assertEquals("1231", JsonPath.read(malLogin, "$.responseCode"));
    }

    @Test
    @Order(3)
    void adminCreaVeterinarioYEsteIniciaSesion() throws Exception {
        adminToken = login("demo", "demo123");

        String json = ok(post("/integrador/veterinarians"), adminToken, """
                {"nombre":"Carlos","apellido":"Perez","telefono":"3001112233","email":"cperez@vet.com",
                 "username":"dr.perez","password":"secreta123"}""");
        medicoId = JsonPath.read(json, "$.responseObj.medicoId");

        String vetLogin = ok(post("/integrador/login"), null, "{\"username\":\"dr.perez\",\"password\":\"secreta123\"}");
        vetToken = JsonPath.read(vetLogin, "$.token");
        assertEquals("VETERINARIO", JsonPath.read(vetLogin, "$.responseObj.role"));
    }

    @Test
    @Order(4)
    void clienteRegistraYListaSuMascota() throws Exception {
        ok(post("/integrador/pets/Addpets"), clienteToken,
                "{\"idCliente\":" + clienteId + ",\"nombre\":\"Firulais\",\"edad\":3,\"genero\":\"Macho\",\"razaId\":1}");

        String json = ok(get("/integrador/pets/getListPets").param("idClient", String.valueOf(clienteId)), clienteToken, null);
        mascotaId = JsonPath.read(json, "$.responseObj[0].idMascota");
        assertEquals("Labrador", JsonPath.read(json, "$.responseObj[0].tipoRaza"));
    }

    @Test
    @Order(5)
    void selectoresIncluyenEstadosYVeterinarioActivo() throws Exception {
        String json = ok(get("/integrador/selectors/All").param("idClient", String.valueOf(clienteId)), clienteToken, null);

        List<Integer> vets = JsonPath.read(json, "$.responseObj.selectorVeterinarians[*].idVeterinarian");
        assertTrue(vets.contains(medicoId), json);
        List<String> estados = JsonPath.read(json, "$.responseObj.selectorEstadoList[*].descripcion");
        assertTrue(estados.contains("Programada"), json);
    }

    @Test
    @Order(6)
    void clienteNoPuedeVerDatosDeOtroCliente() throws Exception {
        int status = mockMvc.perform(get("/integrador/pets/getListPets").param("idClient", String.valueOf(clienteId + 1000))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + clienteToken))
                .andReturn().getResponse().getStatus();
        assertEquals(403, status);
    }

    // ---------------------------------------------------------------- citas

    @Test
    @Order(10)
    void clienteAgendaCitaYElHorarioQuedaOcupado() throws Exception {
        String cita = "{\"clienteId\":" + clienteId + ",\"medicoId\":" + medicoId + ",\"mascotaId\":" + mascotaId
                + ",\"description\":\"Vacunación\",\"fecha\":\"" + MANANA + "\",\"hora\":\"100000\"}";
        ok(post("/integrador/agendaCita/scheduleAnAppointment"), clienteToken, cita);

        // Mismo veterinario, misma hora: no hay disponibilidad
        String repetida = call(post("/integrador/agendaCita/scheduleAnAppointment"), clienteToken, cita);
        assertEquals("6612", JsonPath.read(repetida, "$.responseCode"));
        assertEquals("El veterinario no tiene disponible ese horario", JsonPath.read(repetida, "$.responseObj"));
    }

    @Test
    @Order(11)
    void listaYDetalleDeCitaTraenMascotaYVeterinario() throws Exception {
        String lista = ok(get("/integrador/agendaCita/list").param("idClient", String.valueOf(clienteId)), clienteToken, null);
        agendaId = JsonPath.read(lista, "$.responseObj[0].agendaCitaId");
        assertEquals(mascotaId, (int) JsonPath.read(lista, "$.responseObj[0].mascotaId"));
        assertEquals(medicoId, (int) JsonPath.read(lista, "$.responseObj[0].medicoId"));
        assertEquals("Carlos Perez", JsonPath.read(lista, "$.responseObj[0].nombreVeterinario"));
        assertEquals("Programada", JsonPath.read(lista, "$.responseObj[0].estado"));

        // Regresión: el detalle fallaba por "cliente_id" ambiguo desde VET-01
        String detalle = ok(get("/integrador/agendaCita/AnAppointment/details")
                .param("idAppointment", String.valueOf(agendaId))
                .param("idClient", String.valueOf(clienteId))
                .param("username", "ana"), clienteToken, null);
        assertEquals("Firulais", JsonPath.read(detalle, "$.responseObj.infoMascota.nombreMascota"));
        assertEquals("Carlos Perez", JsonPath.read(detalle, "$.responseObj.nombreVeterinario"));
    }

    @Test
    @Order(12)
    void veterinarioVeSuAgendaYConfirmaLaCita() throws Exception {
        String agenda = ok(get("/integrador/agendaCita/veterinarian").param("fecha", MANANA), vetToken, null);
        assertEquals(agendaId, (int) JsonPath.read(agenda, "$.responseObj[0].agendaId"));
        assertEquals("Ana Gomez", JsonPath.read(agenda, "$.responseObj[0].clienteNombre"));

        String confirmada = ok(patch("/integrador/agendaCita/" + agendaId + "/status"), vetToken,
                "{\"estadoId\":" + estadoId("Confirmada") + "}");
        assertEquals("Confirmada", JsonPath.read(confirmada, "$.responseObj.estado"));
    }

    @Test
    @Order(13)
    void clienteReprogramaLaCita() throws Exception {
        String json = ok(patch("/integrador/agendaCita/" + agendaId + "/reschedule"), clienteToken,
                "{\"fecha\":\"" + PASADO_MANANA + "\",\"hora\":\"090000\"}");
        assertEquals("Reprogramada", JsonPath.read(json, "$.responseObj.estado"));
        assertTrue(((String) JsonPath.read(json, "$.responseObj.fecha")).startsWith(LocalDate.now().plusDays(2).toString()));
    }

    @Test
    @Order(14)
    void clienteCancelaOtraCita() throws Exception {
        ok(post("/integrador/agendaCita/scheduleAnAppointment"), clienteToken,
                "{\"clienteId\":" + clienteId + ",\"medicoId\":" + medicoId + ",\"mascotaId\":" + mascotaId
                        + ",\"description\":\"Control\",\"fecha\":\"" + MANANA + "\",\"hora\":\"150000\"}");
        int otra = jdbcTemplate.queryForObject("""
                SELECT ac.agenda_id FROM agenda_cita ac JOIN cita c ON c.cita_id = ac.cita_id
                WHERE ac.cliente_id = ? AND c.hora = '15:00:00'""", Integer.class, clienteId);

        ok(patch("/integrador/agendaCita/cancelledAppointment"), clienteToken,
                "{\"idCliente\":" + clienteId + ",\"idAppointment\":" + otra + ",\"motivo\":\"Viaje\"}");

        // El motivo se guarda y se ve en la lista de citas (CIT-07)
        String lista = ok(get("/integrador/agendaCita/list").param("idClient", String.valueOf(clienteId)), clienteToken, null);
        List<String> motivos = JsonPath.read(lista, "$.responseObj[?(@.agendaCitaId == " + otra + ")].motivoCancelacion");
        assertEquals(List.of("Viaje"), motivos);

        int estado = jdbcTemplate.queryForObject("SELECT estado_id FROM agenda_cita WHERE agenda_id = ?", Integer.class, otra);
        assertEquals(estadoId("Cancelada"), estado);
    }

    // ---------------------------------------------------------------- historia clínica

    @Test
    @Order(20)
    void veterinarioRegistraLaAtencionYLaCitaQuedaAtendida() throws Exception {
        // La atención solo se registra desde el día de la cita: se simula que llegó el día
        jdbcTemplate.update("""
                UPDATE cita SET fecha = CURRENT_DATE
                WHERE cita_id = (SELECT cita_id FROM agenda_cita WHERE agenda_id = ?)""", agendaId);

        String json = ok(post("/integrador/clinicalHistory/attentions"), vetToken,
                "{\"agendaId\":" + agendaId + ",\"motivoConsulta\":\"Control\",\"diagnostico\":\"Sano\","
                        + "\"pesoKg\":12.5,\"temperaturaC\":38.6}");
        atencionId = JsonPath.read(json, "$.responseObj.atencionId");
        assertEquals("Carlos Perez", JsonPath.read(json, "$.responseObj.medicoNombre"));

        int estado = jdbcTemplate.queryForObject("SELECT estado_id FROM agenda_cita WHERE agenda_id = ?", Integer.class, agendaId);
        assertEquals(estadoId("Atendida"), estado);

        // Una sola atención por cita
        String segunda = call(post("/integrador/clinicalHistory/attentions"), vetToken,
                "{\"agendaId\":" + agendaId + ",\"diagnostico\":\"Otra\"}");
        assertNotEquals("0000", JsonPath.read(segunda, "$.responseCode"));
    }

    @Test
    @Order(21)
    void veterinarioRegistraTratamientoYVacuna() throws Exception {
        String tratamiento = ok(post("/integrador/clinicalHistory/treatments"), vetToken,
                "{\"mascotaId\":" + mascotaId + ",\"atencionId\":" + atencionId
                        + ",\"medicamento\":\"Amoxicilina\",\"dosis\":\"250 mg\",\"fechaInicio\":\"" + HOY + "\"}");
        int tratamientoId = JsonPath.read(tratamiento, "$.responseObj.tratamientoId");
        assertEquals("EN_CURSO", JsonPath.read(tratamiento, "$.responseObj.estado"));

        String finalizado = ok(patch("/integrador/clinicalHistory/treatments/" + tratamientoId + "/status"), vetToken,
                "{\"estado\":\"FINALIZADO\"}");
        assertEquals("FINALIZADO", JsonPath.read(finalizado, "$.responseObj.estado"));
        assertEquals(LocalDate.now().toString(), JsonPath.read(finalizado, "$.responseObj.fechaFin"));

        ok(post("/integrador/clinicalHistory/vaccines"), vetToken,
                "{\"mascotaId\":" + mascotaId + ",\"nombre\":\"Rabia\",\"fechaAplicacion\":\"" + HOY
                        + "\",\"proximaDosis\":\"" + LocalDate.now().plusDays(10).format(YMD) + "\"}");
    }

    @Test
    @Order(22)
    void duenoConsultaLaHistoriaClinicaYSusVacunasPendientes() throws Exception {
        String historia = ok(get("/integrador/clinicalHistory/pets/" + mascotaId), clienteToken, null);
        assertEquals(1, ((List<?>) JsonPath.read(historia, "$.responseObj.atenciones")).size());
        assertEquals(1, ((List<?>) JsonPath.read(historia, "$.responseObj.tratamientos")).size());
        assertEquals(1, ((List<?>) JsonPath.read(historia, "$.responseObj.vacunas")).size());
        assertEquals("Sano", JsonPath.read(historia, "$.responseObj.atenciones[0].diagnostico"));

        String pendientes = ok(get("/integrador/clinicalHistory/vaccines/upcoming").param("days", "30"), clienteToken, null);
        assertEquals("Rabia", JsonPath.read(pendientes, "$.responseObj[0].nombre"));
    }

    // ---------------------------------------------------------------- reportes y propietarios

    @Test
    @Order(30)
    void reportesDelAdministrador() throws Exception {
        String desde = LocalDate.now().minusDays(1).format(YMD);
        String hasta = LocalDate.now().plusDays(5).format(YMD);

        String porEstado = ok(get("/integrador/reports/appointmentsByStatus").param("desde", desde).param("hasta", hasta),
                adminToken, null);
        List<Map<String, Object>> items = JsonPath.read(porEstado, "$.responseObj.items");
        assertEquals(6, items.size(), "aparecen todos los estados, también los que tienen 0");
        assertEquals(2, ((Number) JsonPath.read(porEstado, "$.responseObj.total")).intValue());
        List<Number> atendidas = JsonPath.read(porEstado, "$.responseObj.items[?(@.status == 'ATENDIDA')].cantidad");
        assertEquals(List.of(1), atendidas.stream().map(Number::intValue).toList());

        String porVeterinario = ok(get("/integrador/reports/attentionsByVeterinarian").param("desde", desde).param("hasta", hasta),
                adminToken, null);
        assertEquals(1, ((Number) JsonPath.read(porVeterinario, "$.responseObj.total")).intValue());

        String mascotas = ok(get("/integrador/reports/attendedPets").param("desde", desde).param("hasta", hasta),
                vetToken, null);
        assertEquals("Firulais", JsonPath.read(mascotas, "$.responseObj.items[0].mascotaNombre"));
    }

    @Test
    @Order(31)
    void clienteEditaSuPerfilYElAdminLoBusca() throws Exception {
        ok(put("/integrador/clients/" + clienteId), clienteToken, """
                {"nombre":"Ana","apellido":"Gomez","direccion":"Calle 2","telefono":"3109999999","email":"ana@correo.com"}""");

        String busqueda = ok(get("/integrador/clients").param("q", "gomez"), adminToken, null);
        assertEquals("3109999999", JsonPath.read(busqueda, "$.responseObj[0].telefono"));
        // Los veterinarios no aparecen como propietarios
        assertEquals(1, ((List<?>) JsonPath.read(busqueda, "$.responseObj")).size());
    }

    @Test
    @Order(23)
    void vacunaRepetidaNoSeRegistraDosVeces() throws Exception {
        String repetida = call(post("/integrador/clinicalHistory/vaccines"), vetToken,
                "{\"mascotaId\":" + mascotaId + ",\"nombre\":\"rabia\",\"fechaAplicacion\":\"" + HOY + "\"}");
        assertEquals("Esa vacuna ya esta registrada para la mascota en esa fecha", JsonPath.read(repetida, "$.responseObj"));
    }

    @Test
    @Order(35)
    void personalRegistraTutorQueCambiaSuPasswordTemporal() throws Exception {
        String creado = ok(post("/integrador/clients"), vetToken, """
                {"nombre":"Luis","apellido":"Diaz","telefono":"3001234567","email":"luis@correo.com","username":"luis.diaz"}""");
        String temporal = JsonPath.read(creado, "$.responseObj.passwordTemporal");
        int luisId = JsonPath.read(creado, "$.responseObj.cliente.clienteId");

        // Primer inicio de sesión: debe cambiar la contraseña
        String login = ok(post("/integrador/login"), null,
                "{\"username\":\"luis.diaz\",\"password\":\"" + temporal + "\"}");
        assertEquals(true, JsonPath.read(login, "$.responseObj.cambiarPassword"));
        String luisToken = JsonPath.read(login, "$.token");

        ok(put("/integrador/clients/" + luisId + "/password"), luisToken,
                "{\"passwordActual\":\"" + temporal + "\",\"passwordNueva\":\"MiClaveNueva1\"}");

        String despues = ok(post("/integrador/login"), null, "{\"username\":\"luis.diaz\",\"password\":\"MiClaveNueva1\"}");
        assertEquals(false, JsonPath.read(despues, "$.responseObj.cambiarPassword"));

        // Si la olvida, el personal le da otra temporal
        String reset = ok(post("/integrador/clients/" + luisId + "/password/reset"), adminToken, null);
        String otra = JsonPath.read(reset, "$.responseObj.passwordTemporal");
        String conOtra = ok(post("/integrador/login"), null, "{\"username\":\"luis.diaz\",\"password\":\"" + otra + "\"}");
        assertEquals(true, JsonPath.read(conOtra, "$.responseObj.cambiarPassword"));
    }

    @Test
    @Order(40)
    void scriptDeUsuariosParaVeterinariosDeEjemplo() throws Exception {
        // Veterinario creado "a mano", como los de ejemplo: personal + medico, sin usuario
        int personalId = jdbcTemplate.queryForObject(
                "INSERT INTO personal (nombre, apellido) VALUES ('Laura', 'Gómez') RETURNING personal_id", Integer.class);
        jdbcTemplate.update("INSERT INTO medico (identificacion_medico) VALUES (?)", personalId);

        String script = java.nio.file.Files.readString(java.nio.file.Path.of("sql/DEV_usuarios_veterinarios.sql"));
        jdbcTemplate.execute(script);
        jdbcTemplate.execute(script); // se puede ejecutar dos veces sin duplicar nada

        // El hash de pgcrypto lo acepta el backend
        String json = ok(post("/integrador/login"), null, "{\"username\":\"laura.gomez\",\"password\":\"Veterinario123\"}");
        assertEquals("VETERINARIO", JsonPath.read(json, "$.responseObj.role"));
        assertEquals(1, (int) jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cliente WHERE username = 'laura.gomez'", Integer.class));
    }

    @Test
    @Order(41)
    void demoEsAdminAunqueEnLaBdExistaComoCliente() throws Exception {
        ok(post("/integrador/register"), null, """
                {"nombre":"Demo","apellido":"Equipo","direccion":"-","telefono":"300","email":"demo@correo.com",
                 "username":"demo","password":"otraClave123"}""");

        String json = ok(post("/integrador/login"), null, "{\"username\":\"demo\",\"password\":\"demo123\"}");
        assertEquals("ADMIN", JsonPath.read(json, "$.responseObj.role"));
    }

    @Test
    @Order(32)
    void propietarioInactivoNoPuedeIniciarSesion() throws Exception {
        ok(patch("/integrador/clients/" + clienteId + "/status"), adminToken, "{\"activo\":false}");

        String json = call(post("/integrador/login"), null, "{\"username\":\"ana\",\"password\":\"secreta123\"}");
        assertEquals("1231", JsonPath.read(json, "$.responseCode"));
        assertEquals("Usuario inactivo. Comuniquese con el administrador", JsonPath.read(json, "$.responseObj"));
    }
}
