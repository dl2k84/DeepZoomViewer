package liang.don.dzviewer.viewer.java.ui

import swing._
import java.awt.image.BufferedImage
import java.lang.Object
import collection.mutable.HashMap
import liang.don.dzviewer.tile.java.TileWrapper
import liang.don.dzviewer.{ImageFetcher, DeepZoomViewerMain}
import liang.don.dzviewer.config.ViewerProperties
import liang.don.dzviewer.tile.{Tile, ImageSize, ImageTile}
import liang.don.dzviewer.log.{LogLevel, Logger}
import java.awt.{RenderingHints, Dimension, Color}

/**
 * Handles the drawing and display of image tiles of a DeepZoom image.<br>
 * This is the Java runtime based version of the viewer.
 *
 * @constructor Create a new viewer UI with the total pages value of the
 *                DeepZoom image collection.
 * @param totalpages The total amount of pages in this DeepZoom collection.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
class ImagePanel(totalPages: Int) extends Panel {

  background = Color.black
  preferredSize = new Dimension(DeepZoomViewerMain.PREFERRED_WINDOW_WIDTH, DeepZoomViewerMain.PREFERRED_WINDOW_HEIGHT)
  minimumSize = preferredSize

  private val page2ZoomedLevelCallbackArray: Array[collection.mutable.Map[Int, () => Array[ImageTile]]] = Array.ofDim(totalPages)
  private val paintLock: AnyRef = new Object

  private var currentImage: Array[ImageTile] = null
  private var isDrawThumbnail: Boolean = true
  private var _optimalImageSize: ImageSize = null
  private var _pageToShow: Int = 0 // 0-index origin
  private var _scaleRatio: Double = 1.0
  private var _zoomToShow: Int = 0
  private var _zoomInFactorCallback: () => Int = null
  private var _zoomInEventCallback: () => Unit = null
  private var _zoomOutEventCallback: () => Unit = null
  protected var _mouseClickedX: Int = -1
  protected var _mouseClickedY: Int = -1

  def optimalImageSize = _optimalImageSize
  def optimalImageSize_=(value: ImageSize) {
    _optimalImageSize = value
  }

  def pageToShow = _pageToShow
  def pageToShow_=(value: Int) {
    _pageToShow = value
    isDrawThumbnail = true
  }

  def zoomToShow = _zoomToShow
  def zoomToShow_=(value: Int) {
    _zoomToShow = value
  }

  def zoomInFactorCallback = _zoomInFactorCallback
  def zoomInFactorCallback_=(value: => Int) {
    _zoomInFactorCallback = { () => value }
  }

  def zoomInEventCallback = _zoomInEventCallback
  def zoomInEventCallback_=(value: () => Unit) {
    _zoomInEventCallback = value
  }

  def zoomOutEventCallback = _zoomOutEventCallback
  def zoomOutEventCallback_=(value: () => Unit) {
    _zoomOutEventCallback = value
  }

  def mouseClickedX = _mouseClickedX
  def mouseClickedX_=(value: Int) {
    _mouseClickedX = value
  }

  def mouseClickedY = _mouseClickedY
  def mouseClickedY_=(value: Int) {
    _mouseClickedY = value
  }

  def updateZoomOutFactor(factor: Int) {
    _mouseClickedX /= factor
    _mouseClickedY /= factor
  }

  def resetMouseClickedPoint() {
    _mouseClickedX = -1
    _mouseClickedY = -1
  }

  def scaleRatio = _scaleRatio
  def scaleRatio_=(value: Double) {
    _scaleRatio = value
  }

  private def drawThumbnail(g: Graphics2D) {
    while (!isThumbnailLoaded(_pageToShow)) { Thread sleep 100 } // TODO Find alternative to thread.sleep!

    val scaleFactor = (_zoomToShow - ViewerProperties.thumbnailLevel)// + 1
    val thumbnail = page2ZoomedLevelCallbackArray(_pageToShow)(ViewerProperties.thumbnailLevel)()(0)
    val xCenter = _mouseClickedX / scaleFactor
    val yCenter = _mouseClickedY / scaleFactor
    val thumbnailImage = thumbnail.image.asInstanceOf[TileWrapper].image.asInstanceOf[BufferedImage]

    Logger.instance.log("[" + getClass.getName + "#drawThumbnail] ThumbnailLevel=" + ViewerProperties.thumbnailLevel + ", for pg=" + _pageToShow + ", scaleFactor=" + scaleFactor + ", zoomToShow=" + zoomToShow + ", mouseX=" + _mouseClickedX + ", mouseY=" + _mouseClickedY, LogLevel.Debug)

    val dx1: Int = if (_mouseClickedX < 1) 0 else (xCenter - (size.width / 2)).abs
    val dy1: Int = if (_mouseClickedY < 1) 0 else (yCenter - (size.height / 2)).abs
    val dx2: Int = _optimalImageSize.width
    val dy2: Int = _optimalImageSize.height

    val sx1: Int = 0
    val sy1: Int = 0
    val sx2: Int = thumbnailImage.getWidth
    val sy2: Int = thumbnailImage.getHeight
    Logger.instance.log("[" + getClass.getName + "#drawThumbnail] dx1: " + dx1 + ", dy1: " + dy1 + ", dx2: " + dx2 + ", dy2: " + dy2 + " || sx1: " + sx1 + ", sy1: " + sy1 + ", sx2: " + sx2 + ", sy2: " + sy2, LogLevel.Debug)
    // Using Bilinear instead of Bicubic since I felt rendering time is more important than absolute quality.
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.drawImage(thumbnailImage, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null)
  }

  override def paintComponent(g: Graphics2D) {
    if (isDrawThumbnail) {
      super.paintComponent(g) // clears this panel of graphics.
      drawThumbnail(g)
      isDrawThumbnail = false
      repaint()
      return
    }

    while (!isTilesLoaded(_pageToShow, _zoomToShow)) {
      paintLock.synchronized {
        Logger.instance.log("[" + getClass.getName + "#paintComponent] Awaiting tiles to finish downloading...", LogLevel.Debug)
        paintLock.wait(1000)
      }
    }

    val tiles = page2ZoomedLevelCallbackArray(_pageToShow)(_zoomToShow)()
    currentImage = tiles
    var adjustTile = true
    tiles.foreach {
      tile => {
        if (tile != null) {
          val currentTile = tile.image.asInstanceOf[TileWrapper].image.asInstanceOf[BufferedImage]
          var tilePosX: Int = tile.position.x
          var tilePosY: Int = tile.position.y
          if (_scaleRatio != 1) {
            if (0 < tile.column) {
              tilePosX = (((tilePosX + 1) * _scaleRatio) - 1).toInt
            } else {
              tilePosX = (tilePosX * _scaleRatio).toInt
            }
            if (0 < tile.row) {
              tilePosY = (((tilePosY + 1) * _scaleRatio) - 1).toInt
            } else {
              tilePosY = (tilePosY * _scaleRatio).toInt
            }
          }
          if (_scaleRatio == 1) {
            if (_mouseClickedX < 0 || _mouseClickedY < 0 || tilePosX <  _mouseClickedX || tilePosY <  _mouseClickedY) {
              adjustTile = false
            }
            if (adjustTile) {
              val topLeftX = if (0 < _mouseClickedX) _mouseClickedX else 0
              val topLeftY = if (0 < _mouseClickedY) _mouseClickedY else 0
              if (topLeftX < tilePosX) {
                tilePosX -= topLeftX
              }
              if (topLeftY < tilePosY) {
                tilePosY -= topLeftY
              }
            }

            Logger.instance.log("[" + getClass.getName + "#paintComponent] Displaying non-scaled image around pos {" + tilePosX + "," + tilePosY + "} scaleRatio: " + _scaleRatio + "mouseX= " + _mouseClickedX + ", mouseY=" + mouseClickedY, LogLevel.Debug)
            val dx1: Int = tilePosX - (if (_mouseClickedX < 1) 0 else _mouseClickedX - (size.width / 2))
            val dy1: Int = tilePosY - (if (_mouseClickedY < 1) 0 else _mouseClickedY - (size.height / 2))
            val dx2: Int = dx1 + tile.tileSize
            val dy2: Int = dy1 + tile.tileSize
            val sx1: Int = if (0 < tile.column) tile.overlapSize else 0
            val sy1: Int = if (0 < tile.row) tile.overlapSize else 0
            val sx2: Int = if (0 < tile.column) sx1 + tile.tileSize else sx1 + tile.tileSize - 1
            val sy2: Int = if (0 < tile.row) sy1 + tile.tileSize else sy1 + tile.tileSize - 1
            Logger.instance.log("[" + getClass.getName + "#paintComponent] dx1: " + dx1 + ", dy1: " + dy1 + ", dx2: " + dx2 + ", dy2: " + dy2 + " || sx1: " + sx1 + ", sy1: " + sy1 + ", sx2: " + sx2 + ", sy2: " + sy2, LogLevel.Debug)
            g.drawImage(currentTile, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null)
          } else {
            Logger.instance.log("[" + getClass.getName + "#paintComponent]    Displaying scaled image around pos {" + tilePosX + "," + tilePosY + "} scaleRatio: " + _scaleRatio, LogLevel.Debug)
            val dx1: Int = tilePosX - (if (_mouseClickedX < 1) 0 else _mouseClickedX - (size.width / 2))
            val dy1: Int = tilePosY - (if (_mouseClickedY < 1) 0 else _mouseClickedY - (size.height / 2))
            val dx2: Int = dx1 + (tile.tileSize * _scaleRatio).toInt + (if (0 < tile.column) 0 else 1)
            val dy2: Int = dy1 + (tile.tileSize * _scaleRatio).toInt + (if (0 < tile.row) 0 else 1)
            val sx1: Int = if (0 < tile.column) tile.overlapSize else 0
            val sy1: Int = if (0 < tile.row) tile.overlapSize else 0
            val sx2: Int = sx1 + tile.tileSize - (if (0 < tile.column) 0 else 1)
            val sy2: Int = sy1 + tile.tileSize - (if (0 < tile.row) 0 else 1)
            Logger.instance.log("[" + getClass.getName + "#paintComponent] dx1: " + dx1 + ", dy1: " + dy1 + ", dx2: " + dx2 + ", dy2: " + dy2 + " || sx1: " + sx1 + ", sy1: " + sy1 + ", sx2: " + sx2 + ", sy2: " + sy2, LogLevel.Debug)
            // Using Bilinear instead of Bicubic since I felt rendering time is more important than absolute quality.
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(currentTile, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null)
          }
        }
      }
    }
    _scaleRatio = 1
  }

  /**
   * If the image tile of the specified page is cached,
   * return true, else return false.
   *
   * @param page The page to check.
   * @param zoomLevel The zoom level to check.
   * @param tile The tile information to check against.
   *
   * @return True if the image tile is cached, else false.
   */
  def isTileLoaded(page: Int, zoomLevel: Int, tile: Tile): Boolean = {
    val zoom2CallbackMap = page2ZoomedLevelCallbackArray(page)
    if (zoom2CallbackMap != null) {
      val callback = zoom2CallbackMap.getOrElse(zoomLevel, null)
      if (callback != null) {
        val pageTiles = callback()
        if (pageTiles != null && pageTiles.length > 0) {
          pageTiles.foreach {
            thisTile => {
              if (thisTile.column == tile.column
                && thisTile.row == tile.row
                && thisTile.image != null
                && thisTile.image.asInstanceOf[TileWrapper].image != null) {
//                Logger.instance.log("[" + getClass.getName + "#isTileLoaded] Tile is cached. col=" + thisTile.column + ", row=" + thisTile.row + ", pos=(" + thisTile.position.x + ", " + thisTile.position.y + ") im=" + thisTile.image, LogLevel.Debug)
                return true
              }
            }
          }
        }
      }
    }
    false
  }

  /**
   * If the thumbnail tile (max 256x256) of the specified page is cached,
   * return true, else return false.
   *
   * @param page The page to check.
   *
   * @return True if the thumbnail tile for the specified page is cached, else false.
   */
  def isThumbnailLoaded(page: Int): Boolean = {
    val zoom2CallbackMap = page2ZoomedLevelCallbackArray(page)
    if (zoom2CallbackMap != null) {
      val callback = zoom2CallbackMap.getOrElse(ViewerProperties.thumbnailLevel, null)
      if (callback != null) {
        val thumbnailTile = callback()
        if (thumbnailTile != null && thumbnailTile.length > 0) {
          val tile = thumbnailTile(0)
          return tile != null && tile.image != null && tile.image.asInstanceOf[TileWrapper].image != null
        }
      }
    }

    false
  }

  /**
   * If all relevant image tile(s) of the specified page are cached,
   * return true, else return false.
   *
   * @param page The page to check.
   * @param zoomLevel The zoom level to check.
   *
   * @return True if the specified image tile(s) are cached, else false.
   */
  def isTilesLoaded(page: Int, zoomLevel: Int): Boolean = {
    if (size.width == 0 || size.height == 0) {
      // TODO See why sometimes this is 0 on start.
      return false
    }

    val zoom2CallbackMap = page2ZoomedLevelCallbackArray(page)
    if (zoom2CallbackMap != null) {
      val callback = zoom2CallbackMap.getOrElse(zoomLevel, null)
      if (callback != null) {
        val pageTiles = callback()
        if (pageTiles != null && pageTiles.length > 0) {
          // Check that each tile image is loaded.
          val xStart = if (_mouseClickedX < 0) 0 else  (_mouseClickedX - (size.width / 2) - 1)
          val xEnd = xStart + size.width
          val yStart = if (_mouseClickedY < 0) 0 else  (_mouseClickedY - (size.height / 2) - 1)
          val yEnd = yStart + size.height
          val startCol = ImageFetcher.calculateGridNumber(xStart + 1, ViewerProperties.tileSize)
          val endCol = ImageFetcher.calculateGridNumber(xEnd, ViewerProperties.tileSize)
          val startRow = ImageFetcher.calculateGridNumber(yStart + 1, ViewerProperties.tileSize)
          val endRow = ImageFetcher.calculateGridNumber(yEnd, ViewerProperties.tileSize)

          val filteredTiles = pageTiles.filter {
            tile => {
              if (tile != null) {
                val colInRange = tile.column match {
                  case i if startCol to endCol contains i => true
                  case _ => false
                }

                val rowInRange = tile.row match {
                  case i if startRow to endRow contains  i => true
                  case _ => false
                }

                colInRange && rowInRange
              } else {
                false
              }
            }
          }

          filteredTiles.foreach(tile => if (tile == null || tile.image == null || tile.image.asInstanceOf[TileWrapper].image == null) { return false })
          paintLock.synchronized {
            paintLock.notify()
          }
          return true
        }
      }
    }
    false
  }

  /**
   * Store the callback to the cached image tile(s).<br>
   * This can be used elsewhere to get the stored callback to retrieve
   * the cached tile.<br>
   *
   * @param page The page that the callback refers to,
   * @param zoomLevel The zoom level of the page that the callback refers to.
   * @param callback The callback function that gets the cached image tile(s).
   */
  def setTiles(page: Int, zoomLevel: Int, callback: () => Array[ImageTile]) {
    val zoom2CallbackMap = {
      val map = page2ZoomedLevelCallbackArray(page)
      if (map != null) {
        map
      } else {
        val newMap = new HashMap[Int, () => Array[ImageTile]]
        page2ZoomedLevelCallbackArray(page) = newMap
        newMap
      }
    }
    zoom2CallbackMap.put(zoomLevel, callback)
  }

  /**
   * Adjusts the display offset of the image tiles.<br>
   * This is to prevent displaying offscreen (empty) pixels.<br>
   *
   * @param xOffset The x-coordinate offset to adjust.
   * @param yOffset The y-coordinate offset to adjust.
   */
  def adjustOffset(xOffset: Int, yOffset: Int, page: Int) {
    val newTiles:Array[ImageTile] = Array.ofDim[ImageTile](currentImage.size)
    currentImage.zipWithIndex.foreach { tile => {
      val oldTile = tile._1
//      Logger.instance.log("[" + getClass.getName + "#adjustOffset] Adjusting offset x: " + oldTile.position.x + " - " + xOffset + ", y: " + oldTile.position.y + " - " + yOffset, LogLevel.Debug)
      newTiles(tile._2) = new ImageTile(oldTile.image, oldTile.uriSource, oldTile.thumbnailUri, oldTile.fileFormat, new liang.don.dzviewer.tile.Point(oldTile.position.x - xOffset.abs, oldTile.position.y - yOffset.abs), oldTile.overlapSize, oldTile.column, oldTile.row, oldTile.tileSize)
    }}
    currentImage = newTiles
  }

  /**
   * Gets the coordinates of the top left where drawing of the image tile(s) start.
   *
   * @param zoomedImageCenter The x,y coordinate representing the center of the image.
   * @param imageSize The image size of the current depth of zoom.
   *
   * @return The top left coordinate of where the image tile(s) start drawing.
   */
  def getTopLeft(zoomedImageCenter: liang.don.dzviewer.tile.Point, imageSize: ImageSize): liang.don.dzviewer.tile.Point = {

    Logger.instance.log("[" + getClass.getName + "#getTopLeft] visibleSize=" + size + ", xCenter=" + zoomedImageCenter.x + ", yCenter=" + zoomedImageCenter.y, LogLevel.Debug)
    var topLeftX: Int = (zoomedImageCenter.x) - (size.width / 2)
    var topLeftY: Int = (zoomedImageCenter.y) - (size.height / 2)

    // Paint / download only the tiles that is visible in this Panel
    // Get the top-left (x, y) coordinates of the image relative to the top-left of this panel.

    // If the zoom point shifts the image outside of the visible boundaries of the image, correct it to the min/max boundary of the image.
    if (topLeftX + size.width > imageSize.width) {
      topLeftX = imageSize.width - size.width
    }
    if (topLeftY + size.height > imageSize.height) {
      topLeftY = imageSize.height - size.height
    }

    // TODO remove this once the image scaling via the imageQuality setting is implemented
    Logger.instance.log("[" + getClass.getName + "#getTopLeft] newImgw=" + imageSize.width + ", newImgH=" + imageSize.height + " topleftX=" + topLeftX + " topLeftY=" + topLeftY, LogLevel.Debug)
    if (imageSize.width <= size.width || topLeftX < 0) {
      topLeftX = 0
    }
    if (imageSize.height <= size.height || topLeftY < 0) {
      topLeftY = 0
    }


//    Logger.instance.log("[" + getClass.getName + "#getTopLeft] Zooming image around top-left point { " + topLeftX + ", " + topLeftY + " } centered at { " + zoomedImageCenter.x + ", " + zoomedImageCenter.y + " }")
//    Logger.instance.log("[" + getClass.getName + "#getTopLeft] {visibleWidth: " + size.width + ", visibleHeight: " + size.height + "} || {imageSize.width: " + imageSize.width + ", imageSize.height: " + imageSize.height + "}")

    _mouseClickedX = topLeftX + (size.width / 2)
    _mouseClickedY = topLeftY + (size.height / 2)

    Logger.instance.log("[" + getClass.getName + "#getTopLeftX] mouseX=" + _mouseClickedX + ", mouseY=" + _mouseClickedY, LogLevel.Debug)

    new liang.don.dzviewer.tile.Point(topLeftX, topLeftY)
  }
}
