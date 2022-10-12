package org.example.test;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * @author Kuznetsovka created 14.07.2022
 */

@Entity(name = "mentors_read_only")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class MentorReadOnly implements MentorNameable {

  @Id
  public Long id;

  public String name;

  public String surname;

  public LocalDateTime birthday;

  public MentorReadOnly() {
  }

  public MentorReadOnly(Long id, String name, String surname, LocalDateTime birthday) {
    this.id = id;
    this.name = name;
    this.surname = surname;
    this.birthday = birthday;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public LocalDateTime getBirthday() {
    return birthday;
  }

  public void setBirthday(LocalDateTime birthday) {
    this.birthday = birthday;
  }

}
