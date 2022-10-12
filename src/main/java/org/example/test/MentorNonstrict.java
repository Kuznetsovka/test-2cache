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
 * Если маловероятно, что два отдельных потока транзакций могут обновлять один и тот же объект,
 * вы можете использовать стратегию нестрочного чтения-записи. У него меньше накладных расходов,
 * чем чтение-запись. Это полезно когда данные редко обновляются.
 */

@Entity(name = "mentors_nonstrict")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
//@OptimisticLocking(type = OptimisticLockType.ALL)
//@DynamicUpdate
public class MentorNonstrict implements MentorNameable {

  @Id
  public Long id;

  public String name;

  public String surname;

  public LocalDateTime birthday;

  @OneToMany(fetch = FetchType.LAZY)
  @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
  public List<Student> students;

  public MentorNonstrict() {
  }

  public MentorNonstrict(Long id, String name, String surname, LocalDateTime birthday) {
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

  public List<Student> getStudents() {
    return students;
  }

  public void setStudents(List<Student> students) {
    this.students = students;
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
