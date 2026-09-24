package ru.lab.shelter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "shift_participations")
public class ShiftParticipation extends BaseEntity {
  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "shift_id", nullable = false)
  private VolunteerShift shift;

  @NotNull
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserProfile user;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ParticipationStatus status = ParticipationStatus.REGISTERED;

  @NotNull
  @Column(nullable = false)
  private Instant registeredAt = Instant.now();

  public VolunteerShift getShift() {
    return shift;
  }

  public void setShift(VolunteerShift shift) {
    this.shift = shift;
  }

  public UserProfile getUser() {
    return user;
  }

  public void setUser(UserProfile user) {
    this.user = user;
  }

  public ParticipationStatus getStatus() {
    return status;
  }

  public void setStatus(ParticipationStatus status) {
    this.status = status;
  }

  public Instant getRegisteredAt() {
    return registeredAt;
  }

  public void setRegisteredAt(Instant registeredAt) {
    this.registeredAt = registeredAt;
  }
}
