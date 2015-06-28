package de.fuberlin.wiwiss.silk.plugins.transformer.linguistic

import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import java.text.Normalizer
import java.util.regex.Pattern
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin

/**
 * Normalizes strings by removing diacritics, converting ß to ss, etc.
 * Implementation based on Andreas Petersson's answer here:
 * http://stackoverflow.com/questions/1453171/n-n-n-or-remove-diacritical-marks-from-unicode-cha
 * @author Florian Kleedorfer, Research Studios Austria
 */
@Plugin(
  id = "normalizeChars",
  categories = Array("Linguistic"),
  label = "normalizeChars",
  description = "Replaces diacritical characters with non-diacritical ones (eg, ö -> o), plus some specialities like transforming æ -> ae, ß -> ss."
)
case class NormalizeCharsTransformer() extends SimpleTransformer {

  /*
 special regexp char ranges relevant for simplification -> see http://docstore.mik.ua/orelly/perl/prog3/ch05_04.htm
 InCombiningDiacriticalMarks: special marks that are part of "normal" ä, ö, î etc..
         IsSk: Symbol, Modifier see http://www.fileformat.info/info/unicode/category/Sk/list.htm
         IsLm: Letter, Modifier see http://www.fileformat.info/info/unicode/category/Lm/list.htm
  */
  private val DIACRITICS_AND_FRIENDS = Pattern.compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");

  private val NONDIACRITICS = Map(
    //replace non-diacritics as their equivalent chars
    ('\u0141' -> "l"), // BiaLystock
    ('\u0142' -> "l"), // Bialystock
    ('ß' -> "ss"),
    ('æ' -> "ae"),
    ('ø' -> "o"),
    ('©' -> "c"),
    ('\u00D0' -> "d"), // all Ð ð from http://de.wikipedia.org/wiki/%C3%90
    ('\u00F0' -> "d"),
    ('\u0110' -> "d"),
    ('\u0111' -> "d"),
    ('\u0189' -> "d"),
    ('\u0256' -> "d"),
    ('\u00DE' -> "th"), // thorn Þ
    ('\u00FE' -> "th")) // thorn þ


  def evaluate(value: String) = {
    simplifyString(value)
  }

  private def simplifyString(str: String): String = {
    if (str == null) {
      null
    } else {
      stripNonDiacritics(stripDiacritics(str))
    }
  }

  private def stripNonDiacritics(orig: String): String = {
    val ret = new StringBuffer()
    for (i <- 0 until orig.length()) {
      val source = orig.charAt(i)
      val replace = NONDIACRITICS.get(source).getOrElse(source)
      ret.append(replace)
    }
    ret.toString
  }

  private def stripDiacritics(str: String): String = {
    val normalized = Normalizer.normalize(str, Normalizer.Form.NFD)
    DIACRITICS_AND_FRIENDS.matcher(normalized).replaceAll("")
  }


}
