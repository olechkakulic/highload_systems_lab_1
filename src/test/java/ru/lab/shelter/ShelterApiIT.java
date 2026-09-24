package ru.lab.shelter;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import ru.lab.shelter.dto.animal.AnimalInput;
import ru.lab.shelter.dto.application.ApplicationInput;
import ru.lab.shelter.dto.application.ApplicationUpdate;
import ru.lab.shelter.dto.application.ReviewDecision;
import ru.lab.shelter.dto.application.ReviewInput;
import ru.lab.shelter.dto.shelter.ShelterInput;
import ru.lab.shelter.dto.tag.TagInput;
import ru.lab.shelter.dto.user.UserInput;
import ru.lab.shelter.dto.user.UserUpdate;
import ru.lab.shelter.dto.volunteer.ParticipationInput;
import ru.lab.shelter.dto.volunteer.ShiftInput;
import ru.lab.shelter.exception.ApiException;
import ru.lab.shelter.model.*;
import ru.lab.shelter.service.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ShelterApiIT {
  @Container @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired JdbcTemplate jdbc;
  @Autowired AdoptionService adoption;
  @Autowired VolunteerService volunteering;
  private long shelter;

  @BeforeEach
  void reset() throws Exception {
    jdbc.execute(
        "TRUNCATE shift_participations, volunteer_shifts, adoption_applications, animal_tags,"
            + " animals, tags, users, shelters RESTART IDENTITY CASCADE");
    shelter = create("/api/shelters", Map.of("name", "Добрый дом", "address", "Улица Лесная, 1"));
  }

  private ResultActions request(String method, String path, Object body) throws Exception {
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

  private JsonNode call(String method, String path, Object body, int expected) throws Exception {
    var response =
        request(method, path, body).andExpect(status().is(expected)).andReturn().getResponse();
    return response.getContentAsByteArray().length == 0
        ? json.nullNode()
        : json.readTree(response.getContentAsByteArray());
  }

  private long create(String path, Object input) throws Exception {
    var response =
        request("POST", path, input)
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andReturn()
            .getResponse();
    return json.readTree(response.getContentAsByteArray()).get("id").asLong();
  }

  private long user(String email, Role role) throws Exception {
    return create("/api/users", Map.of("name", "Пользователь", "email", email, "role", role));
  }

  private AnimalInput animalInput(String name, Set<Long> tags) {
    return new AnimalInput(
        name, Species.CAT, LocalDate.of(2022, 1, 1), "Дружелюбный питомец", shelter, tags);
  }

  private long animal() throws Exception {
    return create("/api/animals", animalInput("Барсик", Set.of()));
  }

  private ShiftInput shiftInput(int capacity) {
    return new ShiftInput(
        shelter,
        "Уход за животными",
        Instant.now().plusSeconds(86400),
        Instant.now().plusSeconds(90000),
        capacity);
  }

  private long shift(int capacity) throws Exception {
    return create("/api/shifts", shiftInput(capacity));
  }

  private long application(long animal, long user) throws Exception {
    return create("/api/applications", new ApplicationInput(animal, user, "Готов заботиться"));
  }

  private void approve(long id) throws Exception {
    call(
        "POST",
        "/api/applications/" + id + "/review",
        new ReviewInput(ReviewDecision.APPROVE),
        200);
  }

  @Test
  void catalogCrudAndRelationshipConstraints() throws Exception {
    call("PUT", "/api/shelters/" + shelter, new ShelterInput("Новый дом", "Адрес 2"), 200);
    assertThat(call("GET", "/api/shelters/" + shelter, null, 200).get("name").asText())
        .isEqualTo("Новый дом");
    call("GET", "/api/shelters", null, 200);
    long employee =
        create(
            "/api/users",
            new UserInput("Сотрудник", "EMPLOYEE@example.org", Role.EMPLOYEE, shelter));
    assertThat(call("GET", "/api/users/" + employee, null, 200).get("email").asText())
        .isEqualTo("employee@example.org");
    call(
        "PUT",
        "/api/users/" + employee,
        new UserUpdate("Имя", "renamed@example.org", shelter),
        200);
    call("GET", "/api/users", null, 200);
    call(
        "POST",
        "/api/users",
        new UserInput("Без приюта", "bad@example.org", Role.EMPLOYEE, null),
        400);
    call("PUT", "/api/users/" + employee, new UserUpdate("Имя", "renamed@example.org", null), 400);
    call(
        "POST",
        "/api/users",
        new UserInput("Повтор", "RENAMED@example.org", Role.APPLICANT, null),
        409);
    call("DELETE", "/api/shelters/" + shelter, null, 409);
    call("DELETE", "/api/users/" + employee, null, 204);
    call("DELETE", "/api/shelters/" + shelter, null, 204);
    call("GET", "/api/shelters/" + shelter, null, 404);
  }

  @Test
  void animalsAndTagsCrudFilteringAndStringEnums() throws Exception {
    long tag = create("/api/tags", new TagInput("Спокойный"));
    call("GET", "/api/tags/" + tag, null, 200);
    call("GET", "/api/tags", null, 200);
    call("PUT", "/api/tags/" + tag, new TagInput("Ласковый"), 200);
    call("POST", "/api/tags", new TagInput("ЛАСКОВЫЙ"), 409);
    long animal = create("/api/animals", animalInput("Мурка", Set.of(tag)));
    var view = call("GET", "/api/animals/" + animal, null, 200);
    assertThat(view.get("tags").get(0).get("id").asLong()).isEqualTo(tag);
    assertThat(jdbc.queryForObject("select species from animals where id=?", String.class, animal))
        .isEqualTo("CAT");
    request(
            "GET",
            "/api/animals?shelterId=" + shelter + "&species=CAT&status=AVAILABLE&tagId=" + tag,
            null)
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "1"))
        .andExpect(jsonPath("$.items.length()").value(1));
    request("GET", "/api/animals?species=DOG", null)
        .andExpect(header().string("X-Total-Count", "0"));
    call("PUT", "/api/animals/" + animal, animalInput("Новое имя", Set.of()), 200);
    call("POST", "/api/animals", animalInput("Нет тега", Set.of(999L)), 400);
    call("DELETE", "/api/tags/" + tag, null, 204);
    call("DELETE", "/api/animals/" + animal, null, 204);
    call("GET", "/api/animals/" + animal, null, 404);
  }

  @Test
  void deletingTagUnlinksManyToManyWithoutDeletingAnimal() throws Exception {
    long tag = create("/api/tags", new TagInput("Тег"));
    long animal = create("/api/animals", animalInput("Пёс", Set.of(tag)));
    call("DELETE", "/api/tags/" + tag, null, 204);
    assertThat(call("GET", "/api/animals/" + animal, null, 200).get("tags")).isEmpty();
  }

  @Test
  void adoptionCompletesAllChangesAndRejectsConflictingOperations() throws Exception {
    long animal = animal(),
        u1 = user("first@example.org", Role.APPLICANT),
        u2 = user("second@example.org", Role.APPLICANT);
    long a1 = application(animal, u1), a2 = application(animal, u2);
    call("POST", "/api/applications", new ApplicationInput(animal, u1, "Повтор"), 409);
    call("PUT", "/api/applications/" + a1, new ApplicationUpdate("Дополнение"), 200);
    call("POST", "/api/applications/" + a1 + "/complete", null, 409);
    call("DELETE", "/api/applications/" + a1, null, 409);
    approve(a1);
    call(
        "POST",
        "/api/applications/" + a2 + "/review",
        new ReviewInput(ReviewDecision.APPROVE),
        409);
    call(
        "POST",
        "/api/applications/" + a1 + "/review",
        new ReviewInput(ReviewDecision.APPROVE),
        409);
    call("PUT", "/api/applications/" + a1, new ApplicationUpdate("Поздно"), 409);
    call("POST", "/api/applications/" + a1 + "/complete", null, 200);
    assertThat(call("GET", "/api/animals/" + animal, null, 200).get("status").asText())
        .isEqualTo("ADOPTED");
    assertThat(call("GET", "/api/applications/" + a1, null, 200).get("status").asText())
        .isEqualTo("COMPLETED");
    assertThat(call("GET", "/api/applications/" + a2, null, 200).get("status").asText())
        .isEqualTo("REJECTED");
    request("GET", "/api/applications?animalId=" + animal + "&applicantId=" + u1, null)
        .andExpect(header().string("X-Total-Count", "1"));
    call("POST", "/api/applications/" + a1 + "/complete", null, 409);
    call("POST", "/api/applications", new ApplicationInput(animal, u2, "Поздно"), 409);
    call("POST", "/api/applications/" + a1 + "/withdraw", null, 409);
    call("PUT", "/api/animals/" + animal, animalInput("Поздно", Set.of()), 409);
    call("DELETE", "/api/animals/" + animal, null, 409);
    call("DELETE", "/api/applications/" + a1, null, 409);
  }

  @Test
  void applicationRejectionWithdrawalAndDeletion() throws Exception {
    long animal = animal(), user = user("owner@example.org", Role.APPLICANT);
    long a = application(animal, user);
    call("POST", "/api/applications/" + a + "/review", new ReviewInput(ReviewDecision.REJECT), 200);
    call("POST", "/api/applications/" + a + "/review", new ReviewInput(ReviewDecision.REJECT), 409);
    call("DELETE", "/api/applications/" + a, null, 204);
    a = application(animal, user);
    call("POST", "/api/applications/" + a + "/withdraw", null, 200);
    call("DELETE", "/api/applications/" + a, null, 204);
    call("GET", "/api/applications/" + a, null, 404);
    long volunteer = user("volunteer@example.org", Role.VOLUNTEER);
    call("POST", "/api/applications", new ApplicationInput(animal, volunteer, "Не заявитель"), 409);
  }

  @Test
  void shiftLifecycleCapacityAndParticipationCrud() throws Exception {
    long shift = shift(2),
        u1 = user("v1@example.org", Role.VOLUNTEER),
        u2 = user("v2@example.org", Role.VOLUNTEER);
    call("GET", "/api/shifts/" + shift, null, 200);
    call("PUT", "/api/shifts/" + shift, shiftInput(2), 200);
    long p1 = create("/api/shifts/" + shift + "/participations", new ParticipationInput(u1));
    long p2 = create("/api/shifts/" + shift + "/participations", new ParticipationInput(u2));
    call("PUT", "/api/shifts/" + shift, shiftInput(1), 409);
    call("POST", "/api/shifts/" + shift + "/participations", new ParticipationInput(u1), 409);
    long u3 = user("v3@example.org", Role.VOLUNTEER);
    call("POST", "/api/shifts/" + shift + "/participations", new ParticipationInput(u3), 409);
    long applicant = user("applicant@example.org", Role.APPLICANT);
    call(
        "POST", "/api/shifts/" + shift + "/participations", new ParticipationInput(applicant), 409);
    call("GET", "/api/shifts/" + shift + "/participations/" + p1, null, 200);
    request("GET", "/api/shifts/" + shift + "/participations", null)
        .andExpect(header().string("X-Total-Count", "2"));
    call("DELETE", "/api/shifts/" + shift + "/participations/" + p1, null, 409);
    call("DELETE", "/api/shifts/" + shift, null, 409);
    call("POST", "/api/shifts/" + shift + "/participations/" + p1 + "/cancel", null, 200);
    call("POST", "/api/shifts/" + shift + "/participations/" + p1 + "/cancel", null, 409);
    var restored =
        call("POST", "/api/shifts/" + shift + "/participations", new ParticipationInput(u1), 200);
    assertThat(restored.get("id").asLong()).isEqualTo(p1);
    call("POST", "/api/shifts/" + shift + "/cancel", null, 200);
    assertThat(
            call("GET", "/api/shifts/" + shift + "/participations/" + p2, null, 200)
                .get("status")
                .asText())
        .isEqualTo("CANCELLED");
    call("POST", "/api/shifts/" + shift + "/participations", new ParticipationInput(u3), 409);
    call("DELETE", "/api/shifts/" + shift + "/participations/" + p1, null, 204);
    call("DELETE", "/api/shifts/" + shift + "/participations/" + p2, null, 204);
    call("DELETE", "/api/shifts/" + shift, null, 204);
    call("GET", "/api/shifts/" + shift, null, 404);
  }

  @Test
  void paginationCapsEveryListAndSliceHasNoTotal() throws Exception {
    for (int i = 0; i < 52; i++) jdbc.update("insert into tags(name) values (?)", "tag-" + i);
    request("GET", "/api/tags?size=50", null)
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "52"))
        .andExpect(jsonPath("$.items.length()").value(50))
        .andExpect(jsonPath("$.hasNext").value(true));
    request("GET", "/api/tags?page=1&size=50", null)
        .andExpect(jsonPath("$.items.length()").value(2));
    long shift = shift(1);
    shift(1);
    request("GET", "/api/shifts?shelterId=" + shelter + "&status=SCHEDULED&size=1", null)
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("X-Total-Count"))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.totalElements").doesNotExist());
    request("GET", "/api/shifts?size=1&page=1", null).andExpect(jsonPath("$.hasNext").value(false));
    for (String path :
        List.of(
            "shelters",
            "users",
            "tags",
            "animals",
            "applications",
            "shifts",
            "shifts/" + shift + "/participations")) {
      call("GET", "/api/" + path + "?size=51", null, 400);
      call("GET", "/api/" + path + "?page=-1", null, 400);
    }
  }

  @Test
  void validationErrorsAndOpenApi() throws Exception {
    request("POST", "/api/shelters", new ShelterInput("", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").exists());
    call("POST", "/api/users", new UserInput("Имя", "not-an-email", Role.APPLICANT, null), 400);
    call(
        "POST",
        "/api/animals",
        new AnimalInput(
            "Имя", Species.CAT, LocalDate.now().plusDays(1), "Описание", shelter, Set.of()),
        400);
    call(
        "POST",
        "/api/shifts",
        new ShiftInput(
            shelter, "Смена", Instant.now().plusSeconds(100), Instant.now().plusSeconds(50), 1),
        400);
    call("POST", "/api/shifts", shiftInput(0), 400);
    mvc.perform(post("/api/animals").contentType("application/json").content("{broken"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").exists());
    call("POST", "/api/shelters", Map.of("name", "Дом", "address", "Адрес", "unknown", "no"), 400);
    call("GET", "/api/animals?species=INVALID", null, 400);
    call("GET", "/api/animals/not-a-number", null, 400);
    call("GET", "/api/missing", null, 404);
    for (String path : List.of("users", "tags", "shelters", "animals", "applications", "shifts"))
      call("GET", "/api/" + path + "/99999", null, 404);
    call("POST", "/api/applications/99999/withdraw", null, 404);
    call("POST", "/api/shifts/99999/participations", new ParticipationInput(1L), 404);
    long shift = shift(1);
    call("GET", "/api/shifts/" + shift + "/participations/99999", null, 404);
    request("GET", "/v3/api-docs", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists())
        .andExpect(jsonPath("$.paths['/api/animals']").exists());
    request("GET", "/swagger-ui/index.html", null).andExpect(status().isOk());
    request("GET", "/actuator/health", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void entityValidationProtectsBypassedControllers() {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var validator = factory.getValidator();
      UserProfile employee = new UserProfile();
      employee.setName("Имя");
      employee.setEmail("a@example.org");
      employee.setRole(Role.EMPLOYEE);
      assertThat(validator.validate(employee))
          .anyMatch(v -> v.getPropertyPath().toString().equals("employeeShelterValid"));
      VolunteerShift shift = new VolunteerShift();
      shift.setStartsAt(Instant.now());
      shift.setEndsAt(Instant.now().minusSeconds(1));
      assertThat(validator.validate(shift))
          .anyMatch(v -> v.getPropertyPath().toString().equals("timeRangeValid"));
      assertThat(validator.validate(new Animal())).isNotEmpty();
    }
  }

  @Test
  void twoConcurrentReservationsCannotOverbookLastSlot() throws Exception {
    long shift = shift(1),
        u1 = user("race1@example.org", Role.VOLUNTEER),
        u2 = user("race2@example.org", Role.VOLUNTEER);
    var results =
        race(
            () -> volunteering.enroll(shift, new ParticipationInput(u1)),
            () -> volunteering.enroll(shift, new ParticipationInput(u2)));
    assertThat(results).containsExactlyInAnyOrder("OK", "CONFLICT");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from shift_participations where status='REGISTERED'", Long.class))
        .isEqualTo(1L);
  }

  @Test
  void twoConcurrentTransfersHaveOnlyOneWinner() throws Exception {
    long animal = animal(),
        user = user("race@example.org", Role.APPLICANT),
        app = application(animal, user);
    approve(app);
    assertThat(race(() -> adoption.complete(app), () -> adoption.complete(app)))
        .containsExactlyInAnyOrder("OK", "CONFLICT");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from adoption_applications where status='COMPLETED'", Long.class))
        .isEqualTo(1L);
  }

  private List<String> race(Callable<?> first, Callable<?> second) throws Exception {
    CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      List<Future<String>> futures = new ArrayList<>();
      for (Callable<?> operation : List.of(first, second))
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS))
                    throw new IllegalStateException("Start timeout");
                  try {
                    operation.call();
                    return "OK";
                  } catch (ApiException e) {
                    if (e.getStatus().value() != 409) throw e;
                    return "CONFLICT";
                  }
                }));
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      List<String> result = new ArrayList<>();
      for (var future : futures) result.add(future.get(20, TimeUnit.SECONDS));
      return result;
    }
  }

  @Test
  void databaseFailureRollsBackEntireTransfer() throws Exception {
    long animal = animal(),
        u1 = user("rollback1@example.org", Role.APPLICANT),
        u2 = user("rollback2@example.org", Role.APPLICANT);
    long selected = application(animal, u1), other = application(animal, u2);
    approve(selected);
    jdbc.execute(
        "CREATE FUNCTION fail_completion() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF"
            + " NEW.status='COMPLETED' THEN RAISE EXCEPTION 'simulated failure'; END IF; RETURN"
            + " NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER fail_completion BEFORE UPDATE ON adoption_applications FOR EACH ROW EXECUTE"
            + " FUNCTION fail_completion()");
    try {
      assertThatThrownBy(() -> adoption.complete(selected)).isInstanceOf(RuntimeException.class);
    } finally {
      jdbc.execute("DROP TRIGGER fail_completion ON adoption_applications");
      jdbc.execute("DROP FUNCTION fail_completion()");
    }
    assertThat(jdbc.queryForObject("select status from animals where id=?", String.class, animal))
        .isEqualTo("AVAILABLE");
    assertThat(
            jdbc.queryForObject(
                "select status from adoption_applications where id=?", String.class, selected))
        .isEqualTo("APPROVED");
    assertThat(
            jdbc.queryForObject(
                "select status from adoption_applications where id=?", String.class, other))
        .isEqualTo("PENDING");
    call("POST", "/api/applications/" + selected + "/complete", null, 200);
  }
}
