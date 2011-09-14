package liang.don.dzviewer.viewer.net

import liang.don.dzviewer.viewer.DeepZoomViewer
import liang.don.dzviewer.tile.{ImageTile, Tile}

/**
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
trait NetThreadedViewer extends DeepZoomViewer {
// TODO - Override/impl methods that require the .NET threading APIs

  def create(pageNumber: Int, tiles: Array[Tile]) {
    // TOOD
  }

  private def createImage(pageNumber: Int, tile: Tile): ImageTile = {
    // TODO
    null
  }
}
