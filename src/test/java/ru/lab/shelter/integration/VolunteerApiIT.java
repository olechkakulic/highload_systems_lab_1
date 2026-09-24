package ru.lab.shelter.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ru.lab.shelter.dto.volunteer.ParticipationInput;
import ru.lab.shelter.model.Role;

class VolunteerApiIT extends IntegrationTestSupport {
  @org.junit.jupiter.api.Test
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
}
