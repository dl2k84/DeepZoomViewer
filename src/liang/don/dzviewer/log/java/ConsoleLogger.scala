package liang.don.dzviewer.log.java

import liang.don.dzviewer.log.{LoggerInterface, LogLevel}
import java.text.SimpleDateFormat
import java.lang.StringBuffer

/**
 * Java console logger.
 *
 * @author Don Liang
 * @Version 0.1, 15/09/2011
 */
trait ConsoleLogger extends LoggerInterface {

  private val df = new SimpleDateFormat("HH:mm:ss")

  override def log(message: String) {
    log(message, LogLevel.Info, null)
  }

  override def log(message: String, logLevel: LogLevel.Value) {
    log(message, logLevel, null)
  }

  override def log(message: String, logLevel: LogLevel.Value, exception: Exception) {
    if (_minimumOutputLevel <= logLevel) {
      print("[" + df.format(System.currentTimeMillis()) + "]")
      println(formatMessage(message, logLevel))
      if (exception != null) {
        exception.printStackTrace()
      }
    }
  }

  private def formatMessage(message: String, logLevel: LogLevel.Value): String = {
    val sb = new StringBuffer
    sb.append("[").append(padRight(logLevel.toString.toUpperCase)).append("]").append(message).toString
  }

  private def padRight(message: String): String = {
    // Adds whitespace padding to the left of the message so all log level output looks better formatted.
    "%5s".format(message)
  }
}
