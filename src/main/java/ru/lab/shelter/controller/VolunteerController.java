package ru.lab.shelter.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.lab.shelter.dto.common.ListResult;
import ru.lab.shelter.dto.volunteer.ParticipationInput;
import ru.lab.shelter.dto.volunteer.ParticipationView;
import ru.lab.shelter.dto.volunteer.ShiftInput;
import ru.lab.shelter.dto.volunteer.ShiftView;
import ru.lab.shelter.model.ShiftStatus;
import ru.lab.shelter.service.VolunteerService;

@RestController
@RequestMapping("/api/shifts")
@Tag(name = "volunteering")
public class VolunteerController {
  private final VolunteerService service;

  public VolunteerController(VolunteerService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(
      summary = "Смены для бесконечной прокрутки",
      description =
          "Slice без подсчёта общего количества. Используйте hasNext и увеличивайте page. size от 1"
              + " до 50.")
  public ListResult<ShiftView> list(
      @RequestParam(required = false) Long shelterId,
      @RequestParam(required = false) ShiftStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return Pagination.slice(service.list(shelterId, status, Pagination.request(page, size)));
  }

  @GetMapping("/{id}")
  public ShiftView get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  public ResponseEntity<ShiftView> create(@Valid @RequestBody ShiftInput input) {
    var result = service.create(input);
    return ResponseEntity.created(URI.create("/api/shifts/" + result.id())).body(result);
  }

  @PutMapping("/{id}")
  public ShiftView update(@PathVariable Long id, @Valid @RequestBody ShiftInput input) {
    return service.update(id, input);
  }

  @PostMapping("/{id}/cancel")
  public ShiftView cancel(@PathVariable Long id) {
    return service.cancel(id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }

  @GetMapping("/{shiftId}/participations")
  public ResponseEntity<ListResult<ParticipationView>> participants(
      @PathVariable Long shiftId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return Pagination.page(service.participants(shiftId, Pagination.request(page, size)));
  }

  @GetMapping("/{shiftId}/participations/{id}")
  public ParticipationView participation(@PathVariable Long shiftId, @PathVariable Long id) {
    return service.getParticipation(shiftId, id);
  }

  @PostMapping("/{shiftId}/participations")
  @Operation(
      summary = "Записать волонтёра",
      description =
          "Проверка вместимости и запись выполняются под блокировкой смены. Отменённая запись"
              + " восстанавливается с тем же id и ответом 200; новая получает 201.")
  public ResponseEntity<ParticipationView> enroll(
      @PathVariable Long shiftId, @Valid @RequestBody ParticipationInput input) {
    var result = service.enroll(shiftId, input);
    return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .location(
            URI.create("/api/shifts/" + shiftId + "/participations/" + result.participation().id()))
        .body(result.participation());
  }

  @PostMapping("/{shiftId}/participations/{id}/cancel")
  public ParticipationView cancelParticipation(@PathVariable Long shiftId, @PathVariable Long id) {
    return service.cancelParticipation(shiftId, id);
  }

  @DeleteMapping("/{shiftId}/participations/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteParticipation(@PathVariable Long shiftId, @PathVariable Long id) {
    service.deleteParticipation(shiftId, id);
  }
}
