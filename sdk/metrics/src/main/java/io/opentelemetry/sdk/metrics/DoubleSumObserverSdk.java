/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics;

import io.opentelemetry.api.metrics.DoubleSumObserver;
import io.opentelemetry.sdk.metrics.AbstractAsynchronousInstrument.AbstractDoubleAsynchronousInstrument;
import io.opentelemetry.sdk.metrics.common.InstrumentType;
import io.opentelemetry.sdk.metrics.common.InstrumentValueType;
import java.util.function.Consumer;
import javax.annotation.Nullable;

final class DoubleSumObserverSdk extends AbstractDoubleAsynchronousInstrument
    implements DoubleSumObserver {

  DoubleSumObserverSdk(
      InstrumentDescriptor descriptor,
      InstrumentProcessor instrumentProcessor,
      @Nullable Consumer<DoubleResult> metricUpdater) {
    super(descriptor, instrumentProcessor, metricUpdater);
  }

  static final class Builder
      extends AbstractAsynchronousInstrument.Builder<DoubleSumObserverSdk.Builder>
      implements DoubleSumObserver.Builder {

    @Nullable private Consumer<DoubleResult> callback;

    Builder(
        String name,
        MeterProviderSharedState meterProviderSharedState,
        MeterSharedState meterSharedState) {
      super(
          name,
          InstrumentType.SUM_OBSERVER,
          InstrumentValueType.DOUBLE,
          meterProviderSharedState,
          meterSharedState);
    }

    @Override
    Builder getThis() {
      return this;
    }

    @Override
    public Builder setUpdater(Consumer<DoubleResult> updater) {
      this.callback = updater;
      return this;
    }

    @Override
    public DoubleSumObserverSdk build() {
      return build(
          (instrumentDescriptor, instrumentProcessor) ->
              new DoubleSumObserverSdk(instrumentDescriptor, instrumentProcessor, callback));
    }
  }
}
