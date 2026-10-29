/*-
 * #%L
 * Simple Timer Addon
 * %%
 * Copyright (C) 2019 - 2026 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.flowingcode.vaadin.addons.simpletimer;

import com.flowingcode.vaadin.addons.demo.DemoSource;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.Duration;
import java.util.Locale;

@SuppressWarnings("serial")
@PageTitle("Simple Timer Demo")
@DemoSource
@Route(value = "simple-timer/simple-timer", layout = SimpletimerDemoView.class)
public class SimpletimerDemo extends Div {

  private final SimpleTimer timer = new SimpleTimer();

  private boolean countUpMode;
  private Duration time = Duration.ofSeconds(60);

  public SimpletimerDemo() {
    setSizeFull();
    timer.setWidth("100px");
    timer.setHeight("50px");
    timer.getStyle().set("font-size", "40px");
    timer.setCountdown(time);

    final Span timerTitle = new Span("Simple Countdown Timer");

    final TextField startTime =
        new TextField(
            "Start Time",
            e -> {
              try {
                time = Duration.ofMillis(Math.round(Double.parseDouble(e.getValue()) * 1000));
                update();
              } catch (final Exception ex) {
                Notification.show("Please enter a positive number");
              }
            });
    final Checkbox countUp = new Checkbox("Count Up", false);
    countUp.addValueChangeListener(
        e -> {
          countUpMode = countUp.getValue();
          if (countUpMode) {
            startTime.setLabel("End Time");
            timerTitle.setText("Simple Count Up Timer");
          } else {
            startTime.setLabel("Start Time");
            timerTitle.setText("Simple Countdown Timer");
          }
          update();
        });
    final Button start = new Button("Start", e -> timer.start());
    final Button pause = new Button("Pause", e -> timer.pause());
    final Button reset = new Button("Reset", e -> timer.reset());
    final Button current =
        new Button(
            "Current Time",
            e ->
                Notification.show(
                    seconds(countUpMode ? timer.getElapsed() : timer.getRemaining())
                        + (timer.isRunning() ? "" : " (Not Running)")));
    final Checkbox fractions = new Checkbox("Fractions", true);
    fractions.addValueChangeListener(e -> timer.setFractions(e.getValue()));
    final Checkbox minutes = new Checkbox("Minutes", e -> timer.setMinutes(e.getValue()));
    final Checkbox hours = new Checkbox("Hours", e -> timer.setHours(e.getValue()));
    final Checkbox doubleDigitHours =
        new Checkbox("Double digit hours", e -> timer.setDoubleDigitHours(e.getValue()));
    final Checkbox visible =
        new Checkbox(
            "Visible",
            e -> {
              if (e.isFromClient()) {
                timer.setVisible(!timer.isVisible());
              }
            });
    visible.setValue(true);

    final Span serverValue = new Span();
    timer.addTickListener(
        e ->
            serverValue.setText(
                "Elapsed on the server, once per second: " + seconds(e.getElapsed())),
        Duration.ofSeconds(1));

    timer.addTimerEndListener(e -> Notification.show("Timer Ended"));

    final HorizontalLayout topLayout = new HorizontalLayout(timerTitle, timer);
    topLayout.setAlignItems(Alignment.CENTER);

    final HorizontalLayout options =
        new HorizontalLayout(countUp, fractions, minutes, hours, visible, doubleDigitHours);
    options.setAlignItems(Alignment.CENTER);
    options.getStyle().set("flex-wrap", "wrap");

    final HorizontalLayout bottomLayout = new HorizontalLayout(start, pause, reset, current);
    bottomLayout.setAlignItems(Alignment.BASELINE);

    add(new VerticalLayout(topLayout, startTime, options, bottomLayout, serverValue));
  }

  private static String seconds(Duration duration) {
    return String.format(Locale.ROOT, "%.2f s", duration.toMillis() / 1000d);
  }

  private void update() {
    if (countUpMode) {
      timer.setCountUp(time);
    } else {
      timer.setCountdown(time);
    }
  }
}
