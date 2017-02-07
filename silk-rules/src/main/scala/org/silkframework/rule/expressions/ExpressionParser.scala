package org.silkframework.rule.expressions

import org.silkframework.config.Prefixes
import org.silkframework.rule.input.{Transformer, Input => SilkInput}
import org.silkframework.runtime.resource.EmptyResourceManager
import org.silkframework.runtime.validation.ValidationException
import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input.CharSequenceReader

object ExpressionParser {

  def parse(expr: String)(implicit prefixes: Prefixes): SilkInput = {
    new ExpressionParser().parse(expr)
  }

}

private class ExpressionParser(implicit prefixes: Prefixes) extends ExpressionGenerator with RegexParsers {

  def parse(pathStr: String): SilkInput = {
    parseAll(expression, new CharSequenceReader(pathStr)) match {
      case Success(parsed, _) => parsed
      case error: NoSuccess => throw new ValidationException(error.toString)
    }
  }

  private def expression = plusTerm | multTerm | simpleTerm

  private def plusTerm = (multTerm|simpleTerm) ~ rep(sep ~ plusOrMinus ~ sep ~ (multTerm|simpleTerm)) ^^ {
    case term1 ~ list =>
      var currentTerm = term1
      for(_ ~ op ~ _ ~ term2 <- list) {
        currentTerm = numOp(currentTerm, op, term2)
      }
      currentTerm
  }

  private def multTerm = simpleTerm ~ rep(sep ~ multOrDiv ~ sep ~ simpleTerm) ^^ {
    case term1 ~ list =>
      var currentTerm = term1
      for(_ ~ op ~ _ ~ term2 <- list) {
        currentTerm = numOp(currentTerm, op, term2)
      }
      currentTerm
  }

  private def simpleTerm = bracketedTerm | constantTerm | functionTerm | variableTerm

  private def bracketedTerm: Parser[SilkInput] = "(" ~> expression <~ ")"

  private def functionTerm: Parser[SilkInput] = functionName ~ functionParameters ~ "(" ~ repsep(expression, ";") ~ ")" ^^ {
    case funcName ~ params ~ _ ~ expr ~ _ =>
      implicit val prefixes = Prefixes.empty
      implicit val resources = EmptyResourceManager
      func(Transformer(funcName, params), expr)
  }

  private def functionParameters: Parser[Map[String, String]] = opt("[" ~> repsep(functionParameter, ";") <~ "]") ^^ (params => params.getOrElse(List.empty).toMap)

  private def functionParameter = paramKey ~ ":" ~ paramValue ^^ {
    case key ~ _ ~ value => (key, value.replace("\\", ""))
  }

  private def constantTerm = constantPattern ^^ (c => constant(c))

  private def variableTerm = variable ^^ (v => path(v))

  private val constantPattern = """[\d,.]+""".r

  private val paramKey = "[^\\];:]+".r

  private val paramValue = """(\\[\];:]|[^\];:])+""".r

  private val variable = """\w[^\s\(\)]*""".r

  private val operator = """[^\s]+""".r

  private val multOrDiv = """[*/]""".r

  private val plusOrMinus = """[+-]""".r

  private val sep = """\s+""".r

  private val functionName = """\w+""".r

  override val skipWhitespace = false

}
