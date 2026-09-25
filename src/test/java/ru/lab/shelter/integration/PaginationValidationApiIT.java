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
    request(GET, API_TAGS + "?size=50", null)
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "52"))
        .andExpect(jsonPath("$.items.length()").value(50))
        .andExpect(jsonPath("$.hasNext").value(true));
    request(GET, API_TAGS + "?page=1&size=50", null)
        .andExpect(jsonPath("$.items.length()").value(2));
    long shift = shift(1);
    shift(1);
    request(GET, API_SHIFTS + "?shelterId=" + shelter + "&status=SCHEDULED&size=1", null)
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("X-Total-Count"))
        .andExpect(jsonPath("$.hasNext").value(true))
        .andExpect(jsonPath("$.totalElements").doesNotExist());
    request(GET, API_SHIFTS + "?size=1&page=1", null).andExpect(jsonPath("$.hasNext").value(false));
    for (String path :
        List.of(
            API_SHELTERS,
            API_USERS,
            API_TAGS,
            API_ANIMALS,
            API_APPLICATIONS,
            API_SHIFTS,
            participationsPath(shift))) {
      call(GET, path + "?size=51", null, HTTP_BAD_REQUEST);
      call(GET, path + "?page=-1", null, HTTP_BAD_REQUEST);
    }
  }

  @org.junit.jupiter.api.Test
  void validationErrorsAndOpenApi() throws Exception {
    request(POST, API_SHELTERS, new ShelterInput("", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").exists());
    call(
        POST,
        API_USERS,
        new UserInput("Имя", "not-an-email", Role.APPLICANT, null),
        HTTP_BAD_REQUEST);
    call(
        POST,
        API_ANIMALS,
        new AnimalInput(
            "Имя", Species.CAT, LocalDate.now().plusDays(1), "Описание", shelter, Set.of()),
        HTTP_BAD_REQUEST);
    call(
        POST,
        API_SHIFTS,
        new ShiftInput(
            shelter, "Смена", Instant.now().plusSeconds(100), Instant.now().plusSeconds(50), 1),
        HTTP_BAD_REQUEST);
    call(POST, API_SHIFTS, shiftInput(0), HTTP_BAD_REQUEST);
    mvc.perform(post(API_ANIMALS).contentType("application/json").content("{broken"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").exists());
    call(
        POST,
        API_SHELTERS,
        Map.of("name", "Дом", "address", "Адрес", "unknown", "no"),
        HTTP_BAD_REQUEST);
    call(GET, API_ANIMALS + "?species=INVALID", null, HTTP_BAD_REQUEST);
    call(GET, API_ANIMALS + "/not-a-number", null, HTTP_BAD_REQUEST);
    call(GET, API_MISSING, null, HTTP_NOT_FOUND);
    for (String path :
        List.of(API_USERS, API_TAGS, API_SHELTERS, API_ANIMALS, API_APPLICATIONS, API_SHIFTS))
      call(GET, path + "/99999", null, HTTP_NOT_FOUND);
    call(POST, applicationWithdrawPath(99999L), null, HTTP_NOT_FOUND);
    call(POST, participationsPath(99999L), new ParticipationInput(1L), HTTP_NOT_FOUND);
    long shift = shift(1);
    call(GET, participationPath(shift, 99999L), null, HTTP_NOT_FOUND);
    request(GET, API_OPENAPI, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists())
        .andExpect(jsonPath("$.paths['" + API_ANIMALS + "']").exists());
    request(GET, API_SWAGGER, null).andExpect(status().isOk());
    request(GET, API_HEALTH, null)
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
