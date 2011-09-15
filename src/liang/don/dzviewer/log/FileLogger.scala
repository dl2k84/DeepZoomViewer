package liang.don.dzviewer.log

/**
 * File LoggerInterface.
 *
 * @author Don Liang
 * @Version 0.1.1, 15/09/2011
 */
trait FileLogger extends LoggerInterface {

  override def log(message: String) {
    // TODO
  }

  override def log(message: String, logLevel: LogLevel.Value) {
    // TODO
  }

  override def log(message: String, logLevel: LogLevel.Value, exception: Exception) {
    // TODO
  }
}
