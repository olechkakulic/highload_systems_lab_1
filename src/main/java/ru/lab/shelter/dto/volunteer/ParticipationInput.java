package ru.lab.shelter.dto.volunteer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ParticipationInput(@NotNull @Positive Long userId) {}
