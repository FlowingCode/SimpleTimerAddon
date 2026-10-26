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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.dom.PropertyChangeListener;
import com.vaadin.flow.shared.Registration;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A timer that counts down to zero or counts up to a limit.
 *
 * <p>The state of the timer is kept on the server, as the time banked from previous runs plus the
 * instant the current run started. The browser only renders it. When the component is detached and
 * attached again, the elapsed time is pushed to the new client element, so the timer keeps
 * counting while it is not in the DOM.
 *
 * @author Leonardo Scardanzan / Flowing Code
 */
@Tag("simple-timer")
@JsModule("./simple-timer/simple-timer.js")
public class SimpleTimer extends Component implements HasSize, HasStyle, Serializable {

  private static final long serialVersionUID = 2L;
  private static final String DISPLAY = "display";
  private static final String INLINE = "inline";
  private static final String NONE = "none";
  private static final String CURRENT_TIME = "currentTime";

  /**
   * How much earlier than the server computes it a client may report the end. The client renders
   * slightly behind the server, so an end reported much earlier belongs to a previous run.
   */
  private static final Duration END_TOLERANCE = Duration.ofSeconds(1);

  private Duration limit = Duration.ZERO;
  private boolean countUp;
  private Duration baseElapsed = Duration.ZERO;
  private Instant startedAt;
  private boolean endNotified;
  private Clock clock = Clock.systemUTC();

  /** Creates a timer. */
  public SimpleTimer() {
    getElement().getStyle().set(DISPLAY, INLINE);
    getElement().addEventListener("simple-timer-end", ev -> onClientEnd());
    getElement().addEventListener("simple-timer-sync-request", ev -> sync(false));
  }

  /**
   * Sets the clock used to measure elapsed time. Intended for tests.
   *
   * @param clock the clock to use
   */
  void setClock(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
  }

  /**
   * Configures the timer to count down from the given duration to zero, and resets it. A running
   * timer keeps running from the new start.
   *
   * @param duration the duration to count down from
   * @throws IllegalArgumentException if {@code duration} is negative
   */
  public void setCountdown(Duration duration) {
    setLimit(duration, false);
  }

  /**
   * Configures the timer to count up from zero to the given limit, and resets it. A running timer
   * keeps running from zero.
   *
   * @param limit the duration at which the timer stops
   * @throws IllegalArgumentException if {@code limit} is negative
   */
  public void setCountUp(Duration limit) {
    setLimit(limit, true);
  }

  private void setLimit(Duration limit, boolean countUp) {
    Objects.requireNonNull(limit, "limit");
    if (limit.isNegative()) {
      throw new IllegalArgumentException("limit must not be negative");
    }
    boolean running = isRunning();
    this.limit = limit;
    this.countUp = countUp;
    restart(Duration.ZERO, running);
  }

  /**
   * Returns whether the timer counts up.
   *
   * @return {@code true} if the timer counts up to its limit, {@code false} if it counts down
   */
  public boolean isCountUp() {
    return countUp;
  }

  /**
   * Returns the duration the timer counts down from, or counts up to.
   *
   * @return the limit of the timer
   */
  public Duration getLimit() {
    return limit;
  }

  /** Starts the timer. Does nothing if it is already running or has reached its limit. */
  public void start() {
    if (startedAt != null || rawElapsed().compareTo(limit) >= 0) {
      return;
    }
    startedAt = clock.instant();
    endNotified = false;
    sync(true);
  }

  /** Pauses the timer, keeping the elapsed time. Does nothing if it is not running. */
  public void pause() {
    if (startedAt == null) {
      return;
    }
    baseElapsed = getElapsed();
    startedAt = null;
    sync(false);
  }

  /** Resets the elapsed time to zero. A running timer keeps running from the start. */
  public void reset() {
    setElapsed(Duration.ZERO);
  }

  /**
   * Returns whether the timer is running.
   *
   * @return {@code true} if the timer has been started and has not reached its limit
   */
  public boolean isRunning() {
    return startedAt != null && rawElapsed().compareTo(limit) < 0;
  }

