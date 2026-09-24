package ru.lab.shelter.dto.animal;

import java.time.LocalDate;
import java.util.List;
import ru.lab.shelter.dto.tag.TagView;
import ru.lab.shelter.model.AnimalStatus;
import ru.lab.shelter.model.Species;

public record AnimalView(
    Long id,
    String name,
    Species species,
    LocalDate birthDate,
    String description,
    AnimalStatus status,
    Long shelterId,
    List<TagView> tags) {}
