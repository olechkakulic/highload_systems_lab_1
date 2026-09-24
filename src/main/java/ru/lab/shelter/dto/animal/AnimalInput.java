package ru.lab.shelter.dto.animal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;
import ru.lab.shelter.model.Species;

public record AnimalInput(
    @NotBlank @Size(max = 120) String name,
    @NotNull Species species,
    @NotNull @PastOrPresent LocalDate birthDate,
    @NotBlank @Size(max = 2000) String description,
    @NotNull @Positive Long shelterId,
    @NotNull @Size(max = 50) Set<@NotNull @Positive Long> tagIds) {}
