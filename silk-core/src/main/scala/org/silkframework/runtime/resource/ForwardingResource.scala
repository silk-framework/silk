package org.silkframework.runtime.resource

import java.io.{File, InputStream}
import java.time.Instant
import scala.io.Codec

/**
  * A resource that reads through to another one, so that overrides of the wrapped resource are not bypassed,
  * e.g. the size guard or the load methods of a remote resource.
  *
  * Only for wrappers that pass the content through unchanged: a wrapper that transforms the content must
  * implement the read methods on top of its own [[inputStream]].
  *
  * Writing is not forwarded: a wrapper that intercepts writes states that for each method itself, so that a
  * write method added later is not forwarded silently.
  */
trait ForwardingResource extends Resource {

  /** The wrapped resource. */
  protected def underlying: Resource

  override def name: String = underlying.name

  override def path: String = underlying.path

  override def entryPath: Option[String] = underlying.entryPath

  override def underlyingFile: Option[File] = underlying.underlyingFile

  override def exists: Boolean = underlying.exists

  override def size: Option[Long] = underlying.size

  override def modificationTime: Option[Instant] = underlying.modificationTime

  override def inputStream: InputStream = underlying.inputStream

  override def read[T](reader: InputStream => T): T = underlying.read(reader)

  override def loadAsString(codec: Codec): String = underlying.loadAsString(codec)

  override def loadAsStringCapped(maxChars: Int, codec: Codec): CappedString = underlying.loadAsStringCapped(maxChars, codec)

  override def loadLines(codec: Codec): Seq[String] = underlying.loadLines(codec)

  override def loadAsBytes: Array[Byte] = underlying.loadAsBytes

  override def nonEmpty: Boolean = underlying.nonEmpty

  override def checkSizeForInMemory(): Unit = underlying.checkSizeForInMemory()

  override def toString: String = underlying.toString
}
