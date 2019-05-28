package org.silkframework.rule

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.{Path, StringValueType, TypedPath}
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.similarity.Comparison
import org.silkframework.util.DPair

class BooleanLinkageRuleTest extends FlatSpec with MustMatchers {
  behavior of "Boolean Linkage Rule"

  private val A = leaf("A")
  private val B = leaf("B")
  private val C = leaf("C")
  private val D = leaf("D")
  private val E = leaf("E")
  private val F = leaf("F")
  private val G = leaf("G")
  private val H = leaf("H")

  private val alreadyCNF = and(
    or(
      NOT(A),
      B
    ),
    or(
      C
    )
  )

  it should "convert CNF to CNF by leaving it untouched" in {
    toCNF(alreadyCNF) mustBe alreadyCNF
  }

  it should "turn an expression with double negation into CNF" in {
    toCNF(NOT(NOT(alreadyCNF))) mustBe alreadyCNF
  }

  it should "turn a simple expressions to CNF" in {
    toCNF(A) mustBe and(or(A))
    toCNF(NOT(B)) mustBe and(or(NOT(B)))
    toCNF(or(A, B)) mustBe and(or(A, B))
    toCNF(and(A, B)) mustBe and(or(A), or(B))
  }

  it should "distribute AND over OR" in {
    toCNF(
      or(
        and(A, B),
        and(C)
      )
    ) mustBe and(
      or(A, C),
      or(B, C)
    )
  }

  it should "eliminate unnecessary nesting, but keep CNF structure" in {
    toCNF(
      and(
        and(A, B),
        and(C, D)
      )
    ) mustBe and(
      or(A), or(B), or(C), or(D)
    )

    toCNF(
      or(
        or(A, B),
        or(C, or(D, E))
      )
    ) mustBe and(or(A, B, C, D, E))
  }

  it should "turn complex expressions into CNF" in {
    /** CNF conversion for tests done with Wolfram Alpha,
      * e.g. https://www.wolframalpha.com/input/?i=CNF++(~((~A+%26%26+B+%26%26+(C+%26%26+D))+%7C%7C+(E+%26%26+(F++%7C%7C+(G+%7C%7C+~H))))) */
    toCNF(
      NOT(
        or(
          and(
            NOT(A),
            B,
            and(C, D)
          ),
          and(
            or(E),
            or(
              F,
              or(
                G,
                NOT(H)
              )
            )
          )
        )
      )
    ) mustBe and(
      or(A, NOT(B), NOT(C), NOT(D)),
      or(NOT(E), NOT(F)),
      or(NOT(E), NOT(G)),
      or(NOT(E), H)
    )
    toCNF(
      // ¬(¬A ∨ (B ∧ C) ∨ ((D ∨ E) ∧ (G ∧ ¬H)))
      NOT(or(NOT(A), and(B, C), and(or(D, E), and(G, NOT(H)))))
    ) mustBe and( // A ∧ (¬B ∨ ¬C) ∧ (¬D ∨ ¬G ∨ H) ∧ (¬E ∨ ¬G ∨ H)
        or(A),
        or(NOT(B), NOT(C)),
        or(NOT(D), NOT(G), H),
        or(NOT(E), NOT(G), H)
      )
  }

  private val dummyPathInput = PathInput("path", Path("path"))
  private val dummyValueOutputOperator = InputPathOperator(TypedPath("path", StringValueType), dummyPathInput)

  private val dummyComparison = Comparison("dummyComparison", metric = EqualityMetric(), inputs = DPair(dummyPathInput, dummyPathInput))

  private def toCNF(boolOperator: BooleanOperator): BooleanOperator = {
    BooleanLinkageRule(boolOperator).toCNF.asBooleanOperator
  }

  private def leaf(id: String): BooleanOperator = {
    BooleanComparisonOperator(id, dummyValueOutputOperator, dummyValueOutputOperator, dummyComparison)
  }

  private def and(children: BooleanOperator*): BooleanAnd = {
    BooleanAnd(children)
  }

  private def NOT(child: BooleanOperator): BooleanNot = {
    BooleanNot(child)
  }

  private def or(children: BooleanOperator*): BooleanOr = {
    BooleanOr(children)
  }
}
