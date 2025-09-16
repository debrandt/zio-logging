/*
 * Copyright 2019-2024 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zio.logging.sentry.bridge

import io.sentry.{ ITransport, SentryEvent, SentryLevel }
import zio.logging.LogFilter
import zio.{ Cause, Fiber, FiberId, FiberRef, FiberRefs, LogLevel, Runtime, Trace, Unsafe }

final class ZioLoggerRuntime(runtime: Runtime[Any], filter: LogFilter[Any]) extends ITransport {

  override def send(event: SentryEvent, hint: io.sentry.Hint): io.sentry.TransportResult = {
    val message = Option(event.getMessage).map(_.getFormatted).getOrElse("")
    val level = Option(event.getLevel).getOrElse(SentryLevel.INFO)
    val loggerName = Option(event.getLogger).getOrElse("sentry")
    val throwable = Option(event.getThrowable).orNull

    if (!isEnabled(loggerName, level, message)) {
      return io.sentry.TransportResult.success()
    }

    Unsafe.unsafe { implicit u =>
      val logLevel     = ZioLoggerRuntime.logLevelMapping(level)
      val trace        = Trace.empty
      val fiberId      = FiberId.Gen.Live.make(trace)
      val currentFiber = Fiber._currentFiber.get()

      val currentFiberRefs = if (currentFiber eq null) {
        runtime.fiberRefs.joinAs(fiberId)(FiberRefs.empty)
      } else {
        runtime.fiberRefs.joinAs(fiberId)(currentFiber.unsafe.getFiberRefs())
      }

      val logSpan    = zio.LogSpan(loggerName, java.lang.System.currentTimeMillis())
      val loggerNameAnnotation = (zio.logging.loggerNameAnnotationKey -> loggerName)

      val fiberRefs = currentFiberRefs
        .updatedAs(fiberId)(FiberRef.currentLogSpan, logSpan :: currentFiberRefs.getOrDefault(FiberRef.currentLogSpan))
        .updatedAs(fiberId)(
          FiberRef.currentLogAnnotations,
          currentFiberRefs.getOrDefault(FiberRef.currentLogAnnotations) + loggerNameAnnotation
        )

      val fiberRuntime = zio.internal.FiberRuntime(fiberId, fiberRefs, runtime.runtimeFlags)

      val cause = if (throwable != null) {
        Cause.die(throwable)
      } else {
        Cause.empty
      }

      fiberRuntime.log(() => message, cause, Some(logLevel), trace)
    }

    io.sentry.TransportResult.success()
  }

  override def close(timeout: Long): io.sentry.TransportResult =
    io.sentry.TransportResult.success()

  override def flush(timeoutMills: Long): io.sentry.TransportResult =
    io.sentry.TransportResult.success()

  private def isEnabled(loggerName: String, level: SentryLevel, message: String): Boolean = {
    val logLevel = ZioLoggerRuntime.logLevelMapping(level)

    filter(
      Trace(loggerName, "", 0),
      FiberId.None,
      logLevel,
      () => message,
      Cause.empty,
      FiberRefs.empty,
      List.empty,
      Map(zio.logging.loggerNameAnnotationKey -> loggerName)
    )
  }
}

object ZioLoggerRuntime {

  private val logLevelMapping: Map[SentryLevel, LogLevel] = Map(
    SentryLevel.FATAL   -> LogLevel.Fatal,
    SentryLevel.ERROR   -> LogLevel.Error,
    SentryLevel.WARNING -> LogLevel.Warning,
    SentryLevel.INFO    -> LogLevel.Info,
    SentryLevel.DEBUG   -> LogLevel.Debug
  )
}