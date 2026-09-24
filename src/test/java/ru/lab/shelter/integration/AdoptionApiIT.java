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

  @org.junit.jupiter.api.Test
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
}
