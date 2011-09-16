package liang.don.dzviewer.cache

/**
 * Options related to caching style.
 *
 * @author Don Liang
 * @Version 0.1, 14/09/2011
 */
object CacheOptions {

  /**
   * Hash style for cached image tiles..
   *
   * @author Don Liang
   * @Version 0.1, 14/09/2011
   */
  object HashType extends Enumeration {
    val SHA1 = Value("SHA-1") // default
    val Base64 = Value("Base64") // TODO support later.
  }

}
