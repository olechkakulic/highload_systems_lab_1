package ru.lab.shelter.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "shelters")
public class Shelter extends BaseEntity {
  @NotBlank
  @Size(max = 120)
  @Column(nullable = false, length = 120)
  private String name;

  @NotBlank
  @Size(max = 300)
  @Column(nullable = false, length = 300)
  private String address;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }
}
