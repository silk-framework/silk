package org.silkframework.plugins.dataset.csv.charset

import java.nio.charset.Charset
import scala.collection.JavaConverters._

/**
  * Methods to ask for all available charsets and to retrieve individual charsets by name.
  *
  * Note that the Java Charset class does only load charsets from the system classloader.
  * Because sbt uses a custom system classloader, it will not load the custom [[Utf8BomCharset]].
  * Methods in this class fix this special case.
  */
object CharsetUtils {

  /**
    * All available charset names, not including the aliases.
    */
  lazy val charsetNames: Seq[String] = {
    val defaultCharsets = Charset.availableCharsets().keySet.asScala.toSeq
    (defaultCharsets :+ Utf8BomCharset.name).sorted
  }

  /**
    * Lookup a charset by name.
    */
  def forName(name: String): Charset = {
    if(name == Utf8BomCharset.name) {
      Utf8BomCharset
    } else {
      Charset.forName(name)
    }
  }

}
