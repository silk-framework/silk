package org.silkframework.plugins.dataset.csv

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import CSVSanitizer.sanitize

class CSVSanitizerTest extends AnyFlatSpec with Matchers {
  it should "sanitize the documented examples in OWASP" in {
    // Examples from https://owasp.org/www-community/attacks/CSV_Injection
    sanitize("""=1+2";=1+2""") shouldBe """"'=1+2"";=1+2""""
    sanitize("""=1+2'" ;,=1+2""") shouldBe """"'=1+2'"" ;,=1+2""""
  }
}
