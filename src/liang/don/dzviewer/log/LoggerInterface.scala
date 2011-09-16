package liang.don.dzviewer.log

import liang.don.dzviewer.config.ViewerProperties

/**
 * Logs information.
 *
 * @author Don Liang
 * @Version 0.1, 15/09/2011
 */
abstract class LoggerInterface {

  /**
   * Sets the minimum log level to output.
   * Log message with levels below the specified logLevel will not be outputted.
   */
  protected val _minimumOutputLevel: LogLevel.Value = ViewerProperties.minimumLogLevel

  /**
   * Logs a message with a default log level.
   *
   * @param message The message to log.
   */
  def log(message: String)

  /**
   * Logs a message with the specified log level.
   *
   * @param message The message to log.
   * @param logLevel The log level of this message.
   */
  def log(message: String, logLevel: LogLevel.Value)

  /**
   * Logs a message with the specified log level and exception.
   *
   * @param message The message to log.
   * @param logLevel The log level of this message.
   * @param exception The exception instance to log with this message.
   */
  def log(message: String, logLevel: LogLevel.Value, exception: Exception)

}
