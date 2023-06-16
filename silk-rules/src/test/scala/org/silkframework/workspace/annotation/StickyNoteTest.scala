package org.silkframework.workspace.annotation

import org.silkframework.util.XmlSerializationHelperTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class StickyNoteTest extends AnyFlatSpec with Matchers with XmlSerializationHelperTrait {
  behavior of "Sticky note"

  it should "de-/serialize from/to XML" in {
    testRoundTripSerialization(StickyNote(
      "sticky ID",
      "content with\nnew\n\nlines",
      "#fff",
      (3.5, 6.7),
      (20.1, 24.9)
    ))
  }
}
