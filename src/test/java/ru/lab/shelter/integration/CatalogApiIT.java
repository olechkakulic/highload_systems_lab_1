package ru.lab.shelter.integration;

import static org.assertj.core.api.Assertions.*;

import ru.lab.shelter.dto.shelter.ShelterInput;
import ru.lab.shelter.dto.user.UserInput;
import ru.lab.shelter.dto.user.UserUpdate;
import ru.lab.shelter.model.Role;

class CatalogApiIT extends IntegrationTestSupport {
  @org.junit.jupiter.api.Test
  void catalogCrudAndRelationshipConstraints() throws Exception {
    call(PUT, shelterPath(shelter), new ShelterInput("Новый дом", "Адрес 2"), HTTP_OK);
    assertThat(call(GET, shelterPath(shelter), null, HTTP_OK).get("name").asText())
        .isEqualTo("Новый дом");
    call(GET, API_SHELTERS, null, HTTP_OK);
    long employee =
        create(
            API_USERS,
            new UserInput("Сотрудник", "EMPLOYEE@example.org", Role.EMPLOYEE, shelter));
    assertThat(call(GET, userPath(employee), null, HTTP_OK).get("email").asText())
        .isEqualTo("employee@example.org");
    call(
        PUT,
        userPath(employee),
        new UserUpdate("Имя", "renamed@example.org", shelter),
        HTTP_OK);
    call(GET, API_USERS, null, HTTP_OK);
    call(
        POST,
        API_USERS,
        new UserInput("Без приюта", "bad@example.org", Role.EMPLOYEE, null),
        HTTP_BAD_REQUEST);
    call(PUT, userPath(employee), new UserUpdate("Имя", "renamed@example.org", null), HTTP_BAD_REQUEST);
    call(
        POST,
        API_USERS,
        new UserInput("Повтор", "RENAMED@example.org", Role.APPLICANT, null),
        HTTP_CONFLICT);
    call(DELETE, shelterPath(shelter), null, HTTP_CONFLICT);
    call(DELETE, userPath(employee), null, HTTP_NO_CONTENT);
    call(DELETE, shelterPath(shelter), null, HTTP_NO_CONTENT);
    call(GET, shelterPath(shelter), null, HTTP_NOT_FOUND);
  }
}
