package ru.lab.shelter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "volunteer_shifts")
public class VolunteerShift extends BaseEntity {
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "shelter_id", nullable = false)
  private Shelter shelter;

  @NotBlank
  @Size(max = 160)
  @Column(nullable = false, length = 160)
  private String title;

  @NotNull
  @Column(nullable = false)
  private Instant startsAt;

  @NotNull
  @Column(nullable = false)
  private Instant endsAt;

  @Min(1)
  @Max(1000)
  @Column(nullable = false)
  private int capacity;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ShiftStatus status = ShiftStatus.SCHEDULED;

  public Shelter getShelter() {
    return shelter;
  }

  public void setShelter(Shelter shelter) {
    this.shelter = shelter;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(Instant startsAt) {
    this.startsAt = startsAt;
  }

  public Instant getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(Instant endsAt) {
    this.endsAt = endsAt;
  }

  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public ShiftStatus getStatus() {
    return status;
  }

  public void setStatus(ShiftStatus status) {
    this.status = status;
  }

  @AssertTrue(message = "Окончание смены должно быть позже начала")
  public boolean isTimeRangeValid() {
    return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
  }
}
