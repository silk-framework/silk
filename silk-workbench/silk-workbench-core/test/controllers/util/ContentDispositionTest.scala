package controllers.util

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class ContentDispositionTest extends AnyFlatSpec with Matchers {
  behavior of "ContentDisposition"

  it should "keep plain ASCII file names as a simple quoted filename parameter" in {
    ContentDisposition("attachment", "report.csv") mustBe """attachment; filename="report.csv""""
  }

  it should "encode umlauts in decomposed form (NFD, as produced by macOS) RFC 5987 style" in {
    val nfdName = "test_with_umlauts_äöü.pdf"
    ContentDisposition("attachment", nfdName) mustBe
      "attachment; filename=\"test_with_umlauts_a?o?u?.pdf\"; filename*=UTF-8''test_with_umlauts_a%CC%88o%CC%88u%CC%88.pdf"
  }

  it should "encode umlauts in precomposed form (NFC) RFC 5987 style" in {
    val nfcName = "test_with_umlauts_äöü.pdf"
    ContentDisposition("inline", nfcName) mustBe
      "inline; filename=\"test_with_umlauts_???.pdf\"; filename*=UTF-8''test_with_umlauts_%C3%A4%C3%B6%C3%BC.pdf"
  }

  it should "not put quotes or backslashes into the fallback file name and percent-encode them" in {
    ContentDisposition("attachment", "a\"b\\c d.txt") mustBe
      "attachment; filename=\"a?b?c d.txt\"; filename*=UTF-8''a%22b%5Cc%20d.txt"
  }
}
