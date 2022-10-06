package org.example.test;

//import org.springframework.cache.annotation.Cacheable;

import static org.example.test.Main.session;

/**
 * Пример возможностей в Spring
 * @author Kuznetsovka created 06.10.2022
 */

public class Service {

  //@Cacheable(value = "mentors",  condition = "#mentor.getId()< 100")) Возможности Кэша в Spring.
  public Mentor getMentor(){
    return session.createQuery("select e from mentors e", Mentor.class)
        .setHint( "org.hibernate.cacheable",true) // даже добавив это будет 2 запроса
        .getResultStream()
        .findFirst()
        .orElse(new Mentor());
  }

}
