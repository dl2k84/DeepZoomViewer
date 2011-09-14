package liang.don.dzviewer.tile

/**
 * Stores information regarding the zoom level of a given image size.
 *
 * @constructor Create a new zoom level setting with a zoom level value and
 *                original image size setting.
 * @param level The zoom level.
 * @param imageSize The image size.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
class ZoomLevel(val level: Int, val imageSize: ImageSize) { }
