/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.logging;

import io.opentelemetry.internal.Utils;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logging.data.LogRecord;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LogSinkSdkProvider {
  private final LogSink logSink = new SdkLogSink();
  private final List<LogProcessor> processors = new ArrayList<>();

  private LogSinkSdkProvider() {}

  public LogSink get(String instrumentationName, String instrumentationVersion) {
    // Currently there is no differentiation by instrumentation library
    return logSink;
  }

  public void addLogProcessor(LogProcessor processor) {
    processors.add(Utils.checkNotNull(processor, "Processor can not be null"));
  }

  /**
   * Flushes all attached processors.
   *
   * @return result
   */
  public CompletableResultCode forceFlush() {
    final List<CompletableResultCode> processorResults = new ArrayList<>(processors.size());
    for (LogProcessor processor : processors) {
      processorResults.add(processor.forceFlush());
    }
    return CompletableResultCode.ofAll(processorResults);
  }

  /**
   * Shut down of provider and associated processors.
   *
   * @return result
   */
  public CompletableResultCode shutdown() {
    Collection<CompletableResultCode> processorResults = new ArrayList<>(processors.size());
    for (LogProcessor processor : processors) {
      processorResults.add(processor.shutdown());
    }
    return CompletableResultCode.ofAll(processorResults);
  }

  private class SdkLogSink implements LogSink {
    @Override
    public void offer(LogRecord record) {
      for (LogProcessor processor : processors) {
        processor.addLogRecord(record);
      }
    }
  }

  public static class Builder {
    public LogSinkSdkProvider build() {
      return new LogSinkSdkProvider();
    }
  }
}
