package org.silkframework.workspace.annotation

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.util.XmlSerializationHelperTrait

class UiAnnotationsTest extends FlatSpec with MustMatchers with XmlSerializationHelperTrait {
  behavior of "Sticky note"

  it should "de-/serialize from/to XML" in {
    val stickyNote = StickyNote(
      "sticky ID",
      "content with\nnew\n\nlines",
      "#fff",
      (3.5, 6.7),
      (20.1, 24.9)
    )
    testRoundTripSerialization(UiAnnotations(Seq(stickyNote, stickyNote)))
  }
}
