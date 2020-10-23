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

import com.flowingcode.vaadin.addons.DemoLayout;
import com.flowingcode.vaadin.addons.demo.impl.TabbedDemoImpl;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@SuppressWarnings("serial")
@Route(value = "simple-timer", layout = DemoLayout.class)
public class SimpletimerDemoView extends VerticalLayout {

	private static final String ST_DEMO = "Simple Timer Demo";
	private static final String ST_SOURCE = "https://github.com/FlowingCode/SimpleTimerAddon/blob/master/src/test/java/com/flowingcode/vaadin/addons/simpletimer/SimpletimerDemo.java";

	public SimpletimerDemoView() {
		TabbedDemoImpl<SimpletimerDemo> stDemo = new TabbedDemoImpl<SimpletimerDemo>(new SimpletimerDemo(), ST_DEMO,
				ST_SOURCE);
		setSizeFull();
		add(stDemo);
	}
}