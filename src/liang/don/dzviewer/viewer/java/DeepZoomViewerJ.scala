package liang.don.dzviewer.viewer.java

import liang.don.dzviewer.viewer.DeepZoomViewer
import concurrent.ops._
import liang.don.dzviewer.parser.MultiScaleImageFileParser
import liang.don.dzviewer.{ImageFetcher, DeepZoomViewerMain}
import liang.don.dzviewer.config.ViewerProperties
import swing.event.ButtonClicked
import swing._
import ui.{MouseRecognition, ImagePanel}
import xml.Node
import java.awt.Color
import swing.GridBagPanel.Fill
import liang.don.dzviewer.cache.java.DiskCache
import java.util.UUID
import liang.don.dzviewer.tile.{Point, Tile, ImageSize, ImageTile}
import liang.don.dzviewer.cache.{CacheOptions, DeepZoomCache}
import liang.don.dzviewer.log.{LogLevel, Logger}

/**
 * Java-based viewer that manages information regarding the image or tile's
 * current zoom level, page and so on.
 *
 * @constructor Create a new Java-based viewer with the DeepZoom collection's
 *                descriptor XML, total pages in collection, and mapping of the
 *                tiles to their respective sub image URL
 * @param descriptor The XML descriptor of this DeepZoom image collection.
 * @param totalPages The total amount of pages in this DeepZoom collection.
 * @param tile2SourceMap Mapping of tiles to their respective sub image URL.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
abstract class DeepZoomViewerJ(descriptor: Node, totalPages: Int, tile2SourceMap: Map[Int, String]) extends DeepZoomViewer() {

  private val imagePanel = new ImagePanel(totalPages) with MouseRecognition
  private val imagePanelLock: AnyRef = new Object
  private val diskCache = new DeepZoomCache with DiskCache
  private val page2ZoomLevelArray: Array[Int] = Array.ofDim[Int](totalPages)
  private var zoomLevel = defaultZoomLevel
  private var displayWindow: Frame = null

  private val zoomInEvent = () => {
    spawn {
      imagePanel.scaleRatio = 2

      updateZoom()

      while (imagePanel.scaleRatio != 1) { Thread sleep 100} // TODO find alternative to thread sleep

      increaseZoomLevel()
      updateZoomButtonState()
      updateZoom()
    }
  }

  private val zoomOutEvent = () => {
    spawn {
      decreaseZoomLevel()
      updateZoomButtonState()
      imagePanel.updateZoomOutFactor(math.pow(2, zoomLevel - defaultZoomLevel).toInt)
      updateZoom()
      if (zoomLevel == defaultZoomLevel) {
        imagePanel.resetMouseClickedPoint()
      }
    }
  }

  private var updatePageButtonState: () => Unit = null
  private var updateZoomButtonState: () => Unit = null

  /**
   * Sets the enabled state of the UI buttons based on evaluating the passed condition.
   *
   * @param button The button to toggle.
   * @param condition The boolean condition to evaluate whether the button is enabled or not.
   */
  def toggleButtonEnabled(button: Button, condition: => Boolean) {
    button.enabled = condition
  }

  private def getFileHash(uri: String): String = {
    Logger.instance.log("[" + getClass.getName + "#getFileHash] Generating file hash for uri: " + uri, LogLevel.Debug)
    val sha1 = java.security.MessageDigest.getInstance(CacheOptions.HashType.SHA1.toString)
    val fileHash = sha1.digest(uri.getBytes)
    UUID.nameUUIDFromBytes(fileHash).toString // TODO think more later whether base-64 or Uuid is better.
  }

  private def currentPageImageSize: ImageSize = getImageSize(imagePanel.pageToShow)//MultiScaleImageFileParser.getImageSizeFromCollection(descriptor, imagePanel.pageToShow)

  private def getImageSize(page: Int): ImageSize = {
    if (MultiScaleImageFileParser.isSingleImage(descriptor)) {
      MultiScaleImageFileParser.getImageSize(descriptor)
    } else {
      MultiScaleImageFileParser.getImageSizeFromCollection(descriptor, page)
    }
  }

  private def defaultZoomLevel: Int = {
    ImageFetcher.calculateZoomLevel(0, currentPageImageSize.width, currentPageImageSize.height, DeepZoomViewerMain.PREFERRED_WINDOW_WIDTH, DeepZoomViewerMain.PREFERRED_WINDOW_HEIGHT).level
  }

  private def getOptimalZoomLevel(page: Int): Int = {
    val size = getImageSize(page)
    ImageFetcher.calculateZoomLevel(0, size.width, size.height, DeepZoomViewerMain.PREFERRED_WINDOW_WIDTH, DeepZoomViewerMain.PREFERRED_WINDOW_HEIGHT).level
  }

  private def maxZoomLevel: Int = {
    ImageFetcher.calculateMaximumZoomLevel(currentPageImageSize.width, currentPageImageSize.height)
  }

  private def canDecreaseZoom: Boolean = zoomLevel > defaultZoomLevel
  private def canIncreaseZoom: Boolean = zoomLevel < maxZoomLevel

  private def decreaseZoomLevel() {
    if (canDecreaseZoom) {
      zoomLevel -= 1
      page2ZoomLevelArray(imagePanel.pageToShow) = page2ZoomLevelArray(imagePanel.pageToShow) - 1
    }
  }

  private def increaseZoomLevel() {
    if (canIncreaseZoom) {
      zoomLevel += 1
      page2ZoomLevelArray(imagePanel.pageToShow) = page2ZoomLevelArray(imagePanel.pageToShow) + 1
    }
  }

  private def updatePage() {
    zoomLevel = defaultZoomLevel
    page2ZoomLevelArray(imagePanel.pageToShow) = defaultZoomLevel
    imagePanel.resetMouseClickedPoint()

    // centers the image if it is larger than the viewable size
//    val maxSupportedLevels = MultiScaleImageFileParser.getMaxLevel(descriptor)
//    val displayedZoomLevel = ImageFetcher.calculateZoomLevel(maxSupportedLevels, currentPageImageSize.width, currentPageImageSize.height, DeepZoomViewerMain.PREFERRED_WINDOW_WIDTH, DeepZoomViewerMain.PREFERRED_WINDOW_HEIGHT)
    if (!isTilesCached(imagePanel.pageToShow)) {
      // TODO repetitive. fix this.
      val maxSupportedLevels = {
        if (MultiScaleImageFileParser.isSingleImage(descriptor)) {
          0
        } else {
          MultiScaleImageFileParser.getMaxLevel(descriptor)
        }
      }
      val subImageMap = ImageFetcher.fetchImageDescriptor(ViewerProperties.baseUrl, tile2SourceMap(imagePanel.pageToShow))
      val pageTiles = ImageFetcher.generatePageTiles(ImageFetcher.getSubImageUrl(ViewerProperties.baseUrl, tile2SourceMap(imagePanel.pageToShow)), subImageMap, maxSupportedLevels)
      val t = pageTiles(0)
      createThumbnail(imagePanel.pageToShow, new Tile(t.uriSource, t.thumbnailUri, t.fileFormat, new liang.don.dzviewer.tile.Point(0, 0), t.overlapSize, 0, 0, t.tileSize))
      create(imagePanel.pageToShow, pageTiles)
    } else {
      Logger.instance.log("[" + getClass.getName + "#updatePage] ***** Image tiles for this page already loaded. *****", LogLevel.Debug)
    }

    imagePanel.repaint()
  }

  /**
   * Zooms the image around the specified top-left (x, y) point of the zoomed image.
   */
  private def updateZoom() {
    val zoomedImageCenter = {
      if (imagePanel.mouseClickedX < 0 && imagePanel.mouseClickedY < 0) {
        val center = getZoomedImageCenter
        imagePanel.mouseClickedX = center.x
        imagePanel.mouseClickedY = center.y
        center
      } else {
        new Point(imagePanel.mouseClickedX, imagePanel.mouseClickedY)
      }
    }

    val newImageSize: ImageSize = {
      val size = ImageFetcher.calculateImageSize(zoomLevel, maxZoomLevel, currentPageImageSize.width, currentPageImageSize.height)
      if (imagePanel.scaleRatio == 1) {
        size
      } else {
        new ImageSize((size.width * imagePanel.scaleRatio).toInt, (size.height * imagePanel.scaleRatio).toInt)
      }
    }

    val topLeftPoint = imagePanel.getTopLeft(zoomedImageCenter, newImageSize)
    val imageTopLeftX = topLeftPoint.x
    val imageTopLeftY = topLeftPoint.y
    val imageUrl = ImageFetcher.getSubImageUrl(ViewerProperties.baseUrl, tile2SourceMap(imagePanel.pageToShow))
    val subImageDescriptor = ImageFetcher.fetchImageDescriptor(ViewerProperties.baseUrl, tile2SourceMap(imagePanel.pageToShow))
    val xMax = {
      val newX = imageTopLeftX + imagePanel.size.width
      if (newX > newImageSize.width) {
        newImageSize.width
      } else {
        newX
      }
    }
    val yMax = {
      val newY = imageTopLeftY + imagePanel.size.height
      if (newY > newImageSize.height) {
        newImageSize.height
      } else {
        newY
      }
    }
    if (imagePanel.scaleRatio == 1) {
      // TODO too many arguments for zoomedTiles
      val zoomedTiles = ImageFetcher.calculateVisibleImageGridSize(zoomLevel, imageTopLeftX, imageTopLeftY, xMax, yMax, MultiScaleImageFileParser.getTileSize(descriptor), MultiScaleImageFileParser.getImageOverlapSize(subImageDescriptor), imageUrl, MultiScaleImageFileParser.getFormat(descriptor))
//      zoomedTiles.foreach(tile => Logger.instance.log("col=" + tile.column + ", row=" + tile.row + ", pos=(" + tile.position.x + ", " + tile.position.y + ")"))
      val t = zoomedTiles(0)
      createThumbnail(imagePanel.pageToShow, new Tile(t.uriSource, t.thumbnailUri, t.fileFormat, new liang.don.dzviewer.tile.Point(0, 0), t.overlapSize, 0, 0, t.tileSize))
      create(imagePanel.pageToShow, zoomedTiles)
    } else {
      imagePanel.adjustOffset(imageTopLeftX / imagePanel.zoomInFactorCallback(), imageTopLeftY / imagePanel.zoomInFactorCallback(), imagePanel.pageToShow)
    }
    imagePanel.zoomToShow = zoomLevel
    imagePanel.repaint()
  }

  private def getZoomedImageCenter: Point = {
    val zoomedSize = ImageFetcher.calculateZoomLevel(0, currentPageImageSize.width, currentPageImageSize.height, DeepZoomViewerMain.PREFERRED_WINDOW_WIDTH, DeepZoomViewerMain.PREFERRED_WINDOW_HEIGHT)
    val size = ImageFetcher.calculateImageSize(zoomedSize.level, maxZoomLevel, currentPageImageSize.width, currentPageImageSize.height)
    new Point(size.width, size.height)
  }

  override def cacheThumbnail(page: Int, tile: ImageTile) {
    imagePanelLock.synchronized {
      val fileUuid = getFileHash(ImageFetcher.getSubImageUrl(ViewerProperties.baseUrl, tile2SourceMap(page)).toString)
      diskCache.update(page, ViewerProperties.thumbnailLevel, fileUuid, Array(tile))
    }
  }

  override def cacheTiles(page: Int, tiles: Array[ImageTile]) {
    imagePanelLock.synchronized {
      val baseUrl = ViewerProperties.baseUrl
      Logger.instance.log("[" + getClass.getName + "#cacheTiles] baseUrl: " + baseUrl, LogLevel.Debug)

      val fileUuid = getFileHash(ImageFetcher.getSubImageUrl(ViewerProperties.baseUrl, tile2SourceMap(page)).toString)
      Logger.instance.log("[" + getClass.getName + "#cacheTiles] Caching pg [ " + page + " ] zoommLevel: " + page2ZoomLevelArray(page) + " tiles of  " + fileUuid + "...", LogLevel.Debug)
      diskCache.update(page, page2ZoomLevelArray(page), fileUuid, tiles)
      imagePanel.setTiles(page, page2ZoomLevelArray(page), (() => diskCache.get(page, page2ZoomLevelArray(page), fileUuid)))
      imagePanel.repaint()
    }
  }

  override def isTileCached(page: Int, tile: Tile): Boolean = {
    imagePanelLock.synchronized {
      imagePanel.isTileLoaded(page, page2ZoomLevelArray(page), tile)
    }
  }

  override def isThumbnailCached(page: Int): Boolean = {
    imagePanelLock.synchronized {
      imagePanel.isThumbnailLoaded(page)
    }
  }

  override def isTilesCached(page: Int): Boolean = {
    imagePanelLock.synchronized {
      imagePanel.isTilesLoaded(page, page2ZoomLevelArray(page))
    }
  }

  override def loadCache() {
    val baseUrl = ViewerProperties.baseUrl
    Logger.instance.log("Loading cache for baseUrl: " + baseUrl)

    for (page <- 0 until totalPages) {
      // Load for all levels from optimal size upwards, as well as the first thumbnail (max 256x256 tile image)
      val fileUuid = getFileHash(ImageFetcher.getSubImageUrl(ViewerProperties.baseUrl, tile2SourceMap(page)).toString)
      val zoomLevel = getOptimalZoomLevel(page)
      page2ZoomLevelArray(page) = zoomLevel
      imagePanel.setTiles(page, ViewerProperties.thumbnailLevel, (() => diskCache.get(page, ViewerProperties.thumbnailLevel, fileUuid)))
      for (level <- zoomLevel  to maxZoomLevel by 1) {
        imagePanel.setTiles(page, level, (() => diskCache.get(page, level, fileUuid)))
      }
    }
  }

  override def setCurrentPage(currentPage: Int) {
    imagePanel.pageToShow = currentPage
    imagePanel.zoomToShow = page2ZoomLevelArray(currentPage)
    imagePanel.optimalImageSize = ImageFetcher.calculateZoomLevel(0, currentPageImageSize.width, currentPageImageSize.height, DeepZoomViewerMain.PREFERRED_WINDOW_WIDTH, DeepZoomViewerMain.PREFERRED_WINDOW_HEIGHT).imageSize
  }

  override def show(): Frame = {
    val prevButton = new Button {
      text = "Prev"
      reactions += {
        case ButtonClicked(_) => {
          spawn {
            setCurrentPage(imagePanel.pageToShow - 1)
            updatePageButtonState()
            updateZoomButtonState()
          }
        }
      }
    }

    val nextButton = new Button {
      text = "Next"
      reactions += {
        case ButtonClicked(_) => {
          spawn {
            setCurrentPage(imagePanel.pageToShow + 1)
            updatePageButtonState()
            updateZoomButtonState()
          }
        }
      }
    }

    val zoomInButton = new Button {
      text = "Zoom In"
    }

    val zoomOutButton = new Button {
      text = "Zoom Out"
    }

    updatePageButtonState = () => {
      toggleButtonEnabled(prevButton, (imagePanel.pageToShow > 0))
      toggleButtonEnabled(nextButton, (imagePanel.pageToShow < totalPages - 1))

      updatePage()
    }

    updateZoomButtonState = () => {
      toggleButtonEnabled(zoomInButton, canIncreaseZoom)
      toggleButtonEnabled(zoomOutButton, canDecreaseZoom)
    }

    zoomInButton.reactions += {
      case ButtonClicked(source) => {
        zoomLevel += 1
        val zoomSize = getZoomedImageCenter
        zoomLevel -= 1
        if (imagePanel.mouseClickedX < 0 && imagePanel.mouseClickedY < 0) {
          imagePanel.mouseClickedX = zoomSize.x
          imagePanel.mouseClickedY = zoomSize.y
        } else {
          imagePanel.mouseClickedX += zoomSize.x
          imagePanel.mouseClickedY += zoomSize.y
        }
        zoomInEvent()
      }
    }

    zoomOutButton.reactions += {
      case ButtonClicked(_) => {
        zoomOutEvent()
      }
    }

    imagePanel.zoomInFactorCallback = math.pow(2, (zoomLevel + 1) - defaultZoomLevel).toInt
    imagePanel.zoomInEventCallback = zoomInEvent
    imagePanel.zoomOutEventCallback = zoomOutEvent

    lazy val ui = new GridBagPanel {
      background = Color.black
      val imagePanelConstraints = new Constraints
      imagePanelConstraints.gridx = 0
      imagePanelConstraints.gridy = 0

      val buttonControlConstraints = new Constraints
      buttonControlConstraints.gridx = 0
      buttonControlConstraints.gridy = 1
      buttonControlConstraints.fill = Fill.Horizontal

      layout(imagePanel) = imagePanelConstraints

      val buttonControl = new GridPanel(2, 2) {
        hGap = 3
        vGap = 3
        contents += prevButton
        contents += nextButton
        contents += zoomInButton
        contents += zoomOutButton
      }

      layout(buttonControl) = buttonControlConstraints
    }

    displayWindow = new MainFrame{
      title = "DeepZoomImageViewer"
      contents = ui
    }

    updatePageButtonState()
    updateZoomButtonState()

    displayWindow
  }


}
