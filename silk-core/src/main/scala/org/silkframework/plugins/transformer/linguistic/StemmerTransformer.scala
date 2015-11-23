/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.plugins.transformer.linguistic

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "stem",
  categories = Array("Linguistic"),
  label = "Stem",
  description = "Stems a string using the Porter Stemmer."
)
case class StemmerTransformer() extends SimpleTransformer {
  override def evaluate(value: String) = {
    val stemmer = new PorterStemmer
    stemmer.stem(value)
  }
}

/*
Porter stemmer in Scala. The original paper is in
Porter, 1980, An algorithm for suffix stripping, Program, Vol. 14,
no. 3, pp 130-137,
See also http://www.tartarus.org/~martin/PorterStemmer
A few methods were borrowed from the existing Java port from the above page.
*/
private class PorterStemmer {
  var word: String = ""

  def stem(string: String): String = {
    word = string
    if (word.length > 2) {
      step1()
      step2()
      step3()
      step4()
      step5a()
      step5b()
    }
    return word
  }

  // Just recode the existing stuff, then go through and refactor with some intelligence.
  private def cons(i: Int): Boolean = {
    var ch = word(i)

    // magic!
    var vowels = "aeiou"

    // multi return. yuck
    if (vowels.contains(ch))
      return false

    if (ch == 'y') {
      if (i == 0) {
        return true
      }
      else {
        // loop it!
        return !cons(i - 1)
      }
    }
    return true

  }

  /* m() measures the number of consonant sequences between 0 and j. if c is
     a consonant sequence and v a vowel sequence, and <..> indicates arbitrary
     presence,

        <c><v>       gives 0
        <c>vc<v>     gives 1
        <c>vcvc<v>   gives 2
        <c>vcvcvc<v> gives 3
        ....
  *
  * I think this can be recoded far more neatly.
  */

  private def calcM(s: String): Int = {
    var l = s.length
    var count = 0
    var currentConst = false

    for (c <- 0 to l - 1) {
      if (cons(c)) {
        if (!currentConst && c != 0) {
          count += 1
        }
        currentConst = true
      }
      else {
        currentConst = false
      }
    }

    return count
  }


  // removing the suffix 's', does a vowel exist?'
  private def vowelInStem(s: String): Boolean = {

    for (i <- 0 to word.length - 1 - s.length) {
      if (!cons(i)) {
        return true
      }
    }

    return false;

  }

  /* doublec(j) is true <=> j,(j-1) contain a double consonant. */
  private def doublec(): Boolean = {
    var l = word.length - 1

    if (l < 1)
      return false

    if (word(l) != word(l - 1))
      return false

    return cons(l)

  }

  /* cvc(i) is true <=> i-2,i-1,i has the form consonant - vowel - consonant
     and also if the second c is not w,x or y. this is used when trying to
     restore an e at the end of a short word. e.g.

        cav(e), lov(e), hop(e), crim(e), but
        snow, box, tray.

  */

  private def cvc(s: String): Boolean = {
    var i = word.length - 1 - s.length
    if (i < 2 || !cons(i) || cons(i - 1) || !cons(i - 2))
      return false;

    var ch = word(i)

    var vals = "wxy"

    if (vals.contains(ch))
      return false

    return true;
  }


  // returns true if it did the change.
  private def replacer(orig: String, replace: String, checker: Int => Boolean): Boolean = {
    var l = word.length
    var origLength = orig.length
    var res = false
    if (word.endsWith(orig)) {
      var n = word.substring(0, l - origLength)
      var m = calcM(n)
      if (checker(m)) {
        word = n + replace
      }
      res = true

    }
    return res
  }

  // process the list of tuples to find which prefix matches the case.
  // checker is the conditional checker for m.
  def processSubList(l: List[(String, String)], checker: Int => Boolean): Boolean = {
    var iter = l.iterator
    var done = false

    while (!done && iter.hasNext) {
      var v = iter.next
      done = replacer(v._1, v._2, checker)

    }

    return done
  }

  def step1() {
    var l = word.length
    var m = calcM(word)

    // step 1a
    var vals = List(("sses", "ss"), ("ies", "i"), ("ss", "ss"), ("s", ""))
    processSubList(vals, _ >= 0)

    // step 1b
    if (!(replacer("eed", "ee", _ > 0))) {

      if ((vowelInStem("ed") && replacer("ed", "", _ >= 0)) || (vowelInStem("ing") && replacer("ing", "", _ >= 0))) {

        vals = List(("at", "ate"), ("bl", "ble"), ("iz", "ize"))

        if (!processSubList(vals, _ >= 0)) {
          // if this isn't done, then it gets more confusing.

          m = calcM(word)
          var last = word(word.length - 1)
          if (doublec() && !"lsz".contains(last)) {
            word = word.substring(0, word.length - 1)
          }
          else
          if (m == 1 && cvc("")) {
            word = word + "e"
          }
        }
      }
    }

    // step 1c
    (vowelInStem("y") && replacer("y", "i", _ >= 0))
  }

  def step2() = {

    var vals = List(("ational", "ate"), ("tional", "tion"), ("enci", "ence"), ("anci", "ance"), ("izer", "ize"), ("bli", "ble"), ("alli", "al"),
      ("entli", "ent"), ("eli", "e"), ("ousli", "ous"), ("ization", "ize"), ("ation", "ate"), ("ator", "ate"), ("alism", "al"),
      ("iveness", "ive"), ("fulness", "ful"), ("ousness", "ous"), ("aliti", "al"), ("iviti", "ive"), ("biliti", "ble"), ("logi", "log"))

    processSubList(vals, _ > 0)

  }

  def step3() = {
    var vals = List(("icate", "ic"), ("ative", ""), ("alize", "al"), ("iciti", "ic"), ("ical", "ic"), ("ful", ""), ("ness", ""))
    processSubList(vals, _ > 0)
  }

  def step4() = {

    // first part.
    var vals = List(("al", ""), ("ance", ""), ("ence", ""), ("er", ""), ("ic", ""), ("able", ""), ("ible", ""), ("ant", ""), ("ement", ""),
      ("ment", ""), ("ent", ""))

    var res = processSubList(vals, _ > 1)

    // special part.
    if (!res) {
      if (word.length > 4) {
        if (word(word.length - 4) == 's' || word(word.length - 4) == 't') {
          res = replacer("ion", "", _ > 1)

        }
      }
    }

    // third part.
    if (!res) {
      var vals = List(("ou", ""), ("ism", ""), ("ate", ""), ("iti", ""), ("ous", ""), ("ive", ""), ("ize", ""))
      res = processSubList(vals, _ > 1)
    }

  }

  def step5a() = {
    var res = false
    res = replacer("e", "", _ > 1)
    if (!cvc("e")) {
      res = replacer("e", "", _ == 1)
    }
  }

  def step5b() = {
    var res = false
    var m = calcM(word)
    if (m > 1 && doublec() && word.endsWith("l")) {
      word = word.substring(0, word.length - 1)
    }

  }

}
