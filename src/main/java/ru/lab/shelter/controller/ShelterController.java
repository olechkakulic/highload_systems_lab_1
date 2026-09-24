package ru.lab.shelter.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.lab.shelter.dto.common.ListResult;
import ru.lab.shelter.dto.shelter.ShelterInput;
import ru.lab.shelter.dto.shelter.ShelterView;
import ru.lab.shelter.service.CatalogService;

@RestController
@RequestMapping("/api/shelters")
@Tag(name = "shelters")
public class ShelterController {
  private final CatalogService service;

  public ShelterController(CatalogService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<ListResult<ShelterView>> list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return Pagination.page(service.shelters(Pagination.request(page, size)));
  }

  @GetMapping("/{id}")
  public ShelterView get(@PathVariable Long id) {
    return service.shelter(id);
  }

  @PostMapping
  public ResponseEntity<ShelterView> create(@Valid @RequestBody ShelterInput input) {
    var result = service.createShelter(input);
    return ResponseEntity.created(URI.create("/api/shelters/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public ShelterView update(@PathVariable Long id, @Valid @RequestBody ShelterInput input) {
    return service.updateShelter(id, input);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.deleteShelter(id);
  }
}
