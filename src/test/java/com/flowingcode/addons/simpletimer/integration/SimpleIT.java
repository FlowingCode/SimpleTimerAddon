package com.flowingcode.addons.simpletimer.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.flowingcode.vaadin.testbench.rpc.HasRpcSupport;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;

public class SimpleIT extends AbstractViewTest implements HasRpcSupport {

  IntegrationCallables $server = createCallableProxy(IntegrationCallables.class);

  public SimpleIT() {
    super("it");
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
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

  @Test
  @Ignore
  public void countUpInDialog() {
    $server.openDialog();
    $server.setEndTime(100);
    assertThat(currentTime(), equalTo(0.0));

    long w0 = System.nanoTime();
    $server.start();
    double t0 = currentTime();
    assertThat(t0, greaterThan(0.0));
    long w1 = System.nanoTime();

    $server.closeDialog();
    sleep(0);
    $server.openDialog();

    long w2 = System.nanoTime();
    // double delta = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - now) / 1000.0;
    double t1 = currentTime();
    long w3 = System.nanoTime();
    // assertThat(t1, greaterThan(t0 + delta));
    System.out.println(TimeUnit.NANOSECONDS.toMillis(w3 - w0) / 1000.0);
    System.out.println(TimeUnit.NANOSECONDS.toMillis(w2 - w1) / 1000.0);
    System.out.println(t1 - t0);
    System.out.println(t0);
    System.out.println(t1);
  }

}
