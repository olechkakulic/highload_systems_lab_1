package ru.lab.shelter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.lab.shelter.dto.application.ApplicationInput;
import ru.lab.shelter.dto.volunteer.ParticipationInput;
import ru.lab.shelter.dto.volunteer.ShiftInput;
import ru.lab.shelter.exception.ApiException;
import ru.lab.shelter.model.*;
import ru.lab.shelter.repository.*;
import ru.lab.shelter.service.*;

class BusinessRulesTest {
  private VolunteerShiftRepository shifts;
  private ShiftParticipationRepository participations;
  private CatalogService catalog;
  private VolunteerService service;

  @BeforeEach
  void setup() {
    shifts = mock(VolunteerShiftRepository.class);
    participations = mock(ShiftParticipationRepository.class);
    catalog = mock(CatalogService.class);
    service = new VolunteerService(shifts, participations, catalog);
  }

  @Test
  void fullShiftCannotCreateParticipation() {
    VolunteerShift shift = new VolunteerShift();
    shift.setStartsAt(Instant.now().plusSeconds(3600));
    shift.setCapacity(1);
    UserProfile volunteer = new UserProfile();
    volunteer.setRole(Role.VOLUNTEER);
    when(shifts.findLockedById(1L)).thenReturn(Optional.of(shift));
    when(catalog.requireUser(2L)).thenReturn(volunteer);
    when(participations.findByShiftIdAndUserId(1L, null)).thenReturn(Optional.empty());
    when(participations.countByShiftIdAndStatus(1L, ParticipationStatus.REGISTERED)).thenReturn(1L);
    assertThatThrownBy(() -> service.enroll(1L, new ParticipationInput(2L)))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("нет свободных мест");
    verify(participations, never()).save(any());
  }

  @Test
  void cancelledShiftDoesNotEvenLookUpUser() {
    VolunteerShift shift = new VolunteerShift();
    shift.setStatus(ShiftStatus.CANCELLED);
    when(shifts.findLockedById(1L)).thenReturn(Optional.of(shift));
    assertThatThrownBy(() -> service.enroll(1L, new ParticipationInput(2L)))
        .isInstanceOf(ApiException.class);
    verifyNoInteractions(catalog, participations);
  }

  @Test
  void invalidTimeRangeDoesNotWrite() {
    Instant later = Instant.now().plusSeconds(3600);
    assertThatThrownBy(() -> service.create(new ShiftInput(1L, "Смена", later, later, 3)))
        .isInstanceOf(ApiException.class);
    verify(shifts, never()).save(any());
  }

  @Test
  void adoptedAnimalCannotReceiveApplications() {
    AnimalRepository animals = mock(AnimalRepository.class);
    AdoptionApplicationRepository applications = mock(AdoptionApplicationRepository.class);
    Animal animal = new Animal();
    animal.setStatus(AnimalStatus.ADOPTED);
    when(animals.findLockedById(1L)).thenReturn(Optional.of(animal));
    var adoption = new AdoptionService(applications, animals, catalog);
    assertThatThrownBy(() -> adoption.create(new ApplicationInput(1L, 2L, "Готов забрать")))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("уже передано");
    verifyNoInteractions(applications, catalog);
  }
}
