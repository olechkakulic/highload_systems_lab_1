package ru.lab.shelter.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import ru.lab.shelter.dto.volunteer.ParticipationInput;
import ru.lab.shelter.exception.ApiException;
import ru.lab.shelter.model.Role;

class TransactionConcurrencyIT extends IntegrationTestSupport {
  @org.junit.jupiter.api.Test
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

  @org.junit.jupiter.api.Test
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

  @org.junit.jupiter.api.Test
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
