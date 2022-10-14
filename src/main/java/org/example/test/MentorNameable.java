package org.example.test;

import javax.persistence.Cacheable;
import javax.persistence.MappedSuperclass;

/**
 * @author Kuznetsovka created 12.10.2022
 */
public interface MentorNameable {
  String getName();
  void setName(String name);
}
