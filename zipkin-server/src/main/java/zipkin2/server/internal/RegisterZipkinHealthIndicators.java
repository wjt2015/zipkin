/*
 * Copyright 2015-2019 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package zipkin2.server.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicatorRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import zipkin2.Component;

/** Makes sure all zipkin components end up in the /health endpoint. */
// This is an application listener to ensure the graph is fully constructed before doing health
final class RegisterZipkinHealthIndicators implements ApplicationListener {
  @Override public void onApplicationEvent(ApplicationEvent event) {
    if (!(event instanceof ApplicationReadyEvent)) return;
    ConfigurableListableBeanFactory beanFactory =
      ((ApplicationReadyEvent) event).getApplicationContext().getBeanFactory();
    HealthIndicatorRegistry registry = beanFactory.getBean(HealthIndicatorRegistry.class);
    Map<String, HealthIndicator> indicators = new LinkedHashMap<>();
    for (Component component : beanFactory.getBeansOfType(Component.class).values()) {
      indicators.put(component.toString(), new ZipkinHealthIndicator(component));
    }
    HealthAggregator aggregator = beanFactory.getBean(HealthAggregator.class);

    registry.register("zipkin", new CompositeHealthIndicator(aggregator, indicators));
  }
}
