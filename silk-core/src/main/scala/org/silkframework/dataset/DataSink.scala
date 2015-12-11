package org.silkframework.dataset

import java.io.Closeable

import org.silkframework.entity.Link

/**
 * Represents an abstraction over a data sink.
 */
trait DataSink extends Closeable