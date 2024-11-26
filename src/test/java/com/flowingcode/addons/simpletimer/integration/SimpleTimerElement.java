package com.flowingcode.addons.simpletimer.integration;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;
import java.util.Optional;

@Element("simple-timer")
public class SimpleTimerElement extends TestBenchElement {

  Double currentTime() {
    Number time = (Number) executeScript("return arguments[0].currentTime", this);
    return Optional.ofNullable(time).map(Number::doubleValue).orElse(null);
  }

}
