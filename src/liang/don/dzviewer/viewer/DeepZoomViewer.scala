package liang.don.dzviewer.viewer

import swing.Frame
import liang.don.dzviewer.tile.{ImageTile, Tile}

/**
 * Viewer for DeepZoom images.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
abstract class DeepZoomViewer() {

  /**
   * Cache and store the downloaded thumbnail tile so it can be displayed.
   *
   * @param page The page to cache.
   * @param tile The thumbnail tile to cache.
   */
  def cacheThumbnail(page: Int, tile: ImageTile)

  /**
   * Cache and store the downloaded tiles so it can be displayed.
   *
   * @param page The page to cache.
   * @param tiles The image tile(s) to cache.
   */
  def cacheTiles(page: Int, tiles: Array[ImageTile])

  /**
   * Manages the download and reconstruction of image tiles from the specified page.
   *
   * @param pageNumber The page of the image tile(s) to create and download.
   * @param tiles The array of tile information specifying what image tile(s)
   *               to create and download.
   */
  def create(pageNumber: Int, tiles: Array[Tile])

  /**
   * Downloads the thumbnail tile (max 256x256) of the specified page.
   *
   * @param pageNumber The page of the thumbnail to create and download.
   * @param tile Information regarding what tile to create and download.
   */
  def createThumbnail(pageNumber: Int, tile: Tile)

  /**
   * If the thumbnail tile (max 256x256) of the specified page is cached,
   * return true, else return false.
   *
   * @param page The page to check.
   *
   * @return True if the thumbnail tile of the specified page is cached, else false.
   */
  def isThumbnailCached(page: Int): Boolean

  /**
   * If the image tile of the specified page is cached,
   * return true, else return false.
   *
   * @param page The page to check.
   * @param tile The tile information to check against.
   *
   * @return True if the image tile of the specified page is cached, else false.
   */
  def isTileCached(page: Int, tile: Tile): Boolean

  /**
   * If all relevant image tiles of the specified page is cached,
   * return true, else return false.
   *
   * @param page The page to check.
   *
   * @return True if the specified image tile(s) are cached, else false.
   */
  def isTilesCached(page: Int): Boolean

  /**
   * Loads the image cache.
   */
  def loadCache()

  /**
   * Sets the current page to be shown by the Deep Zoom Viewer.
   *
   * @param currentPage The page value to set as the current page.
   */
  def setCurrentPage(currentPage: Int)

  /**
   * Constructs and returns the UI control that shows the image(s).
   *
   * @return The DeepZoomViewer UI.
   */
  def show(): Frame

}
