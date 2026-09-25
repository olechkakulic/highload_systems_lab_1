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
  protected static final String GET = "GET";
  protected static final String POST = "POST";
  protected static final String PUT = "PUT";
  protected static final String DELETE = "DELETE";

  protected static final int HTTP_OK = 200;
  protected static final int HTTP_CREATED = 201;
  protected static final int HTTP_NO_CONTENT = 204;
  protected static final int HTTP_BAD_REQUEST = 400;
  protected static final int HTTP_NOT_FOUND = 404;
  protected static final int HTTP_CONFLICT = 409;

  protected static final String API_SHELTERS = "/api/shelters";
  protected static final String API_USERS = "/api/users";
  protected static final String API_TAGS = "/api/tags";
  protected static final String API_ANIMALS = "/api/animals";
  protected static final String API_APPLICATIONS = "/api/applications";
  protected static final String API_SHIFTS = "/api/shifts";
  protected static final String API_MISSING = "/api/missing";
  protected static final String API_OPENAPI = "/v3/api-docs";
  protected static final String API_SWAGGER = "/swagger-ui/index.html";
  protected static final String API_HEALTH = "/actuator/health";

  protected static final String DEFAULT_SHELTER_NAME = "Добрый дом";
  protected static final String DEFAULT_SHELTER_ADDRESS = "Улица Лесная, 1";
  protected static final String DEFAULT_USER_NAME = "Пользователь";
  protected static final String DEFAULT_ANIMAL_NAME = "Барсик";
  protected static final String DEFAULT_ANIMAL_DESCRIPTION = "Дружелюбный питомец";
  protected static final String DEFAULT_SHIFT_TITLE = "Уход за животными";
  protected static final String DEFAULT_APPLICATION_COMMENT = "Готов заботиться";

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
    shelter =
        create(
            API_SHELTERS,
            Map.of("name", DEFAULT_SHELTER_NAME, "address", DEFAULT_SHELTER_ADDRESS));
  }

  protected ResultActions request(String method, String path, Object body) throws Exception {
    var builder =
        switch (method) {
          case POST -> post(path);
          case PUT -> put(path);
          case DELETE -> delete(path);
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
        request(POST, path, input)
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn()
            .getResponse();
    return json.readTree(response.getContentAsByteArray()).get("id").asLong();
  }

  protected long user(String email, Role role) throws Exception {
    return create(API_USERS, Map.of("name", DEFAULT_USER_NAME, "email", email, "role", role));
  }

  protected AnimalInput animalInput(String name, Set<Long> tags) {
    return new AnimalInput(
        name, Species.CAT, LocalDate.of(2022, 1, 1), DEFAULT_ANIMAL_DESCRIPTION, shelter, tags);
  }

  protected long animal() throws Exception {
    return create(API_ANIMALS, animalInput(DEFAULT_ANIMAL_NAME, Set.of()));
  }

  protected ShiftInput shiftInput(int capacity) {
    return new ShiftInput(
        shelter,
        DEFAULT_SHIFT_TITLE,
        Instant.now().plusSeconds(86400),
        Instant.now().plusSeconds(90000),
        capacity);
  }

  protected long shift(int capacity) throws Exception {
    return create(API_SHIFTS, shiftInput(capacity));
  }

  protected long application(long animal, long user) throws Exception {
    return create(API_APPLICATIONS, new ApplicationInput(animal, user, DEFAULT_APPLICATION_COMMENT));
  }

  protected void approve(long id) throws Exception {
    call(POST, applicationReviewPath(id), new ReviewInput(ReviewDecision.APPROVE), HTTP_OK);
  }

  protected static String shelterPath(long id) {
    return byId(API_SHELTERS, id);
  }

  protected static String userPath(long id) {
    return byId(API_USERS, id);
  }

  protected static String tagPath(long id) {
    return byId(API_TAGS, id);
  }

  protected static String animalPath(long id) {
    return byId(API_ANIMALS, id);
  }

  protected static String applicationPath(long id) {
    return byId(API_APPLICATIONS, id);
  }

  protected static String applicationReviewPath(long id) {
    return applicationPath(id) + "/review";
  }

  protected static String applicationWithdrawPath(long id) {
    return applicationPath(id) + "/withdraw";
  }

  protected static String applicationCompletePath(long id) {
    return applicationPath(id) + "/complete";
  }

  protected static String shiftPath(long id) {
    return byId(API_SHIFTS, id);
  }

  protected static String shiftCancelPath(long id) {
    return shiftPath(id) + "/cancel";
  }

  protected static String participationsPath(long shiftId) {
    return shiftPath(shiftId) + "/participations";
  }

  protected static String participationPath(long shiftId, long participationId) {
    return participationsPath(shiftId) + "/" + participationId;
  }

  protected static String participationCancelPath(long shiftId, long participationId) {
    return participationPath(shiftId, participationId) + "/cancel";
  }

  private static String byId(String basePath, long id) {
    return basePath + "/" + id;
  }
}
