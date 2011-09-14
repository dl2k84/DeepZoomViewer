package liang.don.dzviewer.tile

/**
 * Stores information regarding an image tile.
 *
 * @constructor Create a new tile setting with its image content, URL,
 *                image format, position, overlap size, column position,
 *                row position, and tile size.
 * @param image The image data. The instance will differ depending on if this is
 *               executed using the Java or .NET runtime.
 * @param uriSource The URL of the image this tile is from.
 * @param fileFormat The image format.
 * @param position The position of the tile as part of the whole image.
 * @param overlapSize The overlap size (if any) in this tile.
 * @param column The column position of the tile as part of the whole image.
 * @param row The row position of the tile as part of the whole image.
 * @param tileSize The length of this tile (width and height).
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
class ImageTile(val image: AnyRef, val uriSource: String, val fileFormat: String, val position: Point, val overlapSize: Int, val column: Int, val row: Int, val tileSize: Int)
  extends Serializable { }
