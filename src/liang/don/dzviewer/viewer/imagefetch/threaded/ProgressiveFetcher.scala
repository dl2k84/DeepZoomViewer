package liang.don.dzviewer.viewer.imagefetch.threaded

import concurrent.ops._
import collection.mutable.HashMap
import xml.Node
import liang.don.dzviewer.ImageFetcher
import liang.don.dzviewer.viewer.imagefetch.TileFetcher
import liang.don.dzviewer.tile.{Tile, Point}
import liang.don.dzviewer.log.Logger

/**
 * Deep Zoom image fetch algorithm that takes an initial page value<br>
 * and continues to fetch images in the background for pages around<br>
 * the initial page "epicenter".<br>
 * <br>
 * E.g. Initial page value = 10<br>
 * After page 10 images are fetched and loaded, this algorithm<br>
 * will then go on to fetch pages less than 10 until 0 (or 1 depending on your style),<br>
 * as well as fetch pages greater than 10 until the last page simultaneously.<br>
 * <br>
 * This algorithm is good for when the user will go to the "previous" and "next"<br>
 * pages from the initial page sequentially as chances are they are those<br>
 * pages are already loaded and cached in the background, thus the page images<br>
 * will seem to load instantaneously, reducing the amount of loading "lag" seen.<br>
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
@deprecated("This implementation using the java thread model is no longer supported", "0.0.1")
trait ProgressiveFetcher extends TileFetcher {
  override def fetch() {
    if (leftPageCount > 0) {
     fetchLeft(leftPageCount)
    }
    if (rightPageCount > 0) {
      fetchRight(rightPageCount)
    }
  }

  private def fetchLeft(pageCount: Int) {
      val lock = new Object
      val page2ImageDescriptorMap = new HashMap[Int, Node]
      spawn {
        for (page <- pageCount - 1 to 0 by -1) {
          val source = tile2SourceMap(page)
          val subImageDescriptor = ImageFetcher.fetchImageDescriptor(baseUri, source)
          lock.synchronized {
            page2ImageDescriptorMap.put(page, subImageDescriptor)
            lock.notify()
          }
        }
      }

      spawn {
        for (page <- pageCount - 1 to 0 by -1) {
          if (!viewer.isTilesCached(page)) {
            lock.synchronized {
              while (!page2ImageDescriptorMap.contains(page)) {
                lock.wait()
              }
            }
            val pageTiles = ImageFetcher.generatePageTiles(ImageFetcher.getSubImageUrl(baseUri, tile2SourceMap(page)), page2ImageDescriptorMap(page), maxSupportedLevels)
            Logger.instance.log(getClass.getName + "#fetch] Getting tiles for page [ " + page + " ]")
            val t = pageTiles(0)
            viewer.createThumbnail(page, new Tile(t.uriSource, t.thumbnailUri, t.fileFormat, new Point(0, 0), t.overlapSize, 0, 0, t.tileSize))
            viewer.create(page, pageTiles)
          }
        }
      }
  }

  private def fetchRight(pageCount: Int) {
    val lock = new Object
    val page2ImageDescriptorMap = new HashMap[Int, Node]
    spawn {
      for (page <- pageToView + 1 to pageToView + pageCount) {
        val source = tile2SourceMap(page)
        val subImageDescriptor = ImageFetcher.fetchImageDescriptor(baseUri, source)
        lock.synchronized {
          page2ImageDescriptorMap.put(page, subImageDescriptor)
          lock.notify()
        }
      }
    }

    spawn {
      for (page <- pageToView + 1 to pageToView + pageCount) {
        if (!viewer.isTilesCached(page)) {
          lock.synchronized {
            while (!page2ImageDescriptorMap.contains(page)) {
              lock.wait()
            }
          }
          val pageTiles = ImageFetcher.generatePageTiles(ImageFetcher.getSubImageUrl(baseUri, tile2SourceMap(page)), page2ImageDescriptorMap(page), maxSupportedLevels)
          Logger.instance.log(getClass.getName + "#fetch] Getting tiles for page [ " + page + " ]")
          val t = pageTiles(0)
          viewer.createThumbnail(page, new Tile(t.uriSource, t.thumbnailUri, t.fileFormat, new Point(0, 0), t.overlapSize, 0, 0, t.tileSize))
          viewer.create(page, pageTiles)
        }
      }
    }
  }
}
