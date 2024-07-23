package com.flowingcode.addons.simpletimer.integration;

import com.flowingcode.vaadin.addons.simpletimer.SimpleTimer;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route("/it")
public class IntegrationView extends Div implements IntegrationCallables {

  private SimpleTimer timer = new SimpleTimer();

  public IntegrationView() {
    add(timer);
  }

  @Override
  @ClientCallable
  public void setStartTime(Integer startTime) {
    timer.setStartTime(startTime);
  }

  @Override
  @ClientCallable
  public void setEndTime(Integer endTime) {
    timer.setEndTime(endTime);
  }

  @Override
  @ClientCallable
  public void start() {
    timer.start();
  }

  @Override
  @ClientCallable
  public void pause() {
    timer.pause();
  }

  @Override
  @ClientCallable
  public void reset() {
    timer.reset();
  }

  @Override
  @ClientCallable
  public boolean isRunning() {
    return timer.isRunning();
  }

}
