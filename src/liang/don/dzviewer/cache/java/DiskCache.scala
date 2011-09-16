package liang.don.dzviewer.cache.java

import collection.mutable.{HashMap, Map}
import liang.don.dzviewer.tile.ImageTile
import java.io._
import liang.don.dzviewer.cache.DeepZoomCache
import liang.don.dzviewer.log.{LogLevel, Logger}

/**
 * Implements the caching of DeepZoom image tiles onto the local hard disk.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
trait DiskCache extends DeepZoomCache {

  private val filenameWithZoomLevel2TilesMap = new HashMap[String, Map[Int, Array[ImageTile]]]

  override def get(page: Int, zoomLevel: Int, fileUuid: String): Array[ImageTile] = {
    Logger.instance.log("[" + getClass.getName + "#get] Getting cache for " + fileUuid + " [ " + page + " ] with zoom level=" + zoomLevel, LogLevel.Debug)
    val cache = getFromUuid(fileUuid, zoomLevel)
    cache.getOrElse(page, null)
  }

  override def update(page: Int, zoomLevel: Int, fileUuid: String, tiles: Array[ImageTile]) {
    val cache = getFromUuid(fileUuid, zoomLevel)
    cache.put(page, tiles)
    saveCache(fileUuid, zoomLevel, cache)
  }

  private def getFromUuid(fileUuid: String, zoomLevel: Int): Map[Int, Array[ImageTile]] = {
    val filenameWithZoomLevel = fileUuid + zoomLevel
    val tilesMap = filenameWithZoomLevel2TilesMap.getOrElse(filenameWithZoomLevel, null)
    if (tilesMap == null) {
      val cache = getCache(filenameWithZoomLevel)
      filenameWithZoomLevel2TilesMap.put(filenameWithZoomLevel, cache)
      cache
    } else {
      tilesMap
    }
  }

  private def getCache(filename: String): Map[Int, Array[ImageTile]] = {
    val pathToFile = getCacheFolder + File.separator +  filename
    val file = new File(pathToFile)
    if (!file.exists()) {
      return new HashMap[Int, Array[ImageTile]]
    }
    val inputStream = new ObjectInputStream(new FileInputStream(file))
    val map = {
      val m = inputStream.readObject().asInstanceOf[Map[Int, Array[ImageTile]]]
      if (m == null) {
        new HashMap[Int, Array[ImageTile]]
      } else {
        m
      }
    }
    inputStream.close()
    map
  }

  /**
   * Returns base folder where cached tiles are stored.
   * If the configured cache folder does not exist, also create it.
   */
  private def getCacheFolder: String = {
    val cacheBaseFolder = baseFolder
    val cacheFolder = new File(cacheBaseFolder)
    if (!cacheFolder.exists()) {
      cacheFolder.mkdirs()
    }
    cacheBaseFolder
  }

  private def saveCache(filename: String, zoomLevel: Int, cache: AnyRef) {
    Logger.instance.log("[" + getClass.getName + "#saveCache] Saving cache for zoomLevel " + zoomLevel, LogLevel.Debug)
    val filenameWithZoomLevel = filename + zoomLevel
    val pathToFile = getCacheFolder + File.separator + filenameWithZoomLevel
    val file = new File(pathToFile)
    if (!file.exists()) {
      Logger.instance.log("[" + getClass.getName + "#saveCache] Creating new cache file: " + pathToFile, LogLevel.Debug)
      file.createNewFile()
    }
    val outputStream = new ObjectOutputStream(new FileOutputStream(file))
    outputStream.writeObject(cache)
    outputStream.close()
  }
}
