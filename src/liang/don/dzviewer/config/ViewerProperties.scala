package liang.don.dzviewer.config

import java.util.Properties
import scala.io.Source
import liang.don.dzviewer.ImageFetcher

/**
 * Contains settings related to the application as
 * well as the downloaded DeepZoom image descriptor.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
object ViewerProperties {

  // TODO write a version that does not rely on the java properties api
  // TODO Separate objects for application settings and DeepZoom descriptor settings.

  private val PROPERTIES_FILE = "Viewer.properties"
  private val BASE_URL_KEY = "baseUrl"
  private val BUILD_TARGET_KEY = "buildTarget"
  private val DISPLAY_RATIO_KEY = "displayRatio"
  private val FETCH_MECHANISM_KEY = "fetchMechanism"
  private val IMAGE_QUALITY_KEY = "imageQuality"
  private val INITIAL_PAGE_KEY = "initialPage"
  private val LOG_TYPE_KEY = "logType"
  private val MAX_WORK_UNITS_PER_THREAD_KEY = "maxWorkUnitsPerThread"
  private val RESET_ZOOM_ON_PAGE_CHANGE_KEY = "resetZoomOnPageChange"

  @deprecated("threadModel setting is no longer used.", "0.0.1")
  private val THREAD_MODEL_KEY = "threadModel"

  private var _fileFormat: String = null
  private var _tileSize: Int = 0

  private val PROPERTIES = getProperties

  /**
   *
   * @author Don Liang
   * @Version 0.1, 14/09/2011
   */
  object BuildTarget {
    def Java = "java"
    def Net = "net"
  }

  /**
   *
   * @author Don Liang
   * @Version 0.1, 14/09/2011
   */
  object DisplayRatio {
    def Default = "default"
    def Scale = "scale"
  }

  /**
   *
   * @author Don Liang
   * @Version 0.1, 14/09/2011
   */
  object FetchMechanism {
    def Progressive = "Progressive"
    def DivideAndConquer = "DivideAndConquer"
  }

  /**
   *
   * @author Don Liang
   * @Version 0.1, 14/09/2011
   */
  object ImageQuality {
    def Lossless = "lossless"
    def Lossy = "lossy"
  }

  /**
   *
   * @author Don Liang
   * @Version 0.1.1, 15/09/2011
   */
  object LogType {
    def Console = "Console"
    def File = "File"
  }

  /**
   *
   * @author Don Liang
   * @Version 0.1, 14/09/2011
   */
  @deprecated("threadModel setting is no longer used.", "0.0.1")
  object ThreadModel {
    def Default = "default"
    def Scala = "scala"
  }

  def baseUrl: String = PROPERTIES.getProperty(BASE_URL_KEY)
  def buildTarget: String = PROPERTIES.getProperty(BUILD_TARGET_KEY)
  def displayRatio: String = PROPERTIES.getProperty(DISPLAY_RATIO_KEY)
  def fetchMechanism: String = PROPERTIES.getProperty(FETCH_MECHANISM_KEY)
  def fileFormat = _fileFormat
  def fileFormat_=(value: String) {
    _fileFormat = value
  }
  def imageQuality: String = PROPERTIES.getProperty(IMAGE_QUALITY_KEY)
  def initialPage: Int = PROPERTIES.getProperty(INITIAL_PAGE_KEY).toInt
  def logType: String = PROPERTIES.getProperty(LOG_TYPE_KEY)
  def maxWorkUnitsPerThread: Int = PROPERTIES.getProperty(MAX_WORK_UNITS_PER_THREAD_KEY).toInt
  def resetZoomOnPageChange: Boolean = PROPERTIES.getProperty(RESET_ZOOM_ON_PAGE_CHANGE_KEY).toBoolean

  @deprecated("threadModel setting is no longer used.", "0.0.1")
  def threadModel: String = PROPERTIES.getProperty(THREAD_MODEL_KEY)

  def thumbnailLevel: Int = ImageFetcher.calculateMaximumZoomLevel(tileSize, tileSize)
  def tileSize = _tileSize
  def tileSize_=(value: Int) {
    _tileSize = value
  }

  private def getProperties: Properties = {
    val p = new Properties()
    val source = Source.fromFile(PROPERTIES_FILE, "UTF-8")
    p.load(source.bufferedReader())
    source.close()
    p
  }
}
