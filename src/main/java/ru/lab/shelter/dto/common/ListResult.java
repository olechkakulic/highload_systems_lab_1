package ru.lab.shelter.dto.common;

import java.util.List;

public record ListResult<T>(List<T> items, int page, int size, boolean hasNext) {}
