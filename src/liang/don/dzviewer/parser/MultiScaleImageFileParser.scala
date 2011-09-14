package liang.don.dzviewer.parser

import collection.mutable.HashMap
import xml.Node
import liang.don.dzviewer.format.MultiScaleImageCollectionAttributes._
import liang.don.dzviewer.format.MultiScaleImageElements._
import liang.don.dzviewer.tile.ImageSize

/**
 * Parses and returns the values contained in a DeepZoom descriptor.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
object MultiScaleImageFileParser {

  def getFormat(node: Node): String = {
    (node \ FORMAT).toString()
  }

  def getImageOverlapSize(node: Node): Int = {
    (node \ IMAGE_OVERLAP_SIZE).toString().toInt
  }

  def getItemCount(node: Node): Int = {
    (node \ ITEM_COUNT).toString().toInt
  }

  def getMaxLevel(node: Node): Int = {
    (node \ MAX_LEVEL).toString().toInt
  }

  def getTileNumber(node: Node): Int = {
    // 0-index origin
    (node \ TILE_NUMBER).toString().toInt
  }

  def getTileSize(node: Node): Int = {
    (node \ TILE_SIZE).toString().toInt
  }

  def getUriSource(node: Node): String = {
    (node \ URI_SOURCE).toString()
  }

  def getImageCollection(doc: Node): Seq[Node] = doc \\ IMAGE_COLLECTION

  def getImageSize(node: Node): ImageSize = {
    val sizeElement = node \ IMAGE_SIZE
    new ImageSize((sizeElement \ IMAGE_WIDTH).toString().toInt, (sizeElement \ IMAGE_HEIGHT).toString().toInt)
  }

  def getImageSizeFromCollection(node: Node, page: Int): ImageSize = {
    getImageCollection(node).foreach {
      n => {
        val tileNumber = (n \ TILE_NUMBER).toString().toInt
        if (page == tileNumber) {
          return getImageSize(n)
        }
      }
    }
    null
  }

  def getTile2UrlSourceMap(node: Seq[Node]): Map[Int, String] = {
    val map = new HashMap[Int, String]
    node.foreach(n => map.put(getTileNumber(n), getUriSource(n)))

    map.toMap // TODO better way to convert mutable to immutable map?
  }

}
