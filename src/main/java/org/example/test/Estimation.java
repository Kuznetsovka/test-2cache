package org.example.test;

import javax.persistence.*;

/**
 * @author Kuznetsovka created 14.07.2022
 */
@Entity(name = "estimations")
public class Estimation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  public Estimation() {
  }

  public Estimation(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}
