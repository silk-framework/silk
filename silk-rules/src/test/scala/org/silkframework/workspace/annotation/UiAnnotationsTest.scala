package org.silkframework.workspace.annotation


import org.silkframework.util.XmlSerializationHelperTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.rule.NodePosition

class UiAnnotationsTest extends AnyFlatSpec with Matchers with XmlSerializationHelperTrait {
  behavior of "Sticky note"

  it should "de-/serialize from/to XML" in {
    val stickyNote = StickyNote(
      "sticky ID",
      "content with\nnew\n\nlines",
      "#fff",
      NodePosition(3, 6, Some(20), Some(24))
    )
    testRoundTripSerialization(UiAnnotations(Seq(stickyNote, stickyNote)))
  }
}
