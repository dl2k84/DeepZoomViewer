package liang.don.dzviewer.viewer

import _root_.java.net.URL
import liang.don.dzviewer.tile.{Tile, ImageTile}
import actors.Actor
import javax.imageio.ImageIO
import collection.mutable.HashMap
import liang.don.dzviewer.tile.java.TileWrapper

/**
 * Retrieves DeepZoom image tiles using the Scala Actor concurrency model.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
trait ActorThreadedViewer extends DeepZoomViewer {

  private val page2RemainingTilesMap = new HashMap[Int, Int]
  private val page2PageTilesMap = new HashMap[Int, Array[ImageTile]]

  private var countDownCounter: CountDownCounter = null
  private var imageFetchActor: ImageFetchActor = null

  def create(pageNumber: Int, tiles: Array[Tile]) {
//    val pageTiles: Array[ImageTile] = Array.ofDim(tiles.length)

    if (countDownCounter == null) {
      countDownCounter = new CountDownCounter
      countDownCounter.start()
    }
    if (imageFetchActor == null) {
      imageFetchActor = new ImageFetchActor
      imageFetchActor.start()
    }

//    page2PageTilesMap.put(pageNumber, pageTiles)
//    page2RemainingTilesMap.put(pageNumber, tiles.length)
    println("Page [ " + pageNumber + " ] with tiles [ " + tiles.length  + " ]")

    // --- DEBUG USE START ---
//    val startTime = System.currentTimeMillis
    // --- DEBUG USE END ---

    val getTilesArray = Array.ofDim[Tile](tiles.length)
    var getCount = 0
    tiles.foreach {
      tile => {
        if (!isTileCached(pageNumber, tile)) {
          // TODO race condition between the 2 actors can occur!
//          countDownCounter ! Increment(pageNumber)
          println("pg " + pageNumber + " tile NOT CACHED. col=" + tile.column + ", row=" + tile.row + ", pos=(" + tile.position.x + ", " + tile.position.y + ")")
          //          imageFetchActor ! GetTile(pageNumber, tile)
          getTilesArray(getCount) = tile
          getCount += 1
        }
      }
    }
    if (0 < getCount) {
      page2RemainingTilesMap.put(pageNumber, getCount)
      page2PageTilesMap.put(pageNumber, Array.ofDim[ImageTile](getCount))
      imageFetchActor ! GetTile(pageNumber, getTilesArray.view(0, getCount).toList)
    }
//    imageFetchActor ! GetTiles(pageNumber, tiles)


    // --- DEBUG USE START ---
//    println(getClass.getName + "#create] Tiles creation done for page [ " + pageNumber + " ]. Time taken: " + (System.currentTimeMillis - startTime) + "ms.")
//    println(getClass.getName + "#create] Tiles creation done for page [ " + pageNumber + " ]. Elapsed time: " + (System.currentTimeMillis - DeepZoomViewerMain.startTime) + "ms.")
    // --- DEBUG USE END ---
  }

  override def createThumbnail(pageNumber: Int, tile: Tile) {
    if (countDownCounter == null) {
      countDownCounter = new CountDownCounter
      countDownCounter.start()
    }
    if (imageFetchActor == null) {
      imageFetchActor = new ImageFetchActor
      imageFetchActor.start()
    }

    if (!isThumbnailCached(pageNumber)) {
      imageFetchActor ! GetThumbnail(pageNumber, tile)
    }
  }

  private def createImage(pageNumber: Int, tile: Tile): ImageTile = {
    val bufferedTile = new ImageTile(new TileWrapper(ImageIO.read(new URL(tile.uriSource))), tile.uriSource, tile.fileFormat, tile.position, tile.overlapSize, tile.column, tile.row, tile.tileSize)
    println("TILE DOWNLOADED. position=(" + bufferedTile.position.x + ", " + bufferedTile.position.y + ", col=" + bufferedTile.column + ", row=" + bufferedTile.row)
    countDownCounter ! Decrement(pageNumber)
    bufferedTile
  }

  private def createThumbnailImage(pageNumber: Int, tile: Tile): ImageTile = {
    new ImageTile(new TileWrapper(ImageIO.read(new URL(tile.uriSource))), tile.uriSource, tile.fileFormat, tile.position, tile.overlapSize, tile.column, tile.row, tile.tileSize)
  }

  private case class Decrement(page: Int)
  private case class Increment(page: Int)
  private class CountDownCounter extends Actor {
    override def act() {
      loop { // TODO This counter is implemented and not meant to stop once started.- think over if this is ok or not.
        react {
          case Decrement(page) => {
            val tileCounter = page2RemainingTilesMap(page) - 1
            page2RemainingTilesMap.put(page, tileCounter)
            if (tileCounter == 0) {
              cacheTiles(page, page2PageTilesMap(page))
            }
          }
          case Increment(page) => {
            val tileCounter = page2RemainingTilesMap(page) + 1
            page2RemainingTilesMap.put(page, tileCounter)
          }
        }
      }
    }
  }

  private case class GetTile(pageNumber: Int, tileList: List[Tile])
  private case class GetThumbnail(pageNumber: Int, tile: Tile)
//  private case class GetTiles(pageNumber: Int, tiles: Array[Tile])
  private class ImageFetchActor extends Actor {
    override def act() {
      loop {
        react {
          case GetTile(pageNumber, tileList) => {
            // TODO
//            spawn {
              tileList.zipWithIndex.foreach {
                tile => {
                  val pageTiles = page2PageTilesMap(pageNumber)
                  pageTiles(tile._2) = createImage(pageNumber, tile._1)
                  if (page2RemainingTilesMap(pageNumber) == 0) {
                    cacheTiles(pageNumber, page2PageTilesMap(pageNumber))
                  }
                }
              }
//            }
          }
          case GetThumbnail(pageNumber, tile) => {
            cacheThumbnail(pageNumber, createThumbnailImage(pageNumber, tile))
          }
//          case GetTiles(pageNumber, tiles) => {
//            spawn {
//              tiles.zipWithIndex.foreach {
//                tile => {
//                  val pageTiles = page2PageTilesMap(pageNumber)
//                  pageTiles(tile._2) = createImage(pageNumber, tile._1)
//                }
//              }
//            }
//          }
        }
      }
    }
  }

}
