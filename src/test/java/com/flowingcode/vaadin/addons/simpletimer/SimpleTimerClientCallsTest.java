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

import com.flowingcode.vaadin.addons.simpletimer.SimpleTimer.DetachPolicy;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

/** Checks the JavaScript calls the timer queues for the browser. */
public class SimpleTimerClientCallsTest {

  private UI ui;
  private SimpleTimer timer;

  /**
   * Queuing a JavaScript call requires a session holding the lock, and since Vaadin 25 the session
   * needs a service with a deployment configuration. The configuration is a proxy, so that it
   * implements whatever methods the interface has in each supported version: it returns the default
   * value passed to the method if there is one, and a neutral value otherwise.
   */
  @Before
  public void setUp() {
    DeploymentConfiguration configuration =
        (DeploymentConfiguration)
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {DeploymentConfiguration.class},
                (proxy, method, args) -> defaultValue(method, args));
    VaadinSession session =
        new VaadinSession(new VaadinServletService(null, configuration)) {
          @Override
          public boolean hasLock() {
            return true;
          }
        };
    ui = new UI();
    ui.getInternals().setSession(session);
    timer = new SimpleTimer();
  }

  private static Object defaultValue(Method method, Object[] args) {
    Class<?> type = method.getReturnType();
    if (args != null && args.length > 0) {
      Class<?> last = method.getParameterTypes()[args.length - 1];
      if (type.isAssignableFrom(last) || (type.isPrimitive() && last == type)) {
        return args[args.length - 1];
      }
    }
    if (type == boolean.class) {
      return false;
    }
    if (type == int.class || type == long.class) {
      return type == int.class ? (Object) 0 : (Object) 0L;
    }
    return null;
  }

  private List<PendingJavaScriptInvocation> flush() {
    ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
    return ui.getInternals().dumpPendingJavaScriptInvocations();
  }

  private static long count(List<PendingJavaScriptInvocation> calls, String function) {
    return calls.stream()
        .filter(call -> call.getInvocation().getExpression().contains(function))
        .count();
  }

  @Test
  public void removingDisplayWhileTimerIsDetachedClearsIt() {
    Span display = new Span();
    ui.add(display);
    Registration registration = timer.addDisplay(display);
    flush();

    registration.remove();

    assertEquals(1, count(flush(), "_unbindDisplay"));
  }

  @Test
  public void resumingOnAttachSyncsOnce() {
    timer.setDetachPolicy(DetachPolicy.PAUSE);
    timer.setCountdown(Duration.ofSeconds(60));
    ui.add(timer);
    timer.start();
    flush();

    ui.remove(timer);
    ui.add(timer);

    assertEquals(1, count(flush(), "_sync"));
  }
}
