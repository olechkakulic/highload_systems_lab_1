package ru.lab.shelter.dto.application;

import java.time.Instant;
import ru.lab.shelter.model.ApplicationStatus;

public record ApplicationView(
    Long id,
    Long animalId,
    Long applicantId,
    String comment,
    ApplicationStatus status,
    Instant createdAt) {}
