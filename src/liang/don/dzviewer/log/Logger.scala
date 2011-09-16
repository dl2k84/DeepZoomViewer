package liang.don.dzviewer.log

import liang.don.dzviewer.config.ViewerProperties
import liang.don.dzviewer.config.ViewerProperties.{LogType, BuildTarget}

/**
 * Logs information.
 *
 * @author Don Liang
 * @Version 0.1, 15/09/2011
 */
object Logger {

  protected val baseFolder = "log"

  private val _logger: LoggerInterface = {
    val buildTarget = ViewerProperties.buildTarget
    val logType = ViewerProperties.logType
    if (LogType.Console == logType) {
      if (BuildTarget.Java == buildTarget) {
        new LoggerInterface with java.ConsoleLogger
      } else if (BuildTarget.Net == buildTarget) {
        // TODO .Net impl
        new LoggerInterface with net.ConsoleLogger
      } else {
        sys.error("[" + getClass.getName + "] Invalid buildTarget.")
      }
    } else if (LogType.File == logType) {
      if (BuildTarget.Java == buildTarget) {
        new LoggerInterface with java.FileLogger
      } else if (BuildTarget.Net == buildTarget) {
        // TODO .Net impl
        new LoggerInterface with net.FileLogger
      } else {
        sys.error("[" + getClass.getName + "] Invalid buildTarget.")
      }
    } else {
      // no logging.
      new LoggerInterface with DummyLogger
    }
  }

  def instance: LoggerInterface = _logger
}
