/**
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
 *
 * <p>This source code is licensed under the BSD-style license found in the LICENSE file in the root
 * directory of this source tree. An additional grant of patent rights can be found in the PATENTS
 * file in the same directory.
 */
package com.facebook.battery.metrics.composite;

import android.support.v4.util.SimpleArrayMap;
import com.facebook.battery.metrics.core.SystemMetrics;
import com.facebook.battery.metrics.core.SystemMetricsCollector;
import com.facebook.infer.annotation.ThreadSafe;

/**
 * Composite metrics collector allows batching and using several Metrics Collectors together, keyed
 * by the SystemMetrics they're attached to.
 *
 * <p>For example, {@code CompositeMetricsCollector collector = new CompositeMetricsCollector()
 * .addMetricsCollector(TimeMetrics.class, new TimeMetricsCollector())
 * .addMetricsCollector(CpuMetrics.class, new CpuMetricsCollector()); <p>CompositeMetrics snapshot =
 * collector.createMetrics(); collector.getSnapshot(snapshot); <p>long uptimeMs =
 * snapshot.getMetric(TimeMetrics.class).uptimeMs; // etc. }
 */
@ThreadSafe
public class CompositeMetricsCollector extends SystemMetricsCollector<CompositeMetrics> {

  /**
   * This is treated as an effectively immutable field and shouldn't be modified beyond the
   * constructor for thread safety.
   */
  private final SimpleArrayMap<Class<? extends SystemMetrics>, SystemMetricsCollector<?>>
      mMetricsCollectorMap = new SimpleArrayMap<>();

  public static class Builder {
    private final SimpleArrayMap<Class<? extends SystemMetrics>, SystemMetricsCollector<?>>
        mMetricsCollectorMap = new SimpleArrayMap<>();

    /**
     * Add a metric and its collector to the CompositeMetricsCollector.
     *
     * @param metricsClass Class of the metric
     * @param collector Collector that returns a type of metricsClass from getSnapshot
     * @return Instance of this CompositeMetricsCollector
     */
    public <T extends SystemMetrics<T>> CompositeMetricsCollector.Builder addMetricsCollector(
        Class<T> metricsClass, SystemMetricsCollector<T> collector) {
      mMetricsCollectorMap.put(metricsClass, collector);
      return this;
    }

    public CompositeMetricsCollector build() {
      return new CompositeMetricsCollector(this);
    }
  }

  protected CompositeMetricsCollector(Builder builder) {
    mMetricsCollectorMap.putAll(builder.mMetricsCollectorMap);
  }

  /**
   * Get the metrics collector associated with a metrics type
   *
   * @param metricsClass Type of metric
   * @return Instance of metric collector if metrics class present in map, else null
   */
  public <S extends SystemMetrics<S>, T extends SystemMetricsCollector<S>> T getMetricsCollector(
      Class<S> metricsClass) {
    return (T) mMetricsCollectorMap.get(metricsClass);
  }

  /**
   * Gets the snapshot for all the metrics and returns a CompositeMetrics object with the value
   *
   * @param snapshot snapshot to reuse
   * @return CompositeMetrics object containing all the snapshot values
   */
  @Override
  @ThreadSafe(enableChecks = false)
  public boolean getSnapshot(CompositeMetrics snapshot) {
    boolean result = true;
    for (int i = 0; i < mMetricsCollectorMap.size(); i++) {
      Class<? extends SystemMetrics> metricsClass = mMetricsCollectorMap.keyAt(i);
      SystemMetricsCollector collector = mMetricsCollectorMap.get(metricsClass);
      SystemMetrics metric = snapshot.getMetric(metricsClass);
      result &= collector.getSnapshot(metric);
    }
    return result;
  }

  @Override
  public CompositeMetrics createMetrics() {
    CompositeMetrics metrics = new CompositeMetrics();
    for (int i = 0, size = mMetricsCollectorMap.size(); i < size; i++) {
      Class metricsClass = mMetricsCollectorMap.keyAt(i);
      metrics.putMetric(metricsClass, mMetricsCollectorMap.valueAt(i).createMetrics());
    }
    return metrics;
  }
}
