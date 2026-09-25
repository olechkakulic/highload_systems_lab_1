package ru.lab.shelter.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Set;
import ru.lab.shelter.dto.application.ApplicationInput;
import ru.lab.shelter.dto.application.ApplicationUpdate;
import ru.lab.shelter.dto.application.ReviewDecision;
import ru.lab.shelter.dto.application.ReviewInput;
import ru.lab.shelter.model.Role;

class AdoptionApiIT extends IntegrationTestSupport {
  @org.junit.jupiter.api.Test
  void adoptionCompletesAllChangesAndRejectsConflictingOperations() throws Exception {
    long animal = animal(),
        u1 = user("first@example.org", Role.APPLICANT),
        u2 = user("second@example.org", Role.APPLICANT);
    long a1 = application(animal, u1), a2 = application(animal, u2);
    call(POST, API_APPLICATIONS, new ApplicationInput(animal, u1, "Повтор"), HTTP_CONFLICT);
    call(PUT, applicationPath(a1), new ApplicationUpdate("Дополнение"), HTTP_OK);
    call(POST, applicationCompletePath(a1), null, HTTP_CONFLICT);
    call(DELETE, applicationPath(a1), null, HTTP_CONFLICT);
    approve(a1);
    call(
        POST,
        applicationReviewPath(a2),
        new ReviewInput(ReviewDecision.APPROVE),
        HTTP_CONFLICT);
    call(
        POST,
        applicationReviewPath(a1),
        new ReviewInput(ReviewDecision.APPROVE),
        HTTP_CONFLICT);
    call(PUT, applicationPath(a1), new ApplicationUpdate("Поздно"), HTTP_CONFLICT);
    call(POST, applicationCompletePath(a1), null, HTTP_OK);
    assertThat(call(GET, animalPath(animal), null, HTTP_OK).get("status").asText())
        .isEqualTo("ADOPTED");
    assertThat(call(GET, applicationPath(a1), null, HTTP_OK).get("status").asText())
        .isEqualTo("COMPLETED");
    assertThat(call(GET, applicationPath(a2), null, HTTP_OK).get("status").asText())
        .isEqualTo("REJECTED");
    request(GET, API_APPLICATIONS + "?animalId=" + animal + "&applicantId=" + u1, null)
        .andExpect(header().string("X-Total-Count", "1"));
    call(POST, applicationCompletePath(a1), null, HTTP_CONFLICT);
    call(POST, API_APPLICATIONS, new ApplicationInput(animal, u2, "Поздно"), HTTP_CONFLICT);
    call(POST, applicationWithdrawPath(a1), null, HTTP_CONFLICT);
    call(PUT, animalPath(animal), animalInput("Поздно", Set.of()), HTTP_CONFLICT);
    call(DELETE, animalPath(animal), null, HTTP_CONFLICT);
    call(DELETE, applicationPath(a1), null, HTTP_CONFLICT);
  }

  @org.junit.jupiter.api.Test
  void applicationRejectionWithdrawalAndDeletion() throws Exception {
    long animal = animal(), user = user("owner@example.org", Role.APPLICANT);
    long a = application(animal, user);
    call(POST, applicationReviewPath(a), new ReviewInput(ReviewDecision.REJECT), HTTP_OK);
    call(POST, applicationReviewPath(a), new ReviewInput(ReviewDecision.REJECT), HTTP_CONFLICT);
    call(DELETE, applicationPath(a), null, HTTP_NO_CONTENT);
    a = application(animal, user);
    call(POST, applicationWithdrawPath(a), null, HTTP_OK);
    call(DELETE, applicationPath(a), null, HTTP_NO_CONTENT);
    call(GET, applicationPath(a), null, HTTP_NOT_FOUND);
    long volunteer = user("volunteer@example.org", Role.VOLUNTEER);
    call(
        POST,
        API_APPLICATIONS,
        new ApplicationInput(animal, volunteer, "Не заявитель"),
        HTTP_CONFLICT);
  }
}
