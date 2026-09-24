package ru.lab.shelter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "users")
public class UserProfile extends BaseEntity {
  @NotBlank
  @Size(max = 120)
  @Column(nullable = false, length = 120)
  private String name;

  @NotBlank
  @Email
  @Size(max = 254)
  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Role role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shelter_id")
  private Shelter shelter;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public Shelter getShelter() {
    return shelter;
  }

  public void setShelter(Shelter shelter) {
    this.shelter = shelter;
  }

  @AssertTrue(message = "Сотрудник должен быть привязан к приюту")
  public boolean isEmployeeShelterValid() {
    return role != Role.EMPLOYEE || shelter != null;
  }
}
