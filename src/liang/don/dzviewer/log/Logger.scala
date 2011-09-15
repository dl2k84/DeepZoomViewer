package liang.don.dzviewer.log

import java.ConsoleLogger
import liang.don.dzviewer.config.ViewerProperties
import liang.don.dzviewer.config.ViewerProperties.{LogType, BuildTarget}

/**
 * Logs information.
 *
 * @author Don Liang
 * @Version 0.1.1, 15/09/2011
 */
object Logger {

  private val _logger: LoggerInterface = {
    val buildTarget = ViewerProperties.buildTarget
    val logType = ViewerProperties.logType
    if (BuildTarget.Java == buildTarget) {
      if (LogType.Console == logType) {
        new LoggerInterface with ConsoleLogger
      } else if (LogType.File == logType) {
        new LoggerInterface with FileLogger
      } else {
        new LoggerInterface with DummyLogger
      }
    } else if (BuildTarget.Net == buildTarget) {
      // TODO
      null
    } else {
      // no logging.
      new LoggerInterface with DummyLogger
    }
  }

  def instance: LoggerInterface = _logger
}
