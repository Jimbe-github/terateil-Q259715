package com.teratail.q259715;

import android.os.Handler;

import java.util.*;

interface WorkoutProperties {
  int getSet();
  int getCount();
  int getTick();
  int getTickMax();
  int getLeftSecond();
  Workout.Status getStatus();
}

class Workout implements WorkoutProperties {
  interface Listener {
    void start(WorkoutProperties properties);
    void progress(WorkoutProperties properties);
    void finish(WorkoutProperties properties);
    void stop(WorkoutProperties properties);
  }
  enum Status {
    Nonexecution(""), Training("トレーニング"), RestCounts("休憩"), RestSets("休憩");

    private String text;
    Status(String text) {
      this.text = text;
    }
    String getText() { return text; };
  }

  private static final int PARTICLE_SIZE = 20; //1秒を何tickに分けるか

  private String name;
  private int time; //トレーニング時間[s]
  private int rest_counts; //休憩時間[s]
  private int countMax; //カウント
  private int setMax; //セット数
  private int rest_sets; //セット間の休憩時間[s]

  private Listener listener;
  private Timer timer;
  private int tick, tickMax;
  private int count, set;
  private Status status;
  private Handler handler;

  /**
   *
   * @param name トレーニング名?
   * @param time トレーニング時間[s]
   * @param rest_counts 休憩時間[s]
   * @param count カウント
   * @param set セット数
   * @param rest_sets セット間の休憩時間[s]
   */
  Workout(String name, int time, int rest_counts, int count, int set, int rest_sets) {
    this.name = name;
    this.time = time;
    this.rest_counts = rest_counts;
    this.countMax = count;
    this.setMax = set;
    this.rest_sets = rest_sets;

    this.handler = new Handler();

    reset();
  }

  public int getSet() { return set; }
  public int getCount() { return count; }
  public int getTick() { return tick; }
  public int getTickMax() { return tickMax; }
  public int getLeftSecond() { return calcLeftSecond(tick); }
  public Status getStatus() { return status; }

  private static int calcLeftSecond(int tick) { return (tick + PARTICLE_SIZE - 1) / PARTICLE_SIZE; }

  synchronized void start(Listener listener) {
    if(timer != null) throw new IllegalStateException();
    this.listener = listener;
    status = Status.Training;
    listener.start(Workout.this);
    timer = new Timer();
    timer.scheduleAtFixedRate(new TickTask(), 1000L / PARTICLE_SIZE, 1000L / PARTICLE_SIZE); //[ms]
  }

  private class TickTask extends TimerTask {
    @Override
    public void run() {
      boolean finish = downTick();
      final WorkoutProperties properties = new WorkoutSnapshot(Workout.this);
      handler.post(() -> postProgress(properties));
      if(finish) {
        synchronized(this) {
          timer.cancel();
          timer = null;
        }
        handler.post(() -> postFinish(properties));
      }
    }
  }

  private boolean downTick() {
    if(--tick > 0) return false;

    switch(status) {
      case RestCounts:
        if(count <= 0) {
          status = Status.RestSets;
          tickMax = rest_sets * PARTICLE_SIZE;
          tick = tickMax;
          break;
        }
        /*break; 引き続き RestSets の処理をする*/
      case RestSets:
        status = Status.Training;
        if(count <= 0) count = countMax;
        tickMax = time * PARTICLE_SIZE;
        tick = tickMax;
        break;
      default: //Training
        if(--count <= 0) {
          if(--set <= 0) {
            status = Status.Nonexecution;
            return true; //finish
          }
        }
        status = Status.RestCounts;
        tickMax = rest_counts * PARTICLE_SIZE;
        tick = tickMax;
    }
    return false;
  }

  private static class WorkoutSnapshot implements WorkoutProperties {
    private int tick, tickMax;
    private int count, set;
    private Status status;

    WorkoutSnapshot(Workout wo) {
      this.tick = wo.tick;
      this.tickMax = wo.tickMax;
      this.count = wo.count;
      this.set = wo.set;
      this.status = wo.status;
    }

    public int getSet() { return set; }
    public int getCount() { return count; }
    public int getTick() { return tick; }
    public int getTickMax() { return tickMax; }
    public int getLeftSecond() { return calcLeftSecond(tick); }
    public Status getStatus() { return status; }
  }

  private synchronized void postProgress(WorkoutProperties properties) {
    if(listener != null) {
      listener.progress(properties);
    }
  }

  private synchronized void postFinish(WorkoutProperties properties) {
    if(listener != null) {
      listener.finish(properties);
      listener = null;
    }
  }

  synchronized void stop() {
    if(timer != null) {
      timer.cancel();
      timer = null;
      if(listener != null) {
        listener.stop(this);
        listener = null;
      }
    }
  }

  synchronized void reset() {
    if(timer == null) {
      count = countMax;
      set = setMax;
      tickMax = time * PARTICLE_SIZE;
      tick = tickMax;
      status = Status.Nonexecution;
    }
  }
}