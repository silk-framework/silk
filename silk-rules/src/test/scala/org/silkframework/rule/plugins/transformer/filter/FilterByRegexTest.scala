package org.silkframework.rule.plugins.transformer.filter

import org.silkframework.rule.test.TransformerTest

class FilterByRegexTest extends TransformerTest[FilterByRegex] {

  "negate" should "produce the exact complement of its non-negated result, for the same regex and input" in {
    val values = Seq("abc", "xabcx", "xyz", "abcabc")
    val kept = FilterByRegex(regex = "abc", negate = false)(Seq(values))
    val excluded = FilterByRegex(regex = "abc", negate = true)(Seq(values))

    kept.toSet ++ excluded.toSet shouldEqual values.toSet
    (kept.toSet intersect excluded.toSet) shouldEqual Set.empty
  }

  "contains" should "keep a superset of what full match keeps, for the same regex and input" in {
    val values = Seq("abc", "xabcx", "xyz", "abcabc")
    val fullMatch = FilterByRegex(regex = "abc", contains = false)(Seq(values))
    val contains = FilterByRegex(regex = "abc", contains = true)(Seq(values))

    fullMatch.toSet.subsetOf(contains.toSet) shouldBe true
  }

  "negate" should "produce the exact complement of its non-negated result, for the same regex and input, when contains is also set" in {
    val values = Seq("abc", "xabcx", "xyz", "abcabc")
    val kept = FilterByRegex(regex = "abc", negate = false, contains = true)(Seq(values))
    val excluded = FilterByRegex(regex = "abc", negate = true, contains = true)(Seq(values))

    kept.toSet ++ excluded.toSet shouldEqual values.toSet
    (kept.toSet intersect excluded.toSet) shouldEqual Set.empty
  }
}
