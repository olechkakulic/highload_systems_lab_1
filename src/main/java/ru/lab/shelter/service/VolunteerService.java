package ru.lab.shelter.service;

import java.time.Instant;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.lab.shelter.dto.DtoMapper;
import ru.lab.shelter.dto.volunteer.ParticipationInput;
import ru.lab.shelter.dto.volunteer.ParticipationView;
import ru.lab.shelter.dto.volunteer.ShiftInput;
import ru.lab.shelter.dto.volunteer.ShiftView;
import ru.lab.shelter.exception.ApiException;
import ru.lab.shelter.model.*;
import ru.lab.shelter.repository.*;

@Service
@Transactional(readOnly = true)
public class VolunteerService {
  private final VolunteerShiftRepository shifts;
  private final ShiftParticipationRepository participations;
  private final CatalogService catalog;

  public VolunteerService(
      VolunteerShiftRepository shifts,
      ShiftParticipationRepository participations,
      CatalogService catalog) {
    this.shifts = shifts;
    this.participations = participations;
    this.catalog = catalog;
  }

  private VolunteerShift require(Long id) {
    return shifts.findById(id).orElseThrow(() -> ApiException.notFound("Смена", id));
  }

  private VolunteerShift lock(Long id) {
    return shifts.findLockedById(id).orElseThrow(() -> ApiException.notFound("Смена", id));
  }

  private void requireOpen(VolunteerShift shift) {
    if (shift.getStatus() != ShiftStatus.SCHEDULED || !shift.getStartsAt().isAfter(Instant.now())) {
      throw ApiException.conflict("Смена отменена или уже началась");
    }
  }

  public Slice<ShiftView> list(Long shelterId, ShiftStatus status, Pageable page) {
    return shifts.search(shelterId, status, page).map(DtoMapper::shift);
  }

  public ShiftView get(Long id) {
    return DtoMapper.shift(require(id));
  }

  private void apply(VolunteerShift e, ShiftInput input) {
    if (!input.endsAt().isAfter(input.startsAt()) || !input.startsAt().isAfter(Instant.now())) {
      throw ApiException.badRequest("Начало должно быть в будущем, окончание — позже начала");
    }
    e.setShelter(catalog.requireShelter(input.shelterId()));
    e.setTitle(input.title().trim());
    e.setStartsAt(input.startsAt());
    e.setEndsAt(input.endsAt());
    e.setCapacity(input.capacity());
  }

  @Transactional
  public ShiftView create(ShiftInput input) {
    VolunteerShift e = new VolunteerShift();
    apply(e, input);
    return DtoMapper.shift(shifts.save(e));
  }

  @Transactional
  public ShiftView update(Long id, ShiftInput input) {
    VolunteerShift e = lock(id);
    requireOpen(e);
    if (input.capacity()
        < participations.countByShiftIdAndStatus(id, ParticipationStatus.REGISTERED)) {
      throw ApiException.conflict("Вместимость не может быть меньше количества записавшихся");
    }
    apply(e, input);
    return DtoMapper.shift(e);
  }

  @Transactional
  public ShiftView cancel(Long id) {
    VolunteerShift e = lock(id);
    requireOpen(e);
    e.setStatus(ShiftStatus.CANCELLED);
    participations.cancelAll(id, ParticipationStatus.CANCELLED, ParticipationStatus.REGISTERED);
    return DtoMapper.shift(e);
  }

  @Transactional
  public void delete(Long id) {
    VolunteerShift e = lock(id);
    shifts.delete(e);
  }

  public Page<ParticipationView> participants(Long shiftId, Pageable page) {
    require(shiftId);
    return participations.findByShiftId(shiftId, page).map(DtoMapper::participation);
  }

  private ShiftParticipation participation(Long shiftId, Long id) {
    return participations
        .findById(id)
        .filter(e -> e.getShift().getId().equals(shiftId))
        .orElseThrow(() -> ApiException.notFound("Участие в смене", id));
  }

  public ParticipationView getParticipation(Long shiftId, Long id) {
    return DtoMapper.participation(participation(shiftId, id));
  }

  public record EnrollmentResult(ParticipationView participation, boolean created) {}

  @Transactional
  public EnrollmentResult enroll(Long shiftId, ParticipationInput input) {
    // Lock the parent even if there are no participant rows yet. Serializes reservations.
    VolunteerShift shift = lock(shiftId);
    requireOpen(shift);
    UserProfile user = catalog.requireUser(input.userId());
    if (user.getRole() != Role.VOLUNTEER)
      throw ApiException.conflict("Записать на смену можно только волонтёра");
    ShiftParticipation e =
        participations
            .findByShiftIdAndUserId(shiftId, user.getId())
            .orElseGet(ShiftParticipation::new);
    if (e.getId() != null && e.getStatus() == ParticipationStatus.REGISTERED)
      throw ApiException.conflict("Волонтёр уже записан на смену");
    if (participations.countByShiftIdAndStatus(shiftId, ParticipationStatus.REGISTERED)
        >= shift.getCapacity()) {
      throw ApiException.conflict("На смене нет свободных мест");
    }
    boolean created = e.getId() == null;
    e.setShift(shift);
    e.setUser(user);
    e.setStatus(ParticipationStatus.REGISTERED);
    e.setRegisteredAt(Instant.now());
    return new EnrollmentResult(DtoMapper.participation(participations.save(e)), created);
  }

  @Transactional
  public ParticipationView cancelParticipation(Long shiftId, Long id) {
    VolunteerShift shift = lock(shiftId);
    requireOpen(shift);
    ShiftParticipation e = participation(shiftId, id);
    if (e.getStatus() != ParticipationStatus.REGISTERED)
      throw ApiException.conflict("Участие уже отменено");
    e.setStatus(ParticipationStatus.CANCELLED);
    return DtoMapper.participation(e);
  }

  @Transactional
  public void deleteParticipation(Long shiftId, Long id) {
    lock(shiftId);
    ShiftParticipation e = participation(shiftId, id);
    if (e.getStatus() != ParticipationStatus.CANCELLED)
      throw ApiException.conflict("Сначала отмените участие");
    participations.delete(e);
  }
}
