package ru.lab.shelter.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import ru.lab.shelter.dto.animal.AnimalInput;
import ru.lab.shelter.dto.application.ApplicationInput;
import ru.lab.shelter.dto.application.ReviewDecision;
import ru.lab.shelter.dto.application.ReviewInput;
import ru.lab.shelter.dto.volunteer.ShiftInput;
import ru.lab.shelter.model.*;
import ru.lab.shelter.service.*;

@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestSupport {
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired protected MockMvc mvc;
  @Autowired protected ObjectMapper json;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected AdoptionService adoption;
  @Autowired protected VolunteerService volunteering;
  protected long shelter;

  @BeforeEach
  void reset() throws Exception {
    jdbc.execute(
        "TRUNCATE shift_participations, volunteer_shifts, adoption_applications, animal_tags,"
            + " animals, tags, users, shelters RESTART IDENTITY CASCADE");
    shelter = create("/api/shelters", Map.of("name", "Добрый дом", "address", "Улица Лесная, 1"));
  }

  protected ResultActions request(String method, String path, Object body) throws Exception {
    var builder =
        switch (method) {
          case "POST" -> post(path);
          case "PUT" -> put(path);
          case "DELETE" -> delete(path);
          default -> get(path);
        };
    if (body != null) builder.contentType("application/json").content(json.writeValueAsBytes(body));
    return mvc.perform(builder);
  }

  protected JsonNode call(String method, String path, Object body, int expected) throws Exception {
    var response =
        request(method, path, body).andExpect(status().is(expected)).andReturn().getResponse();
    return response.getContentAsByteArray().length == 0
        ? json.nullNode()
        : json.readTree(response.getContentAsByteArray());
  }

  protected long create(String path, Object input) throws Exception {
    var response =
        request("POST", path, input)
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn()
            .getResponse();
    return json.readTree(response.getContentAsByteArray()).get("id").asLong();
  }

  protected long user(String email, Role role) throws Exception {
    return create("/api/users", Map.of("name", "Пользователь", "email", email, "role", role));
  }

  protected AnimalInput animalInput(String name, Set<Long> tags) {
    return new AnimalInput(
        name, Species.CAT, LocalDate.of(2022, 1, 1), "Дружелюбный питомец", shelter, tags);
  }

  protected long animal() throws Exception {
    return create("/api/animals", animalInput("Барсик", Set.of()));
  }

  protected ShiftInput shiftInput(int capacity) {
    return new ShiftInput(
        shelter,
        "Уход за животными",
        Instant.now().plusSeconds(86400),
        Instant.now().plusSeconds(90000),
        capacity);
  }

  protected long shift(int capacity) throws Exception {
    return create("/api/shifts", shiftInput(capacity));
  }

  protected long application(long animal, long user) throws Exception {
    return create("/api/applications", new ApplicationInput(animal, user, "Готов заботиться"));
  }

  protected void approve(long id) throws Exception {
    call(
        "POST",
        "/api/applications/" + id + "/review",
        new ReviewInput(ReviewDecision.APPROVE),
        200);
  }
}
