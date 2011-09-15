package liang.don.dzviewer.log

/**
 * Dummy LoggerInterface
 *
 * @author Don Liang
 * @Version 0.1.1, 15/09/2011
 */
trait DummyLogger extends LoggerInterface {

  override def log(message: String) { }

  override def log(message: String, logLevel: LogLevel.Value) { }

  override def log(message: String, logLevel: LogLevel.Value, exception: Exception) { }
}
