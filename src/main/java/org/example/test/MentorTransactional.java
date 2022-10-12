package org.example.test;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * @author Kuznetsovka created 14.07.2022
 * Transactional стратегия кэширования предоставляет поддержку для транзакционных кэш-провайдеров,
 * таких как JBoss TreeCache и EHCACHE(Hibernate 5.0, d 3-м не поддерживало). Использовать такой кэш вы можете
 * только в JTA-окружении, и нужно будет указать hibernate.transaction.manager_lookup_class.
 *
 * Если нам нужен полностью транзакционный кеш. Подходит только в среде JTA.
 */

@Entity(name = "mentors_transaction")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
public class MentorTransactional implements MentorNameable {

  @Id
  public Long id;

  public String name;

  public String surname;

  public LocalDateTime birthday;

  public MentorTransactional() {
  }

  public MentorTransactional(Long id, String name, String surname, LocalDateTime birthday) {
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
