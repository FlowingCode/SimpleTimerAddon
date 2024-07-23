package com.flowingcode.addons.simpletimer.integration;

public interface IntegrationCallables {

  void setStartTime(Integer startTime);

  void setEndTime(Integer endTime);

  void start();

  void pause();

  void reset();

  boolean isRunning();

  void openDialog();

  void closeDialog();
  // BigDecimal getCurrentTime();
  //
  // CompletableFuture<BigDecimal> getCurrentTimeAsync();
  //
  // Registration addCurrentTimeChangeListener(PropertyChangeListener listener, long period,
  // TimeUnit periodUnit);
  //
  // Registration addTimerEndEvent(ComponentEventListener<TimerEndedEvent> listener);

}
