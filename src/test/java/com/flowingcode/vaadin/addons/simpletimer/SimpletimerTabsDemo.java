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
import com.flowingcode.vaadin.addons.simpletimer.SimpleTimer.DetachPolicy;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.time.Duration;

@SuppressWarnings("serial")
@PageTitle("Tabs and External Displays")
@DemoSource
@Route(value = "simple-timer/tabs", layout = SimpletimerDemoView.class)
public class SimpletimerTabsDemo extends Div {

  public SimpletimerTabsDemo() {
    // A timer inside a tab is detached when another tab is selected, and keeps counting
    SimpleTimer countdown = new SimpleTimer();
    countdown.setCountdown(Duration.ofSeconds(30));
    countdown.getStyle().set("font-size", "40px");
    countdown.addTimerEndListener(
        ev ->
            Notification.show(
                ev.isRetroactive()
                    ? "The countdown ended while its tab was not selected"
                    : "The countdown ended"));

    Checkbox pauseWhileHidden =
        new Checkbox(
            "Pause while another tab is selected",
            ev ->
                countdown.setDetachPolicy(
                    ev.getValue() ? DetachPolicy.PAUSE : DetachPolicy.CONTINUE));

    VerticalLayout countdownPage =
        new VerticalLayout(
            countdown,
            new HorizontalLayout(
                new Button("Start", ev -> countdown.start()),
                new Button("Pause", ev -> countdown.pause()),
                new Button("Reset", ev -> countdown.reset())),
            pauseWhileHidden);

    // A hidden timer attached outside the tabs, shown in the header and inside a tab
    SimpleTimer clock = new SimpleTimer();
    clock.setCountUp(Duration.ofHours(1));
    clock.setMinutes(true);
    clock.setFractions(false);
    clock.setVisible(false);
    clock.start();

    Span headerDisplay = new Span();
    Span tabDisplay = new Span();
    tabDisplay.getStyle().set("font-size", "40px");
    clock.addDisplay(headerDisplay);
    clock.addDisplay(tabDisplay);

    VerticalLayout displayPage =
        new VerticalLayout(
            new Span("This tab only holds a display. The timer is attached outside the tabs."),
            tabDisplay,
            new HorizontalLayout(
                new Button("Start", ev -> clock.start()),
                new Button("Pause", ev -> clock.pause()),
                new Button("Reset", ev -> clock.reset())));

    Tab countdownTab = new Tab("Countdown");
    Tab displayTab = new Tab("External display");
    Tabs tabs = new Tabs(countdownTab, displayTab);
    Div content = new Div(countdownPage);
    tabs.addSelectedChangeListener(
        ev -> {
          content.removeAll();
          content.add(tabs.getSelectedTab() == countdownTab ? countdownPage : displayPage);
        });

    HorizontalLayout header = new HorizontalLayout(new Span("Time on this page:"), headerDisplay);
    header.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

    add(header, clock, tabs, content);
  }
}
