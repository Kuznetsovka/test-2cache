package org.example.test;

import javax.persistence.EntityManager;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import static org.example.test.Test2.*;
class MyThread implements Runnable {

  enum MethodName{
    CHANGE,
    NONE,
    DELETE
  }
  private CountDownLatch cdl;
  private String name;
  private MethodName methodName;
  private int time;
  private CyclicBarrier barrier;
  private Class<Mentor> mentor = null;
  private Class<MentorReadOnly> mentorReadOnly = null;
  private Class<MentorTransactional> mentorTransactional = null;
  private Class<MentorNonstrict> mentorNonstrict = null;
  private final EntityManager em = emf.createEntityManager();

  public MyThread(Builder builder) {
    if (builder == null) {
      throw new IllegalArgumentException("Please provide employee builder to build employee object.");
    }
    this.cdl = builder.cdl;
    this.name = builder.name;
    this.methodName = builder.methodName;
    this.time = builder.time;
    this.barrier = builder.barrier;
    this.mentor = builder.mentor;
    this.mentorReadOnly = builder.mentorReadOnly;
    this.mentorTransactional = builder.mentorTransactional;
    this.mentorNonstrict = builder.mentorNonstrict;
    new Thread(this);
  }

  public MyThread() {
    super();
    new Thread(this);
  }

  public void run() {
    sleep(time);
    System.out.println(name + " Начало транзакции");
    System.out.println(cache.toString());
    System.out.println(em.toString());
    em.getTransaction().begin();

    if (mentor != null)
        mentor();
    else if (mentorReadOnly != null)
        mentorReadOnly();
    else if (mentorNonstrict != null)
      mentorNonstrict();
    else if (mentorTransactional != null)
      mentorTransactional();

    System.out.println(name + " Количество запросов: " + statictics.getPrepareStatementCount());
    System.out.println(name + " Количество сущностей в кэше: " + statictics.getSecondLevelCachePutCount());
    System.out.println(name + " Количество сущностей взятых из кэша: " + statictics.getSecondLevelCacheHitCount());
    try {
      barrier.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (BrokenBarrierException e) {
      throw new RuntimeException(e);
    }
    em.getTransaction().commit();
    System.out.println(name + " Количество запросов: " + statictics.getPrepareStatementCount());
    System.out.println(name + " Количество сущностей в кэше: " + statictics.getSecondLevelCachePutCount());
    System.out.println(name + " Количество сущностей взятых из кэша: " + statictics.getSecondLevelCacheHitCount());
    System.out.println(name + " Конец транзакции");
    try {
      barrier.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (BrokenBarrierException e) {
      throw new RuntimeException(e);
    }
    cdl.countDown();
  }

  private void mentorReadOnly() {
    MentorReadOnly mentor1 = em.find(mentorReadOnly, 1L);
    System.out.println(name + " Старое имя объекта: " + mentor1.getName());

    method(mentor1);

    System.out.println(name + " Количество запросов: " + statictics.getPrepareStatementCount());
    System.out.println(name + " Количество сущностей в кэше: " + statictics.getSecondLevelCachePutCount());

    MentorReadOnly mentor2 = em.find(mentorReadOnly, 1L);
    System.out.println(name + " Новое имя объекта: " + (mentor2 != null ? mentor2.getName() : "NULL"));
  }

  private void mentorNonstrict() {
    MentorNonstrict mentor1 = em.find(mentorNonstrict, 1L);
    System.out.println(name + " Старое имя объекта: " + mentor1.getName());

    method(mentor1);

    System.out.println(name + " Количество запросов: " + statictics.getPrepareStatementCount());
    System.out.println(name + " Количество сущностей в кэше: " + statictics.getSecondLevelCachePutCount());

    MentorNonstrict mentor2 = em.find(mentorNonstrict, 1L);
    System.out.println(name + " Новое имя объекта: " + (mentor2 != null ? mentor2.getName() : "NULL"));
  }

  private void mentor() {
    Mentor mentor1 = em.find(mentor, 1L);
    System.out.println(name + " Старое имя объекта: " + mentor1.getName());

    method(mentor1);

    System.out.println(name + " Количество запросов: " + statictics.getPrepareStatementCount());
    System.out.println(name + " Количество сущностей в кэше: " + statictics.getSecondLevelCachePutCount());

    Mentor mentor2 = em.find(mentor, 1L);
    System.out.println(name + " Новое имя объекта: " + (mentor2 != null ? mentor2.getName() : "NULL"));
  }

  private void mentorTransactional() {
    MentorTransactional mentor1 = em.find(mentorTransactional, 1L);
    System.out.println(name + " Старое имя объекта: " + mentor1.getName());

    method(mentor1);

    System.out.println(name + " Количество запросов: " + statictics.getPrepareStatementCount());
    System.out.println(name + " Количество сущностей в кэше: " + statictics.getSecondLevelCachePutCount());

    MentorTransactional mentor2 = em.find(mentorTransactional, 1L);
    System.out.println(name + " Новое имя объекта: " + (mentor2 != null ? mentor2.getName() : "NULL"));
  }

  private void method(MentorNameable mentor) {
    switch (methodName){
      case CHANGE:
        mentor.setName("Ментор новый" + name);
        try {
          em.flush();
        } catch (UnsupportedOperationException e){
          e.printStackTrace();
        }
        break;
      case DELETE:
        em.remove(mentor);
        em.flush();
        break;
      case NONE:
        break;
    }
  }


  public CountDownLatch getCdl() {
    return cdl;
  }

  public String getName() {
    return name;
  }

  public MethodName getMethodName() {
    return methodName;
  }

  public int getTime() {
    return time;
  }

  public CyclicBarrier getBarrier() {
    return barrier;
  }

  public Class<Mentor> getMentor() {
    return mentor;
  }

  public Class<MentorReadOnly> getMentorReadOnly() {
    return mentorReadOnly;
  }

  public Class<MentorTransactional> getMentorTransactional() {
    return mentorTransactional;
  }

  public Class<MentorNonstrict> getMentorNonstrict() {
    return mentorNonstrict;
  }

  public static class Builder {
    protected CountDownLatch cdl;
    protected String name;
    protected MethodName methodName;
    protected int time;
    protected CyclicBarrier barrier;
    protected Class<Mentor> mentor = null;
    protected Class<MentorReadOnly> mentorReadOnly = null;
    protected Class<MentorTransactional> mentorTransactional = null;
    protected Class<MentorNonstrict> mentorNonstrict = null;

    public Builder() {
      super();
    }

    public Builder cdl(CountDownLatch cdl) {
      this.cdl = cdl;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder methodName(MethodName methodName) {
      this.methodName = methodName;
      return this;
    }

    public Builder time(int time) {
      this.time = time;
      return this;
    }

    public Builder barrier(CyclicBarrier barrier) {
      this.barrier = barrier;
      return this;
    }
    public Builder mentor(Class<Mentor> mentor) {
      this.mentor = mentor;
      return this;
    }

    public Builder mentorReadOnly(Class<MentorReadOnly> mentorReadOnly) {
      this.mentorReadOnly = mentorReadOnly;
      return this;
    }

    public Builder mentorTransactional(Class<MentorTransactional> mentorTransactional) {
      this.mentorTransactional = mentorTransactional;
      return this;
    }

    public Builder mentorNonstrict(Class<MentorNonstrict> mentorNonstrict) {
      this.mentorNonstrict = mentorNonstrict;
      return this;
    }

    public MyThread build() {
      return new MyThread(this);
    }
  }
}