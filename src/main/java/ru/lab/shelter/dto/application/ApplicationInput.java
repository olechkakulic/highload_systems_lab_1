package ru.lab.shelter.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ApplicationInput(
    @NotNull @Positive Long animalId,
    @NotNull @Positive Long applicantId,
    @NotBlank @Size(max = 2000) String comment) {}
