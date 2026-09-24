package ru.lab.shelter.integration;

import static org.assertj.core.api.Assertions.*;

import ru.lab.shelter.dto.shelter.ShelterInput;
import ru.lab.shelter.dto.user.UserInput;
import ru.lab.shelter.dto.user.UserUpdate;
import ru.lab.shelter.model.Role;

class CatalogApiIT extends IntegrationTestSupport {
  @org.junit.jupiter.api.Test
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
}
