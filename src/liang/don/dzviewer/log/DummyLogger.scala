package liang.don.dzviewer.log

/**
 * Dummy Logger.
 *
 * @author Don Liang
 * @Version 0.1, 15/09/2011
 */
trait DummyLogger extends LoggerInterface {

  override def log(message: String) { }

  override def log(message: String, logLevel: LogLevel.Value) { }

  override def log(message: String, logLevel: LogLevel.Value, exception: Exception) { }

}
