package liang.don.dzviewer.tile.java

import javax.imageio.ImageIO
import java.io.{ObjectOutputStream, IOException, ObjectInputStream}
import java.awt.image.BufferedImage
import liang.don.dzviewer.config.ViewerProperties

/**
 * Wrapper that allows java.awt.image.BufferedImage images to be serializable.
 *
 * @constructor Create a new TileWrapper with a java.awt.image.BufferedImage.
 * @param image The image of type java.awt.image.BufferedImage.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
class TileWrapper(var image: AnyRef) extends Serializable {

  @throws(classOf[IOException])
  private def writeObject(out: ObjectOutputStream) {
    ImageIO.write(image.asInstanceOf[BufferedImage], ViewerProperties.fileFormat, out)
  }

  @throws(classOf[IOException])
  private def readObject(in: ObjectInputStream) {
    image = ImageIO.read(in)
  }
}
