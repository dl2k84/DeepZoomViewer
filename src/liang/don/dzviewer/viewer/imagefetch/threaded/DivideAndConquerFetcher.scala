package liang.don.dzviewer.viewer.imagefetch.threaded

import concurrent.ops._
import collection.mutable.{Queue, HashMap}
import math._
import xml.Node
import liang.don.dzviewer.config.ViewerProperties
import liang.don.dzviewer.ImageFetcher
import liang.don.dzviewer.viewer.imagefetch.TileFetcher
import liang.don.dzviewer.tile.{Point, Tile}
import liang.don.dzviewer.log.Logger

/**
 * Deep Zoom image fetch algorithm that takes an initial page value<br>
 * and continues to fetch images in the background for pages based<br>
 * on a divide-and-conquer style behavior by spawning threads that<br>
 * will fetch an arbitrary set of image tiles.<br>
 * <br>
 * E.g. Initial page value = 10, total pages = 20, max work units per thread = 5<br>
 * After page 10 images are fetched and loaded, this algorithm<br>
 * will then go on to retrieve other pages with a divide-and-conquer style.<br>
 * 4 threads will be spawned due to max work units per thread = 5<br>
 * i.e. pages yet to load = ceiling((total pages - 1) / max work units per thread)<br>
 *                        =(math.ceil(20 - 1)/5) = 4 threads<br><br>
 * Here's how the work units (pages) will be divided:<br>
 * Thread-1: { 9, 7, 5, 3, 1 }<br>
 * Thread-2: { 8, 6, 4, 2, 0 } // Depending on impl style and interpretation, remove 0 if the page count starts at 1<br>
 * Thread-3: { 11, 13, 15, 17, 19 }<br>
 * Thread-4: { 12, 14, 16, 18, 20 } // Similar to Thread-2,remove 20 if page count starts at 0<br>
 * <br>
 * This algorithm is good for when the user will jump between pages instead<br>
 * of sequentially scrolling through "previous" and "next" pages.<br>
 * <br>
 * The downside is that it will spawn more threads than other algorithms.<br>
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
@deprecated("This implementation using the java thread model is no longer supported", "0.0.1")
trait DivideAndConquerFetcher extends TileFetcher {
  override def fetch() {
    if (leftPageCount > 0) {
      fetchLeft(leftPageCount)
    }
    if (rightPageCount > 0) {
      fetchRight(rightPageCount)
    }
  }

  def fetchLeft(pageCount: Int) {
      val threadNumber2WorkUnitsMap = new HashMap[Int, Queue[Int]]
      val maxThreads = (ceil(pageCount / ViewerProperties.maxWorkUnitsPerThread.toDouble)).toInt
      for (i <- 0 until maxThreads) {
        var count = pageCount - i - 1
        val q = new Queue[Int]
        while (-1 < count) {
          q += count
          count -= maxThreads
        }
        threadNumber2WorkUnitsMap.put(i, q)
      }

      val lock = new Object
      val page2ImageDescriptorMap = new HashMap[Int, Node]
      for (threadNumber <- 0 until maxThreads) {
        spawn {
          val workQueue = new Queue[Int]
          spawn {
            var hasMore = true
            while (hasMore) {
              lock.synchronized {
                while (workQueue.size == 0) {
                  lock.wait()
                }

                val workItem = workQueue.dequeue()
                if (!viewer.isTilesCached(workItem)) {
                  if (workItem > -1) {
                    val pageTiles = ImageFetcher.generatePageTiles(ImageFetcher.getSubImageUrl(baseUri, tile2SourceMap(workItem)), page2ImageDescriptorMap(workItem), maxSupportedLevels)
                    val t = pageTiles(0)
                    viewer.createThumbnail(workItem, new Tile(t.uriSource, t.thumbnailUri, t.fileFormat, new Point(0, 0), t.overlapSize, 0, 0, t.tileSize))
                    viewer.create(workItem, pageTiles)
                  } else {
                    hasMore = false
                    Logger.instance.log(getClass.getName + "#fetch] Received end-of-queue signal. Exiting thread...")
                  }
                }
              }
            }
          }
          val thisQ = threadNumber2WorkUnitsMap(threadNumber)
          while (thisQ.size > 0) {
            val page = thisQ.dequeue()
            val source = tile2SourceMap(page)
            val subImageDescriptor = ImageFetcher.fetchImageDescriptor(baseUri, source)
            lock.synchronized {
              page2ImageDescriptorMap.put(page, subImageDescriptor)
              workQueue += page
              lock.notify()
            }
          }
          lock.synchronized {
            workQueue += -1
            lock.notify()
          }
        }
      }
  }

  def fetchRight(pageCount: Int) {
    val threadNumber2WorkUnitsMap = new HashMap[Int, Queue[Int]]
      val maxThreads = (ceil(pageCount / ViewerProperties.maxWorkUnitsPerThread.toDouble)).toInt
      for (i <- 0 until maxThreads) {
        var count = pageToView + i + 1
        val q = new Queue[Int]
        while (count < pageToView + pageCount + 1) {
          q += count
          count += maxThreads
        }
        threadNumber2WorkUnitsMap.put(i, q)
      }

      val lock = new Object
      val page2ImageDescriptorMap = new HashMap[Int, Node]
      for (threadNumber <- 0 until maxThreads) {
        spawn {
          val workQueue = new Queue[Int]
          spawn {
            var hasMore = true
            while (hasMore) {
              lock.synchronized {
                while (workQueue.size == 0) {
                  lock.wait()
                }

                val workItem = workQueue.dequeue()
                if (!viewer.isTilesCached(workItem)) {
                  if (workItem > -1) {
                    val pageTiles = ImageFetcher.generatePageTiles(ImageFetcher.getSubImageUrl(baseUri, tile2SourceMap(workItem)), page2ImageDescriptorMap(workItem), maxSupportedLevels)
                    val t = pageTiles(0)
                    viewer.createThumbnail(workItem, new Tile(t.uriSource, t.thumbnailUri, t.fileFormat, new Point(0, 0), t.overlapSize, 0, 0, t.tileSize))
                    viewer.create(workItem, pageTiles)
                  } else {
                    hasMore = false
                    Logger.instance.log(getClass.getName + "#fetch] Received end-of-queue signal. Exiting thread...")
                  }
                }
              }
            }
          }
          val thisQ = threadNumber2WorkUnitsMap(threadNumber)
          while (thisQ.size > 0) {
            val page = thisQ.dequeue()
            val source = tile2SourceMap(page)
            val subImageDescriptor = ImageFetcher.fetchImageDescriptor(baseUri, source)
            lock.synchronized {
              page2ImageDescriptorMap.put(page, subImageDescriptor)
              workQueue += page
              lock.notify()
            }
          }
          lock.synchronized {
            workQueue += -1
            lock.notify()
          }
        }
      }
  }
}
