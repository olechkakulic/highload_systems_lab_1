package ru.lab.shelter.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.validation.Validation;
import java.time.*;
import java.util.*;
import ru.lab.shelter.dto.animal.AnimalInput;
import ru.lab.shelter.dto.shelter.ShelterInput;
import ru.lab.shelter.dto.user.UserInput;
import ru.lab.shelter.dto.volunteer.ParticipationInput;
import ru.lab.shelter.dto.volunteer.ShiftInput;
import ru.lab.shelter.model.*;

class PaginationValidationApiIT extends IntegrationTestSupport {
  @org.junit.jupiter.api.Test
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

  @org.junit.jupiter.api.Test
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

  @org.junit.jupiter.api.Test
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
}
