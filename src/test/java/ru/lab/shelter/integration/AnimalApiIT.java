package ru.lab.shelter.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Set;
import ru.lab.shelter.dto.tag.TagInput;

class AnimalApiIT extends IntegrationTestSupport {
  @org.junit.jupiter.api.Test
  void animalsAndTagsCrudFilteringAndStringEnums() throws Exception {
    long tag = create(API_TAGS, new TagInput("Спокойный"));
    call(GET, tagPath(tag), null, HTTP_OK);
    call(GET, API_TAGS, null, HTTP_OK);
    call(PUT, tagPath(tag), new TagInput("Ласковый"), HTTP_OK);
    call(POST, API_TAGS, new TagInput("ЛАСКОВЫЙ"), HTTP_CONFLICT);
    long animal = create(API_ANIMALS, animalInput("Мурка", Set.of(tag)));
    var view = call(GET, animalPath(animal), null, HTTP_OK);
    assertThat(view.get("tags").get(0).get("id").asLong()).isEqualTo(tag);
    assertThat(jdbc.queryForObject("select species from animals where id=?", String.class, animal))
        .isEqualTo("CAT");
    request(
            GET,
            API_ANIMALS + "?shelterId=" + shelter + "&species=CAT&status=AVAILABLE&tagId=" + tag,
            null)
        .andExpect(status().isOk())
        .andExpect(header().string("X-Total-Count", "1"))
        .andExpect(jsonPath("$.items.length()").value(1));
    request(GET, API_ANIMALS + "?species=DOG", null)
        .andExpect(header().string("X-Total-Count", "0"));
    call(PUT, animalPath(animal), animalInput("Новое имя", Set.of()), HTTP_OK);
    call(POST, API_ANIMALS, animalInput("Нет тега", Set.of(999L)), HTTP_BAD_REQUEST);
    call(DELETE, tagPath(tag), null, HTTP_NO_CONTENT);
    call(DELETE, animalPath(animal), null, HTTP_NO_CONTENT);
    call(GET, animalPath(animal), null, HTTP_NOT_FOUND);
  }

  @org.junit.jupiter.api.Test
  void deletingTagUnlinksManyToManyWithoutDeletingAnimal() throws Exception {
    long tag = create(API_TAGS, new TagInput("Тег"));
    long animal = create(API_ANIMALS, animalInput("Пёс", Set.of(tag)));
    call(DELETE, tagPath(tag), null, HTTP_NO_CONTENT);
    assertThat(call(GET, animalPath(animal), null, HTTP_OK).get("tags")).isEmpty();
  }
}
