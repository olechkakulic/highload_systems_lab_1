package ru.lab.shelter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.lab.shelter.dto.animal.AnimalInput;
import ru.lab.shelter.dto.animal.AnimalView;
import ru.lab.shelter.dto.common.ListResult;
import ru.lab.shelter.model.AnimalStatus;
import ru.lab.shelter.model.Species;
import ru.lab.shelter.service.AnimalService;

@RestController
@RequestMapping("/api/animals")
@Tag(name = "animals")
public class AnimalController {
  private final AnimalService service;

  public AnimalController(AnimalService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(
      summary = "Каталог животных",
      description =
          "Фильтры объединяются по AND. page от 0, size от 1 до 50. Общее число совпадений — в"
              + " заголовке X-Total-Count.")
  public ResponseEntity<ListResult<AnimalView>> list(
      @RequestParam(required = false) Long shelterId,
      @RequestParam(required = false) Species species,
      @RequestParam(required = false) AnimalStatus status,
      @RequestParam(required = false) Long tagId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return Pagination.page(
        service.list(shelterId, species, status, tagId, Pagination.request(page, size)));
  }

  @GetMapping("/{id}")
  public AnimalView get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<AnimalView> create(@Valid @RequestBody AnimalInput input) {
    var result = service.create(input);
    return ResponseEntity.created(URI.create("/api/animals/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public AnimalView update(@PathVariable Long id, @Valid @RequestBody AnimalInput input) {
    return service.update(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
