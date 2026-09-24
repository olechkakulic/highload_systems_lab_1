package ru.lab.shelter.dto.volunteer;

import java.time.Instant;
import ru.lab.shelter.model.ShiftStatus;

public record ShiftView(
    Long id,
    Long shelterId,
    String title,
    Instant startsAt,
    Instant endsAt,
    int capacity,
    ShiftStatus status) {}
