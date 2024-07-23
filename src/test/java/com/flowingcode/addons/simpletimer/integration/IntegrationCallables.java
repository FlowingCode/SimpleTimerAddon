package com.flowingcode.addons.simpletimer.integration;

public interface IntegrationCallables {

  void setStartTime(Integer startTime);

  void setEndTime(Integer endTime);

  void start();

  void pause();

  void reset();

  boolean isRunning();

}
