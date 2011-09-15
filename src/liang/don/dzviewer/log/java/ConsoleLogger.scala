package liang.don.dzviewer.log.java

import liang.don.dzviewer.log.{LoggerInterface, LogLevel}
import java.text.SimpleDateFormat

/**
 * Java console (System.out) log..
 *
 * @author Don Liang
 * @Version 0.1.1, 15/09/2011
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
    print("[" + df.format(System.currentTimeMillis()) + "]")
    logLevel match {
      case LogLevel.Debug => print("[" + LogLevel.Debug.toString.toUpperCase + "]")
      case LogLevel.Error => print("[" + LogLevel.Error.toString.toUpperCase + "]")
      case LogLevel.Fatal => print("[" + LogLevel.Fatal.toString.toUpperCase + "]")
      case LogLevel.Info => print("[" + LogLevel.Info.toString.toUpperCase + "]")
    }
    println(message)
    if (exception != null) {
      exception.printStackTrace()
    }
  }

}
