package liang.don.dzviewer.viewer.imagefetch

import liang.don.dzviewer.viewer.DeepZoomViewer

/**
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
abstract class TileFetcher(val viewer: DeepZoomViewer, val tile2SourceMap: Map[Int, String], val baseUri: String, val pageToView: Int, val leftPageCount: Int, val rightPageCount: Int, val maxSupportedLevels: Int) {

  /**
   * Fetch the required image tile(s).
   */
  def fetch()

}
