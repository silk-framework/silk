package org.silkframework.execution.methods

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.silkframework.entity.Path
import scala._

@RunWith(classOf[JUnitRunner])
class QGramsTest extends FlatSpec with ShouldMatchers {

  "QGrams" should "generate the correct sublists" in {
    subLists("Miller", q = 2, t = 0.8) should equal (millerSubLists4)
    subLists("Miller", q = 2, t = 0.75) should equal (millerSubLists3)
  }

  private def subLists(str: String, q: Int, t: Double) = {
    QGrams(Path(Nil), Path(Nil), q, t).generateSubLists(str)
  }

  // All sub-lists with minimum length 5
  val millerSubLists5 =
    Set(
      Seq("mi", "il", "ll", "le", "er")
    )

  // All sub-lists with minimum length 4
  val millerSubLists4 = millerSubLists5 ++
    Set(
      Seq("il", "ll", "le", "er"),
      Seq("mi", "ll", "le", "er"),
      Seq("mi", "il", "le", "er"),
      Seq("mi", "il", "ll", "er"),
      Seq("mi", "il", "ll", "le")
    )

  // All sub-lists with minimum length 3
  val millerSubLists3 = millerSubLists4 ++
    Set(
      Seq("ll", "le", "er"),
      Seq("il", "le", "er"),
      Seq("il", "ll", "er"),
      Seq("il", "ll", "le"),
      Seq("mi", "le", "er"),
      Seq("mi", "ll", "er"),
      Seq("mi", "ll", "le"),
      Seq("mi", "il", "er"),
      Seq("mi", "il", "le"),
      Seq("mi", "il", "ll")
    )
}
