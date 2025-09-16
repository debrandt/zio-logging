package zio.logging.sentry.bridge

import io.sentry.{ Sentry, SentryLevel }
import zio.logging.LogFilter
import zio.test._
import zio.{ ConfigProvider, LogLevel, ZIO, ZIOAspect }

object SentryBridgeSpec extends ZIOSpecDefault {

  final case class LogEntry(
    span: List[String],
    level: LogLevel,
    annotations: Map[String, String],
    message: String
  )

  override def spec =
    suite("SentryBridge")(
      test("parallel init") {
        for {
          _ <-
            ZIO.foreachPar((1 to 5).toList) { _ =>
              ZIO
                .succeed {
                  Sentry.captureMessage("Test message!", SentryLevel.WARNING)
                }
                .provide(SentryBridge.initialize)
            }
        } yield assertCompletes
      },
      test("logs through sentry") {
        val testFailure = new RuntimeException("test error")
        for {
          _      <-
            (for {
              _      <- ZIO.logSpan("span")(ZIO.succeed(Sentry.captureMessage("test debug message", SentryLevel.DEBUG))) @@ ZIOAspect
                          .annotated("trace_id", "tId")
              _      <- ZIO.succeed(Sentry.captureMessage("hello world", SentryLevel.WARNING)) @@ ZIOAspect.annotated("user_id", "uId")
              _      <- ZIO.succeed(Sentry.captureMessage("3..2..1 ... go!", SentryLevel.INFO))
              _      <- ZIO.succeed(Sentry.captureException(testFailure))
            } yield ()).exit
          output <- ZTestLogger.logOutput
          lines   = output.map { logEntry =>
                      LogEntry(
                        logEntry.spans.map(_.label),
                        logEntry.logLevel,
                        logEntry.annotations,
                        logEntry.message()
                      )
                    }
        } yield assertTrue(lines.size >= 3)
      }.provide(
        SentryBridge.init(LogFilter.acceptAll),
        Runtime.removeDefaultLoggers
      )
    )
}