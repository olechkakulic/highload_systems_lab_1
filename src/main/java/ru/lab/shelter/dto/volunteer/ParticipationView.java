package ru.lab.shelter.dto.volunteer;

import java.time.Instant;
import ru.lab.shelter.model.ParticipationStatus;

public record ParticipationView(
    Long id, Long shiftId, Long userId, ParticipationStatus status, Instant registeredAt) {}
