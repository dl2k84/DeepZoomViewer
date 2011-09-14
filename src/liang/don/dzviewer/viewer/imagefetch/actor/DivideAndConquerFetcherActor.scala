package liang.don.dzviewer.viewer.imagefetch.actor

import liang.don.dzviewer.viewer.imagefetch.TileFetcher

/**
 * Deep Zoom image fetch algorithm that takes an initial page value<br>
 * and continues to fetch images in the background for pages based<br>
 * on a divide-and-conquer style behavior by spawning threads that<br>
 * will fetch an arbitrary set of image tiles.<br>
 * <br>
 * E.g. Initial page value = 10, total pages = 20, max work units per thread = 5<br>
 * After page 10 images are fetched and loaded, this algorithm<br>
 * will then go on to retrieve other pages with a divide-and-conquer style.<br>
 * 4 threads will be spawned due to max work units per thread = 5<br>
 * i.e. pages yet to load = ceiling((total pages - 1) / max work units per thread)<br>
 *                        =(math.ceil(20 - 1)/5) = 4 threads<br><br>
 * Here's how the work units (pages) will be divided:<br>
 * Thread-1: { 9, 7, 5, 3, 1 }<br>
 * Thread-2: { 8, 6, 4, 2, 0 } // Depending on impl style and interpretation, remove 0 if the page count starts at 1<br>
 * Thread-3: { 11, 13, 15, 17, 19 }<br>
 * Thread-4: { 12, 14, 16, 18, 20 } // Similar to Thread-2,remove 20 if page count starts at 0<br>
 * <br>
 * This algorithm is good for when the user will jump between pages instead<br>
 * of sequentially scrolling through "previous" and "next" pages.<br>
 * <br>
 * The downside is that it will spawn more threads than other algorithms.<br>
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
trait DivideAndConquerFetcherActor extends TileFetcher {
// TODO

  override def fetch() {
    // TODO
  }
}
