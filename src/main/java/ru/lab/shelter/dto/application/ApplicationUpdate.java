package ru.lab.shelter.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationUpdate(@NotBlank @Size(max = 2000) String comment) {}
