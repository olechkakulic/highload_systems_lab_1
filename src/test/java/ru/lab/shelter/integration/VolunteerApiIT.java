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
    call(GET, shiftPath(shift), null, HTTP_OK);
    call(PUT, shiftPath(shift), shiftInput(2), HTTP_OK);
    long p1 = create(participationsPath(shift), new ParticipationInput(u1));
    long p2 = create(participationsPath(shift), new ParticipationInput(u2));
    call(PUT, shiftPath(shift), shiftInput(1), HTTP_CONFLICT);
    call(POST, participationsPath(shift), new ParticipationInput(u1), HTTP_CONFLICT);
    long u3 = user("v3@example.org", Role.VOLUNTEER);
    call(POST, participationsPath(shift), new ParticipationInput(u3), HTTP_CONFLICT);
    long applicant = user("applicant@example.org", Role.APPLICANT);
    call(
        POST, participationsPath(shift), new ParticipationInput(applicant), HTTP_CONFLICT);
    call(GET, participationPath(shift, p1), null, HTTP_OK);
    request(GET, participationsPath(shift), null)
        .andExpect(header().string("X-Total-Count", "2"));
    call(DELETE, participationPath(shift, p1), null, HTTP_CONFLICT);
    call(DELETE, shiftPath(shift), null, HTTP_CONFLICT);
    call(POST, participationCancelPath(shift, p1), null, HTTP_OK);
    call(POST, participationCancelPath(shift, p1), null, HTTP_CONFLICT);
    var restored =
        call(POST, participationsPath(shift), new ParticipationInput(u1), HTTP_OK);
    assertThat(restored.get("id").asLong()).isEqualTo(p1);
    call(POST, shiftCancelPath(shift), null, HTTP_OK);
    assertThat(
            call(GET, participationPath(shift, p2), null, HTTP_OK)
                .get("status")
                .asText())
        .isEqualTo("CANCELLED");
    call(POST, participationsPath(shift), new ParticipationInput(u3), HTTP_CONFLICT);
    call(DELETE, participationPath(shift, p1), null, HTTP_NO_CONTENT);
    call(DELETE, participationPath(shift, p2), null, HTTP_NO_CONTENT);
    call(DELETE, shiftPath(shift), null, HTTP_NO_CONTENT);
    call(GET, shiftPath(shift), null, HTTP_NOT_FOUND);
  }
}
