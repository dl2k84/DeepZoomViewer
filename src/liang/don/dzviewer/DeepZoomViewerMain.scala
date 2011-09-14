package liang.don.dzviewer

import swing.SimpleSwingApplication

/**
 * Java runtime version of the DeepZoomViewer main class.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
object DeepZoomViewerMain extends SimpleSwingApplication {

  var startTime: Long = 0
  val PREFERRED_WINDOW_WIDTH = 800 //1920
  val PREFERRED_WINDOW_HEIGHT = 600 // 1200
  var VISIBLE_WINDOW_WIDTH = 800 //1920
  var VISIBLE_WINDOW_HEIGHT = 600 // 1200

  def top = new MultiScaleImageBuilder().buildMultiScaleImageViewer().show()
}
