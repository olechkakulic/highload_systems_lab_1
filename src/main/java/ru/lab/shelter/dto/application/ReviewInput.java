package ru.lab.shelter.dto.application;

import jakarta.validation.constraints.NotNull;

public record ReviewInput(@NotNull ReviewDecision decision) {}
