package org.example.test;

import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import java.util.concurrent.*;

import static org.example.test.Test2.*;

class MyThread implements Runnable {



  enum MethodName {
    CHANGE,
    FIND,
    DELETE
  }

  enum TypeBarrier {
    EASY(0),
    TIMEOUT_1(1),
    TIMEOUT_3(3),
    TIMEOUT_5(5),
    TIMEOUT_10(10),
    TIMEOUT_20(20);

    private volatile int time;

    public int getTime() {
      return time;
    }

    TypeBarrier(int time) {
      this.time = time;
    }
  }

  private CountDownLatch cdl;
  private String name;
  private MethodName methodName;
  private int timeBeforeStart;
  private int timeBeforeCommit;
  private int timeBeforeMethod;
  private TypeBarrier typeBarrier;
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
    this.timeBeforeStart = builder.timeBeforeStart;
    this.timeBeforeCommit = builder.timeBeforeCommit;
    this.timeBeforeMethod = builder.timeBeforeMethod;
    this.barrier = builder.barrier;
    this.typeBarrier = builder.typeBarrier;
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
    sleep(timeBeforeStart);
    System.out.println(name + " ***** Начало транзакции *****");
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

    barrier(typeBarrier);

    try {
      sleep(timeBeforeCommit);
      em.getTransaction().commit();
      System.out.println(name + " ***** Конец транзакции *****");

      barrier(typeBarrier);

      printNameAfterTransaction();

      System.out.println(name + " Количество запросов: " + statictics.getPrepareStatementCount());
      System.out.println(name + " Количество сущностей в кэше: " + statictics.getSecondLevelCachePutCount());
      System.out.println(name + " Количество сущностей взятых из кэша: " + statictics.getSecondLevelCacheHitCount());

      barrier(typeBarrier);

    } catch (OptimisticLockException e) {
      e.printStackTrace();
    }
    cdl.countDown();
  }

  private void barrier(TypeBarrier typeBarrier) {
    if(typeBarrier.equals(TypeBarrier.EASY)) {
      try {
        barrier.await();
      } catch (InterruptedException | BrokenBarrierException e) {
        throw new RuntimeException(e);
      }
    } else {
      try {
        barrier.await(typeBarrier.getTime(), TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } catch (TimeoutException e) {
        System.out.println(name + " Конец таймаута");
      } catch (BrokenBarrierException ignored) {
      }
    }
  }

  private void printNameAfterTransaction() {
    if (mentor != null) {
      Mentor mentor = em.find(Mentor.class, 1L);
      System.out.println(name + " " + (mentor!= null ? mentor.getName() : "NULL"));
    } else if (mentorReadOnly != null) {
      MentorReadOnly mentor = em.find(MentorReadOnly.class, 1L);
      System.out.println(name + " " + (mentor!= null ? mentor.getName() : "NULL"));
    } else if (mentorNonstrict != null) {
      MentorNonstrict mentor = em.find(MentorNonstrict.class, 1L);
      System.out.println(name + " " + (mentor!= null ? mentor.getName() : "NULL"));
    } else if (mentorTransactional != null) {
      MentorTransactional mentor = em.find(MentorTransactional.class, 1L);
      System.out.println(name + " " + (mentor!= null ? mentor.getName() : "NULL"));
    }
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

  private void method(MentorNameable mentor) throws OptimisticLockException, UnsupportedOperationException {
    sleep(timeBeforeMethod);
    switch (methodName) {
      case CHANGE:
        mentor.setName("Ментор новый" + name);
        em.merge(mentor);
        try {
          em.flush();
          System.out.println(name + " CHANGE flush()");
        } catch (UnsupportedOperationException | OptimisticLockException e) {
          e.printStackTrace();
        }
        break;
      case DELETE:
        em.remove(mentor);
        try {
          em.flush();
        } catch (UnsupportedOperationException | OptimisticLockException e) {
          e.printStackTrace();
        }
        System.out.println(name + " DELETE flush()");
        break;
      case FIND:
        break;
    }
  }

  public static class Builder {
    protected CountDownLatch cdl;
    protected String name;
    protected MethodName methodName;
    protected int timeBeforeStart;
    protected int timeBeforeCommit;
    protected int timeBeforeMethod;
    protected CyclicBarrier barrier;
    protected TypeBarrier typeBarrier = TypeBarrier.EASY;
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

    public Builder timeBeforeStart(int timeBeforeStart) {
      this.timeBeforeStart = timeBeforeStart;
      return this;
    }

    public Builder timeBeforeCommit(int timeBeforeCommit) {
      this.timeBeforeCommit = timeBeforeCommit;
      return this;
    }
    public Builder timeBeforeMethod(int timeBeforeMethod) {
      this.timeBeforeMethod = timeBeforeMethod;
      return this;
    }

    public Builder barrier(CyclicBarrier barrier) {
      this.barrier = barrier;
      return this;
    }
    public Builder typeBarrier(TypeBarrier typeBarrier) {
      this.typeBarrier = typeBarrier;
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