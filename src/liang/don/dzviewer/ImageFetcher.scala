package liang.don.dzviewer

import java.net.URL
import collection.mutable.HashMap
import parser.MultiScaleImageFileParser
import tile._
import xml.Node
import math._

/**
 * Creates and fetches the required image tile.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
object ImageFetcher {

  /**
   * Retrieves the sub image XML descriptor from the specified base URL
   * and relative or absolute image collection path.
   *
   * @param baseUri The URL for the base descriptor.
   * @param tilePath The relative or absolute path specified in the
   *                  image collection element of the base descriptor
   *                  of where to find the sub image.
   *
   * @return The sub image's XML descriptor.
   */
  def fetchImageDescriptor(baseUri: String, tilePath: String): Node = {
    new HttpRequest(getSubImageUrl(baseUri, tilePath)).getXmlDescriptor
  }

  /**
   * Retrieve a collection of sub image XML descriptor from the specified
   * base URL and relative or absolute image collection path.
   *
   * @param baseUri The URL for the base descriptor.
   * @param tilePath A collection of relative or absolute paths specified
   *                  from the image collection element of the base descriptor
   *                  of where to find the sub image.
   *
   * @return A collection of sub image XML descriptors.
   */
  def fetchImageDescriptors(baseUri: String, tileUrls: Map[Int, String]): Map[Int, Node] = {
    val page2ImageDescriptorMap = new HashMap[Int, Node]
    tileUrls.foreach(mapItem => page2ImageDescriptorMap.put(mapItem._1, fetchImageDescriptor(baseUri, mapItem._2)))

    page2ImageDescriptorMap.toMap // TODO bit of a memory waste.... improve upon later (by nulling it/making it immediately GC-able)
  }

  /**
   * Constructs the URL where the image tile exists.
   *
   * @param descriptorUrl The URL of the XML descriptor.
   * @param level The zoom level.
   * @param col The column value of the image tile.
   * @param row The row value of the image tile.
   * @param fileFormat The image format (file extension).
   *
   * @return The URL of the image tile.
   */
  def createImageURL(descriptorUrl: URL, level: Int, col: Int, row: Int, fileFormat: String): URL = {
    val uriBuilder = new StringBuffer
    val descriptorUrlString = descriptorUrl.toString
    uriBuilder.append(descriptorUrlString.substring(0, descriptorUrlString.lastIndexOf(".xml")))
    	.append("_files").append("/").append(level).append("/")
    	.append(col).append("_").append(row).append(".").append(fileFormat)

    new URL(uriBuilder.toString)
  }

  /**
   * Constructs the URL where the sub image XML descriptor exists.
   *
   * @param baseUri The URL of the base XML descriptor.
   * @param sourceUri The relative or absolute path where the
   *                   sub image descriptor exists.
   *
   * @return The URL of the sub image descriptor.
   */
  def getSubImageUrl(baseUri: String, sourceUri: String): URL = {
    if (!sourceUri.startsWith("http") && !sourceUri.startsWith("https")) { // TODO improve check for file:/// etc
      // sourceUri is a relative URL
      new URL(new URL(baseUri), sourceUri)
    } else {
      // sourceUri is an absolute URL
      new URL(sourceUri)
    }
  }

  /**
   * Generates image tile(s) information.
   *
   * @param imageUri The URL of the image.
   * @param descriptor The XML descriptor of the image or sub image.
   * @param maxSupportedLevels The maximum generated level of zoom.
   *
   * @return Image tile(s) information as an array.
   */
  def generatePageTiles(imageUrl: URL, descriptor: Node, maxSupportedLevels: Int): Array[Tile] = {
    val fileFormat = MultiScaleImageFileParser.getFormat(descriptor)
    val overlapSize = MultiScaleImageFileParser.getImageOverlapSize(descriptor)
    val tileSize = MultiScaleImageFileParser.getTileSize(descriptor)

    val imageSize = MultiScaleImageFileParser.getImageSize(descriptor)
    val imageWidth = imageSize.width
    val imageHeight = imageSize.height
    println("Original image size: " + imageWidth + "x" + imageHeight)

    val optimalZoom = calculateZoomLevel(maxSupportedLevels, imageWidth, imageHeight, DeepZoomViewerMain.PREFERRED_WINDOW_WIDTH, DeepZoomViewerMain.PREFERRED_WINDOW_HEIGHT)
    // TODO Downscales window to optimal screen size - Called multiple times for documents with multiple pages - fix it.
    DeepZoomViewerMain.VISIBLE_WINDOW_WIDTH = optimalZoom.imageSize.width
    DeepZoomViewerMain.VISIBLE_WINDOW_HEIGHT = optimalZoom.imageSize.height

    println("Optimal zoom dimension set to: " + optimalZoom.imageSize.width + "x" + optimalZoom.imageSize.height + " (Level " + optimalZoom.level + ")")

    // TODO if-check if image fits in default zoom (where no downscaling is necessary)
    val imageGridSize = calculateImageGridSize(optimalZoom, imageWidth, imageHeight, tileSize)
    val rowCount = imageGridSize.rows
    val colCount = imageGridSize.cols

    //    println("Grid size: " + colCount + " (col) x " + rowCount + " (rows).")

    val tileArray = new Array[Tile](rowCount * colCount)

    // TODO find the scala-style of nested for-loops...
    var tileCount = 0
    for (row <- 0 until rowCount) {
      for (col <- 0 until colCount) {
        val tileUrl = ImageFetcher.createImageURL(imageUrl, optimalZoom.level, col, row, fileFormat)
        tileArray(tileCount) = new Tile(tileUrl.toString, fileFormat, calculateTileStartOffset(col, row, tileSize, overlapSize), overlapSize, col, row, tileSize)
        tileCount += 1
      }
    }

    tileArray
  }

  /**
   * Calculates the maximum possible zoom.
   *
   * @param width Image width.
   * @param height Image height.
   */
  def calculateMaximumZoomLevel(width: Int, height: Int): Int = {
    ceil(log(max(width, height)) / log(2)).toInt
  }

  /**
   * Calculates the optimal visible zoom level.
   *
   * @param maxSupportedLevels The maximum generated level of zoom.
   * @param imageWidth Image width.
   * @param imageHeight Image height.
   * @param preferredWidth Visible width.
   * @param preferredHeight Visible height.
   *
   * @return Zoom level value of image that will fit within the visible display size.
   */
  def calculateZoomLevel(maxSupportedLevels:Int, imageWidth: Int, imageHeight: Int, preferredWidth: Int, preferredHeight: Int): ZoomLevel = {
    // Returns the optimal viewing zoom level based on passed window width and height.
    var w = imageWidth
    var h = imageHeight
    val maxLevels = calculateMaximumZoomLevel(imageWidth, imageHeight)

    // Only divide down to the level set by maxSupportedLevels. Any levels lower than maxLevels - maxSupportedLevels
    // i.e,, lower resolution will not be considered.
    for (level <- maxLevels - 1 until maxSupportedLevels by -1) {
      w = ceil(w / (2).toDouble).toInt
      h = ceil(h / (2).toDouble).toInt

      // TODO investigate scala style equalization instead of if (x && y) style below...
      if (w < preferredWidth && h < preferredHeight) {
        println("Returning optimal zoom level (inside FOR): level=" + level + " w=" + w + ", h=" + h)
        return new ZoomLevel(level, new ImageSize(w, h))
      }
    }

    // return lowest zoom level (ideally should not return a 1x1 level as "optimal")
    println("Returning optimal zoom level (outside FOR): level=" + 0 + " w=" + 1 + ", h=" + 1)
    new ZoomLevel(0, new ImageSize(1, 1))
  }

  /**
   * Calculates the required grid size to fill with image tile(s) in order to display the DeepZoom image.<br>
   * The calculateVisibleImageGridSize method should be used instead when calculating an area of a zoomed image,
   * instead of the whole image.<br>
   *
   *
   * @param zoomLevel The zoom level to draw.
   * @param imageWidth Image width.
   * @param imageHeight Image height.
   * @param tileSize The length (width, height) of the image tile.
   *
   * @return The side of the image grid to draw image tiles into.
   */
  def calculateImageGridSize(zoomLevel: ZoomLevel, imageWidth: Int, imageHeight: Int, tileSize: Int): ImageGridSize = {
    var w = imageWidth
    var h = imageHeight
    val maxLevels = calculateMaximumZoomLevel(imageWidth, imageHeight)

    // start at maxLevels - 1 as it's assumed this method is called when not viewed at max resolution (highest level)
    for (level <- maxLevels - 1 to zoomLevel.level by -1) {
      w = ceil(w / (2).toDouble).toInt
      h = ceil(h / (2).toDouble).toInt
    }

    new ImageGridSize(ceil(w / tileSize.toDouble).toInt, ceil(h / tileSize.toDouble).toInt)
  }

  /**
   * Calculates the top-left (x,y) coordinate where the tile will start drawing graphics.
   *
   * @param col The column value in the image grid.
   * @param row The row value in the image grid.
   * @param tileSize The length (width, height) of the tile.
   * @param overlapSize The amount of pixel overlap (if any) with other neighbouring tiles.
   *
   * @return The top-left (x, y) coordinate of the image tile.
   */
  def calculateTileStartOffset(col: Int, row: Int, tileSize: Int, overlapSize: Int): Point = {
    val xOverlapOffset: Int = if (0 < col) overlapSize else 0
    val yOverlapOffset: Int = if (0 < row) overlapSize else 0

    new Point((col * tileSize) - xOverlapOffset, (row * tileSize) - yOverlapOffset)
  }

  /**
   * Calculates the 0-index origin position of a tile in an image grid.
   *
   * @param i The x or y coordinate.
   * @param tileSize Tile size.
   *
   * @return The column or row position.
   */
  def calculateGridNumber(i:Int, tileSize: Int): Int = {
    ceil(i / tileSize.toDouble).toInt - 1
  }

  /**
   * Calculates the image dimension for the specified zoom level.
   *
   * @param zoomLevel The zoom level.
   * @param originalWidth The width of the original image at maximum zoom.
   * @param originalHeight The height of the original image at maximum zoom.
   *
   * @return The image dimension.
   */
  def calculateImageSize(zoomLevel: Int, originalWidth: Int, originalHeight: Int): ImageSize = {
    calculateImageSize(zoomLevel, calculateMaximumZoomLevel(originalWidth, originalHeight), originalWidth, originalHeight)
  }

  /**
   * Calculates the image dimension for the specified zoom level.
   *
   * @param zoomLevel The zoom level.
   * @param maxZoomLevel The maximum possible zoom level for the image.
   * @param originalWidth The width of the original image at maximum zoom.
   * @param originalHeight The height of the original image at maximum zoom.
   *
   * @return The image dimension.
   */
  def calculateImageSize(zoomLevel: Int, maxZoomLevel: Int, originalWidth: Int, originalHeight: Int): ImageSize = {
    var w = originalWidth
    var h = originalHeight
    for (zoomLevel <- maxZoomLevel until zoomLevel by -1) {
      w = ceil(w / (2).toDouble).toInt
      h = ceil(h / (2).toDouble).toInt
    }
    new ImageSize(w, h)
  }

  /**
   * Calculates the required grid size to fill with image tile(s) in order to display the DeepZoom image.<br>
   * This calculateImageGridSize method should be used instead when calculating the area for a whole image.<br>
   *
   * @param zoomLevel The zoom level to draw.
   * @param topLeftX The top left x-coordinate where drawing starts.
   * @param topLeftY The top left y-coordinate where drawing starts.
   * @param xMax The x-coordinate where drawing ends.
   * @param yMax The y-coordinate where drawing ends.
   * @param tileSize The length (width, height) of the image tile.
   * @param overlapSize The size of overlap (if any) with neighbouring tiles.
   * @param imageUri The URL of the image XML descriptor.
   * @param fileFormat The image format.
   *
   * @return Image tile(s) information as an array.
   */
  def calculateVisibleImageGridSize(zoomLevel: Int, topLeftX: Int, topLeftY: Int, xMax: Int, yMax: Int, tileSize: Int, overlapSize: Int, imageUrl: URL, fileFormat: String): Array[Tile] = {
    println(getClass.getName + "#calculateVisibleImageGridSize] topLeftX: " + topLeftX + ", topLeftY: " + topLeftY + ", xMax: "  + xMax + " , yMax: " + yMax)
    val endCol = calculateGridNumber(xMax, tileSize)
    val xStart = {
      if (topLeftX <= 0) {
        1
      } else {
        topLeftX
      }
    }
    val startCol = calculateGridNumber(xStart, tileSize)

    val endRow = calculateGridNumber(yMax, tileSize)
    val yStart = {
      if (topLeftY <= 0) {
        1
      } else {
        topLeftY
      }
    }
    val startRow = calculateGridNumber(yStart, tileSize)

    val colCount = endCol - startCol + 1
    val rowCount = endRow - startRow + 1

    val tileArray = new Array[Tile](rowCount * colCount)
    var tileCount = 0
    for (row <- startRow to endRow) {
      for (col <- startCol to endCol) {
        val tileUrl = ImageFetcher.createImageURL(imageUrl, zoomLevel, col, row, fileFormat)
        val tileOffset = calculateTileStartOffset(col, row, tileSize, overlapSize)
        tileArray(tileCount) = new Tile(tileUrl.toString, fileFormat, tileOffset, overlapSize, col, row, tileSize)
        tileCount += 1
      }
    }

    tileArray
  }
}
