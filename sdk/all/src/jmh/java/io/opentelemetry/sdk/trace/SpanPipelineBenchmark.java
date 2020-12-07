/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.trace;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@State(Scope.Benchmark)
public class SpanPipelineBenchmark {
  private static SpanBuilderSdk spanBuilderSdk;
  private static final DockerImageName OTLP_COLLECTOR_IMAGE =
      DockerImageName.parse("otel/opentelemetry-collector-dev:latest");


  public static void main (String[] args) {
    // Configuring the collector test-container
    GenericContainer<?> collector = new GenericContainer<>(OTLP_COLLECTOR_IMAGE)
        .withExposedPorts(5678, 13133)
        .waitingFor(Wait.forHttp("/").forPort(13133))
//        .waitingFor(Wait.defaultWaitStrategy())
//        .waitingFor(
//            Wait.forLogMessage(".*Everything is ready. Begin running and processing data.*", 1)
//        )
        .withCopyFileToContainer(MountableFile.forClasspathResource("/otel.yaml"), "/etc/otel.yaml")
        .withCommand("--config /etc/otel.yaml");
    try {
      // starting the collector container
      collector.start(); // TODO: The container starts for each benchmark. Do it only once for the class.
    }catch (Exception e) {
      System.out.println("Container Exception caught!!!");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }

    // Getting logs from the collector container
    final String logs = collector.getLogs();
    System.out.println("Container logs::::::");
    System.out.println(logs);
    System.out.println("======");
  }


  @Setup(Level.Trial)
  public final void setup() {
    System.out.println("Inside setup method");

    // Configuring the collector test-container
    GenericContainer<?> collector = new GenericContainer<>(OTLP_COLLECTOR_IMAGE)
        .withExposedPorts(5678, 13133)
        .waitingFor(Wait.forHttp("/").forPort(13133))
        .withCopyFileToContainer(MountableFile.forClasspathResource("/otel.yaml"), "/etc/otel.yaml")
        .withCommand("--config /etc/otel.yaml");
    try {
      // starting the collector container
      collector.start(); // TODO: The container starts for each benchmark. Do it only once for the class.
    }catch (Exception e) {
      System.out.println("Container Exception caught!!!");
      System.out.println(e.getMessage());
      e.printStackTrace();
    }

    // Getting logs from the collector container
    final String logs = collector.getLogs();
    System.out.println("Container logs::::::");
    System.out.println(logs);
    System.out.println("======");
    // ToStringConsumer toStringConsumer = new ToStringConsumer();
    // collector.followOutput(toStringConsumer, OutputFrame.OutputType.STDOUT);

    String address =
        collector.getHost() + ":" + collector.getMappedPort(5678);
    System.out.println("collector address:::: " + address);

    TracerSdkProvider tracerProvider = TracerSdkProvider.builder().build();

    SimpleSpanProcessor spanProcessor = SimpleSpanProcessor
        .builder(OtlpGrpcSpanExporter.builder()
//            .setEndpoint("localhost:1234")
            .setEndpoint(address)
            .setDeadlineMs(10000)
            .build())
        .build();

    tracerProvider.addSpanProcessor(spanProcessor);

    TraceConfig alwaysOn =
        tracerProvider.getActiveTraceConfig().toBuilder().setSampler(Sampler.alwaysOn()).build();
    tracerProvider.updateActiveTraceConfig(alwaysOn);

    Tracer tracerSdk = tracerProvider.get("PipelineBenchmarkTracer");
    spanBuilderSdk =
        (SpanBuilderSdk)
            tracerSdk.spanBuilder("PipelineBenchmarkSpan")
                .setSpanKind(Span.Kind.SERVER); // TODO: Remove this
  }

  @Benchmark
  @Threads(value = 1)
  @Fork(1)
  @Warmup(iterations = 1, time = 1)
  @Measurement(iterations = 1, time = 1)
  @OutputTimeUnit(TimeUnit.SECONDS)
  public void runThePipeline_01Threads() {
    doWork();
  }

//  @Benchmark
//  @Threads(value = 1)
//  @Fork(1)
//  @Warmup(iterations = 1, time = 1)
//  @Measurement(iterations = 1, time = 1)
//  @OutputTimeUnit(TimeUnit.SECONDS)
//  public void runThePipeline_05Threads() {
//    doWork();
//  }

  private static void doWork() {
    Span span = spanBuilderSdk.startSpan();
    for (int i = 0; i < 10; i++) {
      span.setAttribute("benchmarkAttribute_" + i, "benchmarkAttrValue_" + 1);
    }
    span.end();
  }
}
