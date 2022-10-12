package org.example.test;


import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * @author Kuznetsovka created 14.07.2022
 */
@Entity(name = "students")
@NamedQuery(name = "Student.getBySurname", query = "select e from students e where e.surname=:surname")
public class Student {
  @Id
  public Long id;

  public String name;

  public String surname;

  public LocalDateTime birthday;
  @OneToMany(fetch = FetchType.LAZY)
  List<Course> courses;

  public Student(Long id, String name, String surname) {
    this.id = id;
    this.name = name;
    this.surname = surname;
  }

  public Student(String name, String surname) {
    this.name = name;
    this.surname = surname;
  }

  public Student() {
  }

  public Student(Long id, String name, String surname, LocalDateTime birthday, List<Course> courses) {
    this.id = id;
    this.name = name;
    this.surname = surname;
    this.birthday = birthday;
    this.courses = courses;
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

  public List<Course> getCourses() {
    return courses;
  }

  public void setCourses(List<Course> courses) {
    this.courses = courses;
  }
}
