/*-
 * #%L
 * Simple Timer Addon
 * %%
 * Copyright (C) 2019 Flowing Code
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


import java.math.BigDecimal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;

/**
 * @author Leonardo Scardanzan / Flowing Code
 */
@Tag("simple-timer")
@JsModule("./simple-timer/simple-timer.js")
public class SimpleTimer extends Component implements HasSize, HasStyle {

	private static final long serialVersionUID = 1L;

	/**
     * Creates a timer with a start time of 60
     */
    public SimpleTimer() {
        getElement().getStyle().set("display", "inline");
    }

    /**
     * Creates a timer using the start time passed in the constructor
     *
     * @param startTime value in seconds for the start time
     */
    public SimpleTimer(final Number startTime) {
        this();
        setStartTime(startTime);
    }

    /**
     * Sets the start time
     *
     * @param startTime value in seconds for the start time
     */
    public void setStartTime(final Number startTime) {
        getElement().setProperty("startTime", startTime.doubleValue());
        reset();
    }

    /**
     * Changes the behavior to count up or down Default is false for count down
     *
     * @param countUp
     */
    public void setCountUp(final boolean countUp) {
        getElement().setProperty("countUp", countUp);
        reset();
    }

    /**
     * Starts or stops the timer if it is already started
     */
    public void start() {
        getElement().callFunction("start");
    }

    /**
     * Stops the timer, does nothing if already stopped
     */
    public void pause() {
        getElement().callFunction("pause");
    }

    /**
     * Resets the current value to the start time
     */
    public void reset() {
        getElement().callFunction("ready");
    }

    /**
     * Returns the status of the timer
     *
     * @return
     */
    @Synchronize(property = "isRunning", value = "is-running-changed")
    public boolean isRunning() {
        return getElement().getProperty("isRunning", false);
    }

    /**
     * Returns the current value of the timer
     *
     * @return current value in seconds
     */
    @Synchronize("current-time-changed")
    public BigDecimal getCurrentTime() {
        return BigDecimal.valueOf(getElement().getProperty("currentTime", 0d));
    }

    /**
     * Event that gets triggered when the timer reaches 0
     *
     */
    @DomEvent("simple-timer-end")
    public static class TimerEndedEvent extends ComponentEvent<SimpleTimer> {

        public TimerEndedEvent(final SimpleTimer source, final boolean fromClient) {
            super(source, fromClient);
        }

    }

    /**
     * Adds a timer ended listener that will be triggered when the timer reaches 0
     *
     * @param listener
     * @return
     */
    public Registration addTimerEndEvent(final ComponentEventListener<TimerEndedEvent> listener) {
        return addListener(TimerEndedEvent.class, listener);
    }
}
