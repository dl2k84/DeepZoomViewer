package liang.don.dzviewer.log

/**
 * Log level.<br>
 * Level priority:
 * Fatal > Error > Info > Debug
 *
 * @author Don Liang
 * @Version 0.1, 15/09/2011
 */
object LogLevel extends Enumeration {

  val Debug = Value(0)
  val Info = Value(1)
  val Error = Value(2)
  val Fatal = Value(3)

}
