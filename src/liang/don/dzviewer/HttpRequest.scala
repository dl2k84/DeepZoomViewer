package liang.don.dzviewer
import java.net._
import scala.xml._

/**
 * Performs HTTP/HTTPS based requests for DeepZoom image descriptor.
 *
 * @constructor Create a new request object.
 * @param uri The URL to retrieve data from.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
class HttpRequest(val uri: URL) {

  /**
   * @constructor Create a new request object.
   * @param uri The URL as a String to retrieve data from.
   */
  def this(uri: String) = this(new URL(uri))

  /**
   * Retrieves the XML descriptor from the specified URL.
   *
   * @return The XML descriptor from the specified URL.
   */
  def getXmlDescriptor: Node = {
    XML.load(uri)
  }

}
