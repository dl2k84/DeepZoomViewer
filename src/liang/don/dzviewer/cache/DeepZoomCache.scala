package liang.don.dzviewer.cache

import liang.don.dzviewer.tile.ImageTile

/**
 * Basic cache with get and update (set) operations.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
abstract class DeepZoomCache {

  protected val baseFolder = "cache"

  /**
   * Get the requested image tile(s) info from a cache.<br>
   * Returns null if it does not exist in the cache.<br>
   *
   * @param page The page to get.
   * @param zoomLevel The zoom level to get.
   * @param fileUuid The hashed value of the image tile(s) to get.
   *
   * @return The requested image tile(s) as an array.
   */
  def get(page: Int, zoomLevel: Int, fileUuid: String): Array[ImageTile]

  /**
   * Updates (and if required, create) the cache entry for the specified image tile(s).
   *
   * @param page The page of the image tile(s) to update or create.
   * @param zoomLevel The zoom level for the image tile(s) to update or create.
   * @param tiles The image tile(s) to update or create.
   */
  def update(page: Int, zoomLevel: Int, fileUuid: String, tiles: Array[ImageTile])
}
