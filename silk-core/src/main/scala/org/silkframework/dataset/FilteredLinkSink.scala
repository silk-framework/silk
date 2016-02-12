package org.silkframework.dataset

import org.silkframework.entity.Link

/**
 * A link sink that pre filters links before writing it to the underlying [[LinkSink]]
 * @param linkSink the underlying link sink that is wrapped.
 * @param filterFn True if the link should be passed to the underlying link sink, else it is ignored.
 */
case class FilteredLinkSink(linkSink: LinkSink, filterFn: Link => Boolean) extends LinkSink {
  /**
   * Initialize the link sink
   */
  override def init(): Unit = linkSink.init()

  /**
   * Filter the link before writing it to the underlying link sink.
   */
  override def writeLink(link: Link, predicateUri: String): Unit = {
    if(filterFn(link)) {
      linkSink.writeLink(link, predicateUri)
    }
  }

  override def close(): Unit = linkSink.close()
}
