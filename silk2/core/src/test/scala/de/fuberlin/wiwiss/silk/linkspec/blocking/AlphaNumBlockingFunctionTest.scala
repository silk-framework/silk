package de.fuberlin.wiwiss.silk.linkspec.blocking

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.linkspec.util.approximatelyEqualTo

class AlphaNumBlockingFunctionTest extends FlatSpec with ShouldMatchers
{
    val f = new AlphaNumBlockingFunction()

    "AlphaNumBlockingFunction" should "assign a different index to all numeric and alphabetic characters" in
    {
        f("0") should be (approximatelyEqualTo (0 / 36.0))
        f("9") should be (approximatelyEqualTo (9 / 36.0))
        f("a") should be (approximatelyEqualTo (10 / 36.0))
        f("z") should be (approximatelyEqualTo ((10 + 'z' - 'a') / 36.0))
        f("_") should be (approximatelyEqualTo (35.0 / 36.0))
    }

    "AlphaNumBlockingFunction" should "index strings with multiple characters" in
    {
        f("00") should be (approximatelyEqualTo ((0 + 0 / 36.0) / 36.0))
        f("99") should be (approximatelyEqualTo ((9 + 9 / 36.0) / 36.0))
        f("9b") should be (approximatelyEqualTo ((9 + 11 / 36.0) / 36.0))
        f("b9") should be (approximatelyEqualTo ((11 + 9 / 36.0) / 36.0))
        f("a_") should be (approximatelyEqualTo ((10 + 35 / 36.0) / 36.0))
    }
}
