package org.silkframework.rule.plugins.transformer.linguistic

import java.text.Normalizer
import java.util.regex.Pattern

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.rule.plugins.transformer.normalize.{AlphaReduceTransformer, RemoveSpecialCharsTransformer}
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

/**
 * Normalizes strings by removing diacritics, converting ß to ss, etc.
 * Implementation based on Andreas Petersson's answer here:
 * http://stackoverflow.com/questions/1453171/n-n-n-or-remove-diacritical-marks-from-unicode-cha
 * @author Florian Kleedorfer, Research Studios Austria
 */
@Plugin(
  id = NormalizeCharsTransformer.pluginId,
  categories = Array("Linguistic"),
  label = "Normalize chars",
  description = "Replaces diacritical characters with non-diacritical ones (eg, ö -> o), plus some specialities like transforming æ -> ae, ß -> ss.",
  relatedPlugins = Array(
    new PluginReference(
      id = RemoveSpecialCharsTransformer.pluginId,
      description = "After Normalize chars, a string still contains its original punctuation, spaces, and symbols. Remove special chars removes all of those, keeping only letters, digits, and underscores."
    ),
    new PluginReference(
      id = AlphaReduceTransformer.pluginId,
      description = "Normalize chars is a substitution-only plugin: it converts diacritics but does not remove anything. Strip non-alphabetic characters is a removal-only plugin: it strips digits and punctuation while keeping letters and spaces, but leaves diacritical letters in their original form."
    )
  )
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


  def evaluate(value: String): String = {
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
    val ret = new StringBuilder()
    for (i <- 0 until orig.length()) {
      val source = orig.charAt(i)
      val replace = NONDIACRITICS.getOrElse(source, source)
      ret.append(replace)
    }
    ret.toString
  }

  private def stripDiacritics(str: String): String = {
    val normalized = Normalizer.normalize(str, Normalizer.Form.NFD)
    DIACRITICS_AND_FRIENDS.matcher(normalized).replaceAll("")
  }


}

object NormalizeCharsTransformer {
  final val pluginId = "normalizeChars"
}
