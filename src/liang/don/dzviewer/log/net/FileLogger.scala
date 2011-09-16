package liang.don.dzviewer.log.net

import liang.don.dzviewer.log.{LogLevel, LoggerInterface}

/**
 * File Logger using .Net (C#) libraries.
 *
 * @author Don Liang
 * @Version 0.1.2, 16/09/2011
 */
trait FileLogger extends LoggerInterface {

  override def log(message: String) {
    sys.error("[" + getClass.getName + "#log] Not implemented.")
  }

  override def log(message: String, logLevel: LogLevel.Value) {
    sys.error("[" + getClass.getName + "#log] Not implemented.")
  }

  override def log(message: String, logLevel: LogLevel.Value, exception: Exception) {
    sys.error("[" + getClass.getName + "#log] Not implemented.")
  }
}
