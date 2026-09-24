package ru.lab.shelter.dto;

import java.util.Comparator;
import ru.lab.shelter.dto.animal.AnimalView;
import ru.lab.shelter.dto.application.ApplicationView;
import ru.lab.shelter.dto.shelter.ShelterView;
import ru.lab.shelter.dto.tag.TagView;
import ru.lab.shelter.dto.user.UserView;
import ru.lab.shelter.dto.volunteer.ParticipationView;
import ru.lab.shelter.dto.volunteer.ShiftView;
import ru.lab.shelter.model.*;

public final class DtoMapper {
  private DtoMapper() {}

  public static ShelterView shelter(Shelter e) {
    return new ShelterView(e.getId(), e.getName(), e.getAddress());
  }

  public static UserView user(UserProfile e) {
    return new UserView(
        e.getId(),
        e.getName(),
        e.getEmail(),
        e.getRole(),
        e.getShelter() == null ? null : e.getShelter().getId());
  }

  public static TagView tag(Tag e) {
    return new TagView(e.getId(), e.getName());
  }

  public static AnimalView animal(Animal e) {
    return new AnimalView(
        e.getId(),
        e.getName(),
        e.getSpecies(),
        e.getBirthDate(),
        e.getDescription(),
        e.getStatus(),
        e.getShelter().getId(),
        e.getTags().stream()
            .map(DtoMapper::tag)
            .sorted(Comparator.comparing(TagView::id))
            .toList());
  }

  public static ApplicationView application(AdoptionApplication e) {
    return new ApplicationView(
        e.getId(),
        e.getAnimal().getId(),
        e.getApplicant().getId(),
        e.getComment(),
        e.getStatus(),
        e.getCreatedAt());
  }

  public static ShiftView shift(VolunteerShift e) {
    return new ShiftView(
        e.getId(),
        e.getShelter().getId(),
        e.getTitle(),
        e.getStartsAt(),
        e.getEndsAt(),
        e.getCapacity(),
        e.getStatus());
  }

  public static ParticipationView participation(ShiftParticipation e) {
    return new ParticipationView(
        e.getId(), e.getShift().getId(), e.getUser().getId(), e.getStatus(), e.getRegisteredAt());
  }
}
