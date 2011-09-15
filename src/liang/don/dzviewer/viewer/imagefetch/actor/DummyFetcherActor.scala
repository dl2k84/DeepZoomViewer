package liang.don.dzviewer.viewer.imagefetch.actor

import liang.don.dzviewer.viewer.imagefetch.TileFetcher

/**
 * This implementation does not prefetch any tiles.
 *
 * @author Don Liang
 * @Version 0.1.1, 15/09/2011
 */
trait DummyFetcherActor extends TileFetcher {

  override def fetch() { }

}
