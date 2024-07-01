/*-
 * #%L
 * Simple Timer Addon
 * %%
 * Copyright (C) 2019 - 2020 Flowing Code
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
import java.math.BigDecimal;

@SuppressWarnings("serial")
@PageTitle("Simple Timer Demo")
@DemoSource
@Route(value = "simple-timer/simple-timer", layout = SimpletimerDemoView.class)
public class SimpletimerDemo extends Div {

  public SimpletimerDemo() {
    setSizeFull();
    final SimpleTimer timer = new SimpleTimer();
    timer.setWidth("100px");
    timer.setHeight("50px");
    timer.getStyle().set("font-size", "40px");

    Span timerTitle = new Span("Simple Count Up Timer");

    final TextField startTime =
        new TextField("Start Time", e -> {
          timer.setStartTime(new BigDecimal(e.getValue()));
          timer.setEndTime(new BigDecimal(e.getValue()));
        });
    final Checkbox countUp = new Checkbox("Count Up", false);
    countUp.addValueChangeListener(
        e -> {
          timer.setCountUp(countUp.getValue());
          if (e.getValue()) {
            startTime.setLabel("End Time");
            timerTitle.setText("Simple Count Up Timer");
          } else {
            startTime.setLabel("Start Time");
            timerTitle.setText("Simple Countdown Timer");
          }
        });
    final Button start = new Button("Start/Stop", e -> timer.start());
    final Button stop = new Button("Stop", e -> timer.pause());
    final Button reset =
        new Button(
            "Reset",
            e -> {
              timer.reset();
            });
    final Button running =
        new Button(
            "Current Time",
            e ->
                timer
                    .getCurrentTimeAsync()
                    .thenAccept(
                        time ->
                            Notification.show(
                                time.toPlainString()
                                    + (timer.isRunning() ? "" : " (Not Running)"))));
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

    timer.addTimerEndEvent(e -> Notification.show("Timer Ended"));

    final HorizontalLayout topLayout = new HorizontalLayout(timerTitle, timer);
    topLayout.setAlignItems(Alignment.CENTER);

    HorizontalLayout options =
        new HorizontalLayout(countUp, fractions, minutes, hours, visible, doubleDigitHours);
    options.setAlignItems(Alignment.CENTER);
    options.getStyle().set("flex-wrap", "wrap");

    final HorizontalLayout bottomLayout = new HorizontalLayout(start, stop, reset, running);
    bottomLayout.setAlignItems(Alignment.BASELINE);

    add(new VerticalLayout(topLayout, startTime, options, bottomLayout));
  }
}
