package ru.lab.shelter.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Set;
import ru.lab.shelter.dto.tag.TagInput;

class AnimalApiIT extends IntegrationTestSupport {
  @org.junit.jupiter.api.Test
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

  @org.junit.jupiter.api.Test
  void deletingTagUnlinksManyToManyWithoutDeletingAnimal() throws Exception {
    long tag = create("/api/tags", new TagInput("Тег"));
    long animal = create("/api/animals", animalInput("Пёс", Set.of(tag)));
    call("DELETE", "/api/tags/" + tag, null, 204);
    assertThat(call("GET", "/api/animals/" + animal, null, 200).get("tags")).isEmpty();
  }
}
