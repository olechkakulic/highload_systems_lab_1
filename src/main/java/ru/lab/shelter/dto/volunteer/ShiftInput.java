package ru.lab.shelter.dto.volunteer;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ShiftInput(
    @NotNull @Positive Long shelterId,
    @NotBlank @Size(max = 160) String title,
    @NotNull @Future Instant startsAt,
    @NotNull @Future Instant endsAt,
    @Min(1) @Max(1000) int capacity) {}