  /**
   * Returns the time the timer has been running, up to its limit. The value is computed on the
   * server and does not need a round trip.
   *
   * @return the elapsed time
   */
  public Duration getElapsed() {
    Duration elapsed = rawElapsed();
    return elapsed.compareTo(limit) > 0 ? limit : elapsed;
  }

  /**
   * Returns the time left until the timer reaches its limit.
   *
   * @return the remaining time
   */
  public Duration getRemaining() {
    return limit.minus(getElapsed());
  }

  /**
   * Sets the elapsed time, for instance to restore a timer whose state was saved elsewhere. A
   * running timer keeps running from the given point.
   *
   * @param elapsed the elapsed time, clamped between zero and the limit
   */
  public void setElapsed(Duration elapsed) {
    Objects.requireNonNull(elapsed, "elapsed");
    restart(elapsed, isRunning());
  }

  private void restart(Duration elapsed, boolean running) {
    baseElapsed = elapsed.isNegative() ? Duration.ZERO : elapsed;
    baseElapsed = baseElapsed.compareTo(limit) > 0 ? limit : baseElapsed;
    startedAt = running && baseElapsed.compareTo(limit) < 0 ? clock.instant() : null;
    endNotified = false;
    sync(running);
  }

  private Duration rawElapsed() {
    if (startedAt == null) {
      return baseElapsed;
    }
    Duration run = Duration.between(startedAt, clock.instant());
    // a backwards step of the server clock must not reduce the banked time
    return run.isNegative() ? baseElapsed : baseElapsed.plus(run);
  }

  /**
   * Enables showing fractions of a second.
   *
   * @param fractions whether to show hundredths of a second
   */
  public void setFractions(final boolean fractions) {
    getElement().setProperty("fractions", fractions);
  }

  /**
   * Enables showing minutes.
   *
   * @param minutes whether to show minutes
   */
  public void setMinutes(final boolean minutes) {
    getElement().setProperty("minutes", minutes);
  }

  /**
   * Enables showing hours and minutes.
   *
   * @param hours whether to show hours and minutes
   */
  public void setHours(final boolean hours) {
    getElement().setProperty("hours", hours);
  }

  /**
   * Use two digits for hours.
   *
   * @param doubleDigitHours true to format hours using two digits.
   */
  public void setDoubleDigitHours(final boolean doubleDigitHours) {
    getElement().setProperty("doubleDigitHours", doubleDigitHours);
  }

  /**
   * Adds a listener that is notified when the timer reaches its limit.
   *
   * <p>If the timer reaches its limit while it is detached, the listener is notified when the
   * component is attached again, and {@link TimerEndedEvent#isRetroactive()} returns {@code true}.
   *
   * @param listener the listener to add
   * @return a handle that can be used for removing the listener
   */
  public Registration addTimerEndListener(ComponentEventListener<TimerEndedEvent> listener) {
    return addListener(TimerEndedEvent.class, listener);
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    if (startedAt != null && rawElapsed().compareTo(limit) >= 0) {
      end(true);
    }
    sync(true);
  }

  private void onClientEnd() {
    if (startedAt == null || rawElapsed().plus(END_TOLERANCE).compareTo(limit) < 0) {
      return;
    }
    end(false);
  }

  private void end(boolean retroactive) {
    baseElapsed = limit;
    startedAt = null;
    if (endNotified) {
      return;
    }
    endNotified = true;
    fireEvent(new TimerEndedEvent(this, false, retroactive));
  }

  private void sync(boolean pull) {
    if (!isAttached()) {
      // pushed in onAttach
      return;
    }
    getElement()
        .callJsFunction(
            "_sync",
            (double) getElapsed().toMillis(),
            isRunning(),
            (double) limit.toMillis(),
            countUp,
            pull);
  }

  @Override
  public boolean isVisible() {
    return !NONE.equals(getStyle().get(DISPLAY));
  }

  /**
   * Shows or hides the timer with CSS, so that it keeps communicating with the server while hidden.
   *
   * @param visible whether to show the timer
   */
  @Override
  public void setVisible(boolean visible) {
    getStyle().set(DISPLAY, visible ? INLINE : NONE);
  }

