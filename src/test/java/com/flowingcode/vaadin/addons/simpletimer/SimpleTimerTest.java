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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.flowingcode.vaadin.addons.simpletimer.SimpleTimer.DetachPolicy;
import com.flowingcode.vaadin.addons.simpletimer.SimpleTimer.TimerEndedEvent;
import com.vaadin.flow.component.ComponentUtil;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class SimpleTimerTest {

  private static class MutableClock extends Clock {

    private Instant now = Instant.parse("2026-01-01T00:00:00Z");

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private final MutableClock clock = new MutableClock();
  private final List<TimerEndedEvent> ended = new ArrayList<>();
  private SimpleTimer timer;

  @Before
  public void setUp() {
    timer = new SimpleTimer();
    timer.setClock(clock);
    timer.addTimerEndListener(ended::add);
  }

  private static Duration s(long seconds) {
    return Duration.ofSeconds(seconds);
  }

  @Test
  public void countsWhileRunning() {
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(10));

    assertTrue(timer.isRunning());
    assertEquals(s(10), timer.getElapsed());
    assertEquals(s(50), timer.getRemaining());
  }

  @Test
  public void pauseBanksElapsedTime() {
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(10));
    timer.pause();
    clock.advance(s(100));

    assertFalse(timer.isRunning());
    assertEquals(s(10), timer.getElapsed());

    timer.start();
    clock.advance(s(5));
    assertEquals(s(15), timer.getElapsed());
  }

  @Test
  public void stopsAtLimit() {
    timer.setCountUp(s(30));
    timer.start();
    clock.advance(s(100));

    assertFalse(timer.isRunning());
    assertEquals(s(30), timer.getElapsed());
    assertEquals(Duration.ZERO, timer.getRemaining());
  }

  @Test
  public void startDoesNotToggle() {
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(10));
    timer.start();
    clock.advance(s(10));

    assertTrue(timer.isRunning());
    assertEquals(s(20), timer.getElapsed());
  }

  @Test
  public void startDoesNothingWithoutLimit() {
    timer.start();
    assertFalse(timer.isRunning());
  }

  @Test
  public void resetKeepsRunningTimerRunning() {
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(10));
    timer.reset();
    clock.advance(s(3));

    assertTrue(timer.isRunning());
    assertEquals(s(3), timer.getElapsed());
  }

  @Test
  public void resetKeepsPausedTimerPaused() {
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(10));
    timer.pause();
    timer.reset();
    clock.advance(s(3));

    assertFalse(timer.isRunning());
    assertEquals(Duration.ZERO, timer.getElapsed());
  }

  @Test
  public void shorterLimitKeepsRunningTimerRunning() {
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(20));
    timer.setCountdown(s(10));
    clock.advance(s(4));

    assertTrue(timer.isRunning());
    assertEquals(s(4), timer.getElapsed());
    assertTrue(ended.isEmpty());
  }

  @Test
  public void longerLimitAfterLimitFiresEndOfPreviousRun() {
    timer.setCountdown(s(60));
    timer.start();
    ComponentUtil.onComponentDetach(timer);
    clock.advance(s(90));
    timer.setCountdown(s(120));

    assertEquals(1, ended.size());
    assertFalse(timer.isRunning());
    assertEquals(Duration.ZERO, timer.getElapsed());
  }

  @Test
  public void clockSteppingBackwardsKeepsBankedTime() {
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(10));
    timer.pause();
    timer.start();
    clock.advance(s(-30));

    assertEquals(s(10), timer.getElapsed());
  }

  @Test
  public void setElapsedIsClampedToLimit() {
    timer.setCountdown(s(60));
    timer.setElapsed(s(90));
    assertEquals(s(60), timer.getElapsed());

    timer.setElapsed(s(-5));
    assertEquals(Duration.ZERO, timer.getElapsed());
  }

  @Test
  public void endWhileDetachedFiresRetroactivelyOnAttach() {
    timer.setCountdown(s(60));
    timer.start();
    ComponentUtil.onComponentDetach(timer);
    clock.advance(s(90));
    assertTrue(ended.isEmpty());

    ComponentUtil.onComponentAttach(timer, false);

    assertEquals(1, ended.size());
    assertTrue(ended.get(0).isRetroactive());
    assertFalse(ended.get(0).isFromClient());
    assertFalse(timer.isRunning());
    assertEquals(s(60), timer.getElapsed());
  }

  @Test
  public void endFiresOnlyOnce() {
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(90));

    ComponentUtil.onComponentAttach(timer, false);
    ComponentUtil.onComponentAttach(timer, false);

    assertEquals(1, ended.size());
  }

  @Test
  public void pauseAfterLimitWhileDetachedFiresEnd() {
    timer.setCountdown(s(60));
    timer.start();
    ComponentUtil.onComponentDetach(timer);
    clock.advance(s(90));
    timer.pause();

    assertEquals(1, ended.size());
    assertTrue(ended.get(0).isRetroactive());
    assertEquals(s(60), timer.getElapsed());

    ComponentUtil.onComponentAttach(timer, false);
    assertEquals(1, ended.size());
  }

  @Test
  public void resetAfterLimitWhileDetachedFiresEndOnce() {
    timer.setCountdown(s(60));
    timer.start();
    ComponentUtil.onComponentDetach(timer);
    clock.advance(s(90));
    timer.reset();

    assertEquals(1, ended.size());
    assertFalse(timer.isRunning());
    assertEquals(Duration.ZERO, timer.getElapsed());

    ComponentUtil.onComponentAttach(timer, false);
    assertEquals(1, ended.size());
  }

  @Test
  public void attachBeforeLimitDoesNotEnd() {
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(30));

    ComponentUtil.onComponentAttach(timer, false);

    assertTrue(ended.isEmpty());
    assertTrue(timer.isRunning());
  }

  @Test
  public void continuePolicyCountsWhileDetached() {
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(10));
    ComponentUtil.onComponentDetach(timer);
    clock.advance(s(20));
    ComponentUtil.onComponentAttach(timer, false);

    assertTrue(timer.isRunning());
    assertEquals(s(30), timer.getElapsed());
  }

  @Test
  public void pausePolicyPausesWhileDetached() {
    timer.setDetachPolicy(DetachPolicy.PAUSE);
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(10));
    ComponentUtil.onComponentDetach(timer);
    clock.advance(s(100));

    assertFalse(timer.isRunning());
    assertEquals(s(10), timer.getElapsed());

    ComponentUtil.onComponentAttach(timer, false);
    clock.advance(s(5));

    assertTrue(timer.isRunning());
    assertEquals(s(15), timer.getElapsed());
  }

  @Test
  public void pausePolicyDoesNotResumeExplicitlyPausedTimer() {
    timer.setDetachPolicy(DetachPolicy.PAUSE);
    timer.setCountdown(s(60));
    timer.start();
    clock.advance(s(10));
    ComponentUtil.onComponentDetach(timer);
    timer.pause();
    ComponentUtil.onComponentAttach(timer, false);

    assertFalse(timer.isRunning());
    assertEquals(s(10), timer.getElapsed());
  }

  @Test
  public void pausePolicyDoesNotResumeTimerThatWasNotRunning() {
    timer.setDetachPolicy(DetachPolicy.PAUSE);
    timer.setCountdown(s(60));
    ComponentUtil.onComponentDetach(timer);
    ComponentUtil.onComponentAttach(timer, false);

    assertFalse(timer.isRunning());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void deprecatedCurrentTimeShowsDisplayedValue() {
    timer.setStartTime(60);
    timer.start();
    clock.advance(Duration.ofMillis(10_500));
    assertEquals(new BigDecimal("49.500"), timer.getCurrentTime());

    timer.setEndTime(new BigDecimal("30"));
    clock.advance(Duration.ofMillis(2_250));
    assertEquals(new BigDecimal("2.250"), timer.getCurrentTime());
  }
}
