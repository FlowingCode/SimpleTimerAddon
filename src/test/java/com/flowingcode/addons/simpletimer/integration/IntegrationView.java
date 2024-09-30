package com.flowingcode.addons.simpletimer.integration;

import com.flowingcode.vaadin.addons.simpletimer.SimpleTimer;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;

@Route("/it")
public class IntegrationView extends Div implements IntegrationCallables {

  private final SimpleTimer timer = new SimpleTimer();

  private Dialog dialog;

  private final Label timerTarget;

  public IntegrationView() {
    add(timer);
    timerTarget = new Label();
    timerTarget.setId("timerTarget");
    timer.setTargetId("timerTarget");
  }

  @Override
  @ClientCallable
  public void openDialog() {
    if (dialog == null) {
      dialog = new Dialog(timerTarget);
    }
    dialog.open();
  }

  @Override
  @ClientCallable
  public void closeDialog() {
    dialog.close();
  }

  @Override
  @ClientCallable
  public void setStartTime(final Integer startTime) {
    timer.setStartTime(startTime);
  }

  @Override
  @ClientCallable
  public void setEndTime(final Integer endTime) {
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
