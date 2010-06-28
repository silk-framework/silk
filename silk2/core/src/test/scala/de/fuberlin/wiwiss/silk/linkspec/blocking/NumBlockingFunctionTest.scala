package de.fuberlin.wiwiss.silk.linkspec.blocking

import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.linkspec.util.approximatelyEqualTo
import org.scalatest.FlatSpec

class NumBlockingFunctionTest extends FlatSpec with ShouldMatchers
{
    val f = new NumBlockingFunction()

    "NumBlockingFunction" should "return 0.0 for strings only consisting of zeros" in
    {
        f("0") should be (approximatelyEqualTo (0.0))
        f("00") should be (approximatelyEqualTo (0.0))
        f("000000000000") should be (approximatelyEqualTo (0.0))
    }

    "NumBlockingFunction" should "return 0.n for 'n'" in
    {
        f("1") should be (approximatelyEqualTo (0.1))
        f("2") should be (approximatelyEqualTo (0.2))
        f("3") should be (approximatelyEqualTo (0.3))
        f("4") should be (approximatelyEqualTo (0.4))
        f("5") should be (approximatelyEqualTo (0.5))
        f("6") should be (approximatelyEqualTo (0.6))
        f("7") should be (approximatelyEqualTo (0.7))
        f("8") should be (approximatelyEqualTo (0.8))
        f("9") should be (approximatelyEqualTo (0.9))
    }

    "NumBlockingFunction" should "return 0.nm for 'nm'" in
    {
        f("11") should be (approximatelyEqualTo (0.11))
        f("36") should be (approximatelyEqualTo (0.36))
        f("41") should be (approximatelyEqualTo (0.41))
        f("99") should be (approximatelyEqualTo (0.99))
    }

    "NumBlockingFunction" should "ignore leading zeros" in
    {
        f("00011") should be (approximatelyEqualTo (0.11))
        f("036") should be (approximatelyEqualTo (0.36))
        f("0041") should be (approximatelyEqualTo (0.41))
        f("099") should be (approximatelyEqualTo (0.99))
    }
}
