package ru.lab.shelter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.lab.shelter.dto.application.ApplicationInput;
import ru.lab.shelter.dto.application.ApplicationUpdate;
import ru.lab.shelter.dto.application.ApplicationView;
import ru.lab.shelter.dto.application.ReviewInput;
import ru.lab.shelter.dto.common.ListResult;
import ru.lab.shelter.service.AdoptionService;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "applications")
public class AdoptionController {
  private final AdoptionService service;

  public AdoptionController(AdoptionService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<ListResult<ApplicationView>> list(
      @RequestParam(required = false) Long animalId,
      @RequestParam(required = false) Long applicantId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return Pagination.page(service.list(animalId, applicantId, Pagination.request(page, size)));
  }

  @GetMapping("/{id}")
  public ApplicationView get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<ApplicationView> create(@Valid @RequestBody ApplicationInput input) {
    var result = service.create(input);
    return ResponseEntity.created(URI.create("/api/applications/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public ApplicationView update(
      @PathVariable Long id, @Valid @RequestBody ApplicationUpdate input) {
    return service.update(id, input);
  }

  @PostMapping("/{id}/review")
  public ApplicationView review(@PathVariable Long id, @Valid @RequestBody ReviewInput input) {
    return service.review(id, input);
  }

  @PostMapping("/{id}/withdraw")
  public ApplicationView withdraw(@PathVariable Long id) {
    return service.withdraw(id);
  }

  @PostMapping("/{id}/complete")
  @Operation(
      summary = "Передать животное владельцу",
      description =
          "Атомарно меняет статус животного, завершает одобренную заявку и отклоняет остальные"
              + " активные заявки. Блокирует строку животного на время транзакции.")
  public ApplicationView complete(@PathVariable Long id) {
    return service.complete(id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }
}
