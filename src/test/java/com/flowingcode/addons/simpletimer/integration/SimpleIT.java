package com.flowingcode.addons.simpletimer.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.flowingcode.vaadin.testbench.rpc.HasRpcSupport;
import org.junit.Test;

public class SimpleIT extends AbstractViewTest implements HasRpcSupport {

  IntegrationCallables $server = createCallableProxy(IntegrationCallables.class);

  public SimpleIT() {
    super("it");
  }

  private Double currentTime() {
    return $(SimpleTimerElement.class).first().currentTime();
  }

  @Test
  public void countDown() {
    assertThat(currentTime(), nullValue());
    assertFalse($server.isRunning());

    $server.setStartTime(1);
    assertThat(currentTime(), equalTo(1.0));

    $server.start();
    assertTrue($server.isRunning());
    double t0 = currentTime();
    double t1 = currentTime();
    assertThat(t0, lessThan(1.0));
    assertThat(t1, lessThan(t0));
  }

  @Test
  public void countUp() {
    assertThat(currentTime(), nullValue());
    assertFalse($server.isRunning());

    $server.setEndTime(1);
    assertThat(currentTime(), equalTo(0.0));

    $server.start();
    assertTrue($server.isRunning());
    double t0 = currentTime();
    double t1 = currentTime();
    assertThat(t0, greaterThan(0.0));
    assertThat(t1, greaterThan(t0));
  }

}
