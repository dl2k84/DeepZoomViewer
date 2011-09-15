package liang.don.dzviewer.tile

/**
 * Stores information regarding an image tile.
 *
 * @constructor Create a new tile setting with its URL, image format,
 *                position, overlap size, column position, row position
 *                and tile size.
 * @param uriSource The URL of the image this tile is from
 * @param thumbnailUri The thumbnail URL (which is of maximum 1 tile size) of the image.
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
class Tile(val uriSource: String, val thumbnailUri: String, val fileFormat: String, val position: Point, val overlapSize: Int, val column: Int, val row: Int, val tileSize: Int) { }

// TODO Deprecate this and only use ImageTile.
