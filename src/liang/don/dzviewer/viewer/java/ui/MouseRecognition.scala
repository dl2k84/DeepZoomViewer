package liang.don.dzviewer.viewer.java.ui

import swing.event.{MouseWheelMoved, MouseClicked}
import liang.don.dzviewer.log.{LogLevel, Logger}

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
        Logger.instance.log("[" + getClass.getName + "#MouseClicked] Double click detected at: [" + point + "], scaledX: " + _mouseClickedX + ", scaledY: " + _mouseClickedY, LogLevel.Debug)
        _mouseClickedX += (point.getX.toInt * 2) // Doubling mouse coordinates as the next zoom level's image resolution is 2x the size
        _mouseClickedY += (point.getY.toInt * 2) // See above
        Logger.instance.log("[" + getClass.getName + "#MouseClicked] scaledX: " + _mouseClickedX + ", scaledY: " + _mouseClickedY, LogLevel.Debug)
        zoomInEventCallback()
      }
    }
    case MouseWheelMoved(source, point, modifiers, rotation) => {
      if (rotation < 0) {
        // Wheel moved up; zoom in action
        // TODO (rotation + 1) / 2 for 1 level of zoom until max
        Logger.instance.log("[" + getClass.getName + "#MouseWheelMoved] Mouse wheel UP detected. Zooming in...", LogLevel.Debug)
        zoomInEventCallback()
      } else {
        // Wheel move down: zoom out action
        // TODO (rotation + 1) / 2 for 1 level of zoom until min
        Logger.instance.log("[" + getClass.getName + "#MouseWheelMoved] Mouse wheel DOWN detected. Zooming out...", LogLevel.Debug)
        zoomInEventCallback()
      }
    }

  }

  // TODO Support wheel up/down for zoom in / zoom out
}
