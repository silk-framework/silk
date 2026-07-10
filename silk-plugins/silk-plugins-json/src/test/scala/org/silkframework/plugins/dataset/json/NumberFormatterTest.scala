package org.silkframework.plugins.dataset.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.math.{BigDecimal => JBigDec}

class NumberFormatterTest extends AnyFlatSpec with Matchers {

  behavior of "NumberFormatter"

  it should "format integers in plain notation" in {
    format("172800000") should equal ("172800000")
    format("1.728e8") should equal ("172800000")
    format("0") should equal ("0")
    format("-42") should equal ("-42")
  }

  it should "format decimals in plain notation" in {
    format("19.99") should equal ("19.99")
    format("-12.5") should equal ("-12.5")
    format("1e-7") should equal ("0.0000001")
  }

  it should "strip trailing zeros" in {
    format("1.50") should equal ("1.5")
    format("2.000") should equal ("2")
  }

  it should "use scientific notation if the plain notation would be excessively long" in {
    format("1e1000") should equal ("1E+1000")
    format("-1e1000") should equal ("-1E+1000")
    format("1e-1000") should equal ("1E-1000")
    // The limit also holds for exponents that are too large to expand in memory
    format("1e100000000") should equal ("1E+100000000")
  }

  private def format(value: String): String = {
    NumberFormatter.format(new JBigDec(value))
  }
}
