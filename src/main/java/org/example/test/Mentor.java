package org.example.test;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * @author Kuznetsovka created 14.07.2022
 */

@Entity(name = "mentors")
@Cacheable
/*
* @Cache(include = )
* include(необязательно: по умолчанию all)
* non-lazy: указывает, что свойства объекта, сопоставленные с lazy="true",
* не могут быть кэшированы, если включена отложенная выборка на уровне атрибута.
* READ_WRITE не обеспечивает уровень изоляции SERIALIZABLE
* */
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedQuery(name = "Mentor.getBySurname", query = "select e from mentors e where e.surname=:surname",
    hints = { @QueryHint(name = "org.hibernate.cacheable", value = "true") })
@OptimisticLocking(type = OptimisticLockType.ALL)
@DynamicUpdate
public class Mentor implements MentorNameable {
  @Id
  public Long id;

  public String name;

  public String surname;

  public LocalDateTime birthday;

  /*   */
  @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
  @OneToMany(fetch = FetchType.LAZY)
  public List<Student> students;

  public Mentor() {
  }

  public Mentor(Long id, String name, String surname) {
    this.id = id;
    this.name = name;
    this.surname = surname;
  }

  public Mentor(Long id, String name, String surname, LocalDateTime birthday, List<Student> students) {
    this.id = id;
    this.name = name;
    this.surname = surname;
    this.birthday = birthday;
    this.students = students;
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

  public List<Student> getStudents() {
    return students;
  }

  public void setStudents(List<Student> students) {
    this.students = students;
  }

}
