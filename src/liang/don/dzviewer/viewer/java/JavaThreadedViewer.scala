package liang.don.dzviewer.viewer.java

import concurrent.ops._
import liang.don.dzviewer.DeepZoomViewerMain
import collection.mutable.HashMap
import javax.imageio.ImageIO
import java.net.URL
import liang.don.dzviewer.tile.{ImageTile, Tile}
import liang.don.dzviewer.tile.java.TileWrapper
import liang.don.dzviewer.log.Logger

/**
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
@deprecated("This implementation using the java thread model is no longer supported", "0.0.1")
trait JavaThreadedViewer extends DeepZoomViewerJ {
// TODO - Override/impl methods that require the Java threading APIs

//  private val imagePanelLock: AnyRef = new Object
  private val page2CreateTilesLockMap = new HashMap[Int, AnyRef]
  private val page2RemainingTilesMap = new HashMap[Int, Int]

  override def create(pageNumber: Int, tiles: Array[Tile]) {
//    imagePanelLock.synchronized {
//      if (imagePanel.isTilesLoaded(pageNumber)) {
//        Logger.instance.log(getClass.getName + "create] ***** Image tiles for this page already loaded. *****")
//        return
//      }
//    }

    // TODO load page tiles from URL here...
    // TODO support all page creation, NOT just current...
    val pageTiles: Array[ImageTile] = Array.ofDim(tiles.length)
    val countDownLock: AnyRef = new Object
    page2CreateTilesLockMap.put(pageNumber, countDownLock)
    page2RemainingTilesMap.put(pageNumber, tiles.length)

    // --- DEBUG USE START ---
    val startTime = System.currentTimeMillis
    // --- DEBUG USE END ---

    tiles.zipWithIndex.foreach { tile =>
      spawn{
        pageTiles(tile._2) = createImage(pageNumber, tile._1)
      }
    }

    countDownLock.synchronized {
      if (page2RemainingTilesMap(pageNumber) > 0) {
        Logger.instance.log("Waiting for all images to be created...")
        countDownLock.wait()
      }
    }

    cacheTiles(pageNumber, pageTiles)
//    imagePanelLock.synchronized {
//      imagePanel.setTiles(pageNumber, pageTiles)
//    }

    // --- DEBUG USE START ---
    Logger.instance.log(getClass.getName + "#create] Tiles creation done for page [ " + pageNumber + " ]. Time taken: " + (System.currentTimeMillis - startTime) + "ms.")
    Logger.instance.log(getClass.getName + "#create] Elapsed time: " + (System.currentTimeMillis - DeepZoomViewerMain.startTime) + "ms.")
    // --- DEBUG USE END ---
  }


  private def createImage(pageNumber: Int, tile: Tile): ImageTile = {
    val bufferedTile = new ImageTile(new TileWrapper(ImageIO.read(new URL(tile.uriSource))), tile.uriSource, tile.thumbnailUri, tile.fileFormat, tile.position, tile.overlapSize, tile.column, tile.row, tile.tileSize)
    val countDownLock = page2CreateTilesLockMap(pageNumber)
    countDownLock.synchronized {
      val tileCounter = page2RemainingTilesMap(pageNumber) - 1
      page2RemainingTilesMap.put(pageNumber, tileCounter)
      if (tileCounter == 0) {
        countDownLock.notify()
      }
    }

    bufferedTile

  }
}
