package liang.don.dzviewer

import config.ViewerProperties
import config.ViewerProperties._
import log.Logger
import parser.MultiScaleImageFileParser
import tile.{Point, Tile}
import viewer.imagefetch.actor.{DummyFetcherActor, DivideAndConquerFetcherActor, ProgressiveFetcherActor}
import viewer.imagefetch.TileFetcher
import viewer.java.DeepZoomViewerJ
import viewer.{ActorThreadedViewer, DeepZoomViewer}
import xml.Node
import java.net.URL

/**
 * Builds the DeepZoomViewer based on build language (Java/.NET)
 * and set application settings in Viewer.properties.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
class MultiScaleImageBuilder {

  private val baseUri = ViewerProperties.baseUrl
  private val buildTarget = ViewerProperties.buildTarget.toLowerCase
  private val initialPage = ViewerProperties.initialPage

  /**
   * Builds a DeepZoomViewer suitable for viewing a single DeepZoom image.
   *
   * @return DeepZoomViewer.
   */
  def buildMultiScaleImageViewer(): DeepZoomViewer = {

    // TODO: make this a polymorphic solution....
    val descriptor = new HttpRequest(baseUri).getXmlDescriptor
    ViewerProperties.fileFormat = MultiScaleImageFileParser.getFormat(descriptor)
    ViewerProperties.tileSize = MultiScaleImageFileParser.getTileSize(descriptor)

    if (MultiScaleImageFileParser.isSingleImage(descriptor)) {
      buildMultiScaleSingleImageViewer(descriptor)
    } else {
      buildMultiScaleImageCollectionViewer(descriptor, initialPage)
    }
  }

  /**
   * Builds a DeepZoomViewer suitable for viewing a collection of DeepZoom images.
   *
   * @param descriptor The base image XML descriptor.
   * @param initialPage The page to display when the application starts.
   *
   * @return DeepZoomViewer
   */
  def buildMultiScaleImageCollectionViewer(descriptor: Node, initialPage: Int): DeepZoomViewer = {
    Logger.instance.log("Building a MultiScaleImageViewer...")
    val maxSupportedLevels = MultiScaleImageFileParser.getMaxLevel(descriptor)
    val pageCount = MultiScaleImageFileParser.getItemCount(descriptor)
    val tile2SourceMap = MultiScaleImageFileParser.getTile2UrlSourceMap(MultiScaleImageFileParser.getImageCollection(descriptor))

    var pageToView = initialPage

    Logger.instance.log("Pages in document: " + pageCount)
    if (initialPage > pageCount - 1) {
      pageToView = (pageCount - 1)
    }

    val viewer = buildDeepZoomViewer(descriptor, pageCount, tile2SourceMap, maxSupportedLevels)
    viewer.loadCache()

    // TODO: correct MSI subimage impl commented out... currently only gets 1 page
    val subImageMap = ImageFetcher.fetchImageDescriptor(baseUri, tile2SourceMap(pageToView))
    val pageTiles = ImageFetcher.generatePageTiles(ImageFetcher.getSubImageUrl(baseUri, tile2SourceMap(pageToView)), subImageMap, maxSupportedLevels)

    viewer.setCurrentPage(pageToView)
    if (!viewer.isThumbnailCached(pageToView)) {
      val t = pageTiles(0)
      viewer.createThumbnail(pageToView, new Tile(t.uriSource, t.thumbnailUri, t.fileFormat, new Point(0, 0), t.overlapSize, 0, 0, t.tileSize))
    }
    if (!viewer.isTilesCached(pageToView)) {
      viewer.create(pageToView, pageTiles)
    }

    concurrent.ops.spawn {
      prefetchPageImagesExceptInitial(viewer, tile2SourceMap, baseUri, pageCount, pageToView, maxSupportedLevels)
    }

    viewer
  }

  private def buildDeepZoomViewer(descriptor: Node, totalPages: Int, tile2SourceMap: Map[Int, String], maxSupportedLevels: Int): DeepZoomViewer = {
    if (BuildTarget.Java == buildTarget) {
      new DeepZoomViewerJ(descriptor, totalPages, tile2SourceMap) with ActorThreadedViewer
    } else if (BuildTarget.Net == buildTarget) {
        // TODO - .NET Viewer
        null
    } else {
       sys.error("[" + getClass.getName + "#buildDeepZoomViewer] Invalid buildTarget.")
    }
  }

  private def buildMultiScaleSingleImageViewer(descriptor: Node): DeepZoomViewer = {
    Logger.instance.log("Building a SingleImageViewer...")
    val pageTiles = ImageFetcher.generatePageTiles(new URL(baseUri), descriptor, 0)
    val tile2SourceMap: Map[Int, String] = Map[Int, String](0 -> baseUrl)

    val viewer: DeepZoomViewer = {
      if (BuildTarget.Java == buildTarget) {
        new DeepZoomViewerJ(descriptor, 1, tile2SourceMap) with ActorThreadedViewer
      } else if (BuildTarget.Net == buildTarget) {
        // TODO - .NET Viewer
        null
      } else {
       sys.error("[" + getClass.getName + "#buildMultiScaleSingleImageViewer] Invalid buildTarget.")
      }
    }
    viewer.loadCache()
    viewer.setCurrentPage(0)
    if (!viewer.isThumbnailCached(0)) {
      val t = pageTiles(0)
      viewer.createThumbnail(0, new Tile(t.uriSource, t.thumbnailUri, t.fileFormat, new Point(0, 0), t.overlapSize, 0, 0, t.tileSize))
    }
    if (!viewer.isTilesCached(0)) {
      viewer.create(0, pageTiles)
    }
    viewer
  }

  private def getTileFetcher(viewer: DeepZoomViewer, tile2SourceMap: Map[Int, String], baseUri: String, pageToView: Int, leftPageCount: Int, rightPageCount: Int, maxSupportedLevels: Int): TileFetcher = {
    val fetchMechanism = ViewerProperties.fetchMechanism
    if (FetchMechanism.Progressive == fetchMechanism) {
      new TileFetcher(viewer, tile2SourceMap, baseUri, pageToView, leftPageCount, rightPageCount, maxSupportedLevels) with ProgressiveFetcherActor
    } else if (FetchMechanism.DivideAndConquer == fetchMechanism) {
      new TileFetcher(viewer, tile2SourceMap, baseUri, pageToView, leftPageCount, rightPageCount, maxSupportedLevels) with DivideAndConquerFetcherActor
    } else {
      // No prefetch.
      new TileFetcher(viewer, tile2SourceMap, baseUri, pageToView, leftPageCount, rightPageCount, maxSupportedLevels) with DummyFetcherActor
    }
  }

  private def prefetchPageImagesExceptInitial(viewer: DeepZoomViewer, tile2SourceMap: Map[Int, String], baseUri: String, totalPageCount:Int, pageToView: Int, maxSupportedLevels: Int) {
    // TODO Breadth-first and depth-first algorithms
    val leftPageCount = pageToView - 0 // Images before the pageToView to generate
    val rightPageCount = totalPageCount.toString.toInt - pageToView - 1 // -1 because 0-index origin

    val fetcher = getTileFetcher(viewer, tile2SourceMap, baseUri, pageToView, leftPageCount, rightPageCount, maxSupportedLevels)
    fetcher.fetch()
  }
}
