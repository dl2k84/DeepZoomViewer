package liang.don.dzviewer.viewer.imagefetch.actor

import liang.don.dzviewer.viewer.imagefetch.TileFetcher
import collection.mutable.HashMap
import xml.Node
import liang.don.dzviewer.ImageFetcher
import actors.Actor
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
trait ProgressiveFetcherActor extends TileFetcher {
// TODO
  private var leftDescriptorFetcher: DescriptorFetchActor = null
  private var rightDescriptorFetcher: DescriptorFetchActor = null
  private var imageCreator: ImageCreateActor = null
  private val page2ImageDescriptorMap = new HashMap[Int, Node]

  override def fetch() {
    if (leftDescriptorFetcher == null) {
      leftDescriptorFetcher = new DescriptorFetchActor
      leftDescriptorFetcher.start()
    }
    if (rightDescriptorFetcher == null) {
      rightDescriptorFetcher = new DescriptorFetchActor
      rightDescriptorFetcher.start()
    }
    if (imageCreator == null) {
      imageCreator = new ImageCreateActor
      imageCreator.start()
    }

    if (leftPageCount > 0) {
     fetchLeft(leftPageCount)
    }
    if (rightPageCount > 0) {
      fetchRight(rightPageCount)
    }
  }

  private def fetchLeft(pageCount: Int) {
    leftDescriptorFetcher ! FetchDescriptor({() => {
      for (page <- pageCount - 1 to 0 by -1) {
        if (!viewer.isTilesCached(page)) {
          val source = tile2SourceMap(page)
          val subImageDescriptor = ImageFetcher.fetchImageDescriptor(baseUri, source)
          page2ImageDescriptorMap.put(page, subImageDescriptor)
          imageCreator ! Create(page)
        }
      }
    }})
  }

  private def fetchRight(pageCount: Int) {
    rightDescriptorFetcher ! FetchDescriptor({() => {
      for (page <- pageToView + 1 to pageToView + pageCount) {
        if (!viewer.isTilesCached(page)) {
          val source = tile2SourceMap(page)
          val subImageDescriptor = ImageFetcher.fetchImageDescriptor(baseUri, source)
          page2ImageDescriptorMap.put(page, subImageDescriptor)
          imageCreator ! Create(page)
        }
      }
    }})
  }

  private case class FetchDescriptor(toFetch: () => Unit)
  private class DescriptorFetchActor extends Actor {
    override def act() {
      loop {
        react {
          case FetchDescriptor(toFetch) =>  {
            // Do stuff
            toFetch()
            if (page2ImageDescriptorMap.size >= (leftPageCount + rightPageCount + 1)) {
              // TODO reassess how to impl this properly
              Logger.instance.log(getClass.getName + " EXITING")
              exit()
            }
          }
        }
      }
    }
  }

  private case class Create(page: Int)
  private class ImageCreateActor extends Actor {
    override def act() {
      var count = 0
      loop {
        react {
          case Create(page) => {
            if (!viewer.isTilesCached(page)) {
              val pageTiles = ImageFetcher.generatePageTiles(ImageFetcher.getSubImageUrl(baseUri, tile2SourceMap(page)), page2ImageDescriptorMap(page), maxSupportedLevels)
              Logger.instance.log(getClass.getName + "#Create(page)] Getting tiles for page [ " + page + " ]")
              val t = pageTiles(0)
              viewer.createThumbnail(page, new Tile(t.uriSource, t.thumbnailUri, t.fileFormat, new Point(0, 0), t.overlapSize, 0, 0, t.tileSize))
              viewer.create(page, pageTiles)
            } else {
              Logger.instance.log(getClass.getName + "Actor#Create] tiles are cached. Not creating new.")
            }

            count += 1
            if (count >= (leftPageCount + rightPageCount + 1)) {
              Logger.instance.log(getClass.getName + " EXITING")
              exit()
            }
          }
        }
      }
    }
  }
}
