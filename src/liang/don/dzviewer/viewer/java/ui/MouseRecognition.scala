package liang.don.dzviewer.viewer.java.ui

import swing.event.{MouseWheelMoved, MouseClicked}

/**
 * Decorator for ImagePanel that recognizes<br>
 * - Double-click events to zoom-in the image<br>
 * - Mouse drag/move events to move around a zoomed in image.<br>
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
trait MouseRecognition extends ImagePanel {

  // Mouse events
  listenTo(mouse.clicks)
  listenTo(mouse.wheel)

  reactions += {
    case MouseClicked(source, point, modifiers, clicks, triggersPopup) => {
      if (clicks == 2) {
        println(getClass.getName + "#mouseClicked] Double click detected at: [" + point + "], scaledX: " + _mouseClickedX + ", scaledY: " + _mouseClickedY)
        _mouseClickedX += (point.getX.toInt * 2) // Doubling mouse coordinates as the next zoom level's image resolution is 2x the size
        _mouseClickedY += (point.getY.toInt * 2) // See above
//        _mouseClickedX = point.getX.toInt * zoomInFactorCallback() // Doubling mouse coordinates as the next zoom level's image resolution is 2x the size
//        _mouseClickedY = point.getY.toInt * zoomInFactorCallback() // See above
        println(getClass.getName + "#mouseClicked] scaledX: " + _mouseClickedX + ", scaledY: " + _mouseClickedY)
        zoomInEventCallback()
      }
    }
    case MouseWheelMoved(source, point, modifiers, rotation) => {
      if (rotation < 0) {
        // Wheel moved up; zoom in action
        // TODO (rotation + 1) / 2 for 1 level of zoom until max
        println(getClass.getName + "mouseWheelMoved] Mouse wheel UP detected. Zooming in...")
        zoomInEventCallback()
      } else {
        // Wheel move down: zoom out action
        // TODO (rotation + 1) / 2 for 1 level of zoom until min
        println(getClass.getName + "mouseWheelMoved] Mouse wheel DOWN detected. Zooming out...")
        zoomInEventCallback()
      }
    }

  }

  // TODO Support wheel up/down for zoom in / zoom out
}