  /** Event fired when the timer reaches its limit. */
  public static class TimerEndedEvent extends ComponentEvent<SimpleTimer> {

    private final boolean retroactive;

    /**
     * Creates a new event.
     *
     * @param source the timer that reached its limit
     * @param fromClient whether the event originated from the client
     */
    public TimerEndedEvent(final SimpleTimer source, final boolean fromClient) {
      this(source, fromClient, false);
    }

    /**
     * Creates a new event.
     *
     * @param source the timer that reached its limit
     * @param fromClient whether the event originated from the client
     * @param retroactive whether the timer ended while it was detached
     */
    public TimerEndedEvent(
        final SimpleTimer source, final boolean fromClient, final boolean retroactive) {
      super(source, fromClient);
      this.retroactive = retroactive;
    }

    /**
     * Returns whether the timer reached its limit while it was detached, in which case the event is
     * fired late, when the component is attached again.
     *
     * @return {@code true} if the event is fired after the fact
     */
    public boolean isRetroactive() {
      return retroactive;
    }
  }

  // ---- deprecated API ----

  /**
   * Sets the start time, for countdown mode.
   *
   * @param startTime value in seconds for the start time
   * @deprecated Use {@link #setCountdown(Duration)} instead.
   */
  @Deprecated
  public void setStartTime(final Number startTime) {
    setCountdown(seconds(startTime));
  }

  /**
   * Sets the end time, for countup mode.
   *
   * @param endTime value in seconds for the end time
   * @deprecated Use {@link #setCountUp(Duration)} instead.
   */
  @Deprecated
  public void setEndTime(final Number endTime) {
    setCountUp(seconds(endTime));
  }

  private static Duration seconds(Number seconds) {
    return Duration.ofNanos(Math.round(seconds.doubleValue() * 1_000_000_000d));
  }

  /**
   * Returns the displayed value of the timer: the remaining time when counting down, or the elapsed
   * time when counting up.
   *
   * @return current value in seconds
   * @deprecated Use {@link #getElapsed()} or {@link #getRemaining()} instead.
   */
  @Deprecated
  public BigDecimal getCurrentTime() {
    Duration displayed = countUp ? getElapsed() : getRemaining();
    return BigDecimal.valueOf(displayed.toMillis(), 3);
  }

  /**
   * Returns the displayed value of the timer. The value is computed on the server, so the returned
   * future is already completed.
   *
   * @return a completed future with the value of {@link #getCurrentTime()}
   * @deprecated Use {@link #getElapsed()} or {@link #getRemaining()} instead.
   */
  @Deprecated
  public CompletableFuture<BigDecimal> getCurrentTimeAsync() {
    return CompletableFuture.completedFuture(getCurrentTime());
  }

  /**
   * Adds a property change listener for the {@code currentTime} property.
   *
   * @param listener the property change listener
   * @param period the minimum period between listener invocations, or 0 to disable throttling
   * @param periodUnit time duration of throttling period
   * @return this registration, for chaining
   * @deprecated The value is now available on the server through {@link #getElapsed()}.
   */
  @Deprecated
  public Registration addCurrentTimeChangeListener(
      PropertyChangeListener listener, final long period, final TimeUnit periodUnit) {
    final int millis = (int) Math.min(periodUnit.toMillis(period), Integer.MAX_VALUE);
    if (listener == null) {
      listener = ev -> {};
    }
    return getElement()
        .addPropertyChangeListener(CURRENT_TIME, "current-time-changed", listener)
        .throttle(millis);
  }

  /**
   * Adds a timer ended listener that will be triggered when the timer reaches its limit.
   *
   * @param listener the listener to add
   * @return a handle that can be used for removing the listener
   * @deprecated Use {@link #addTimerEndListener(ComponentEventListener)} instead.
   */
  @Deprecated
  public Registration addTimerEndEvent(final ComponentEventListener<TimerEndedEvent> listener) {
    return addTimerEndListener(listener);
  }
}
