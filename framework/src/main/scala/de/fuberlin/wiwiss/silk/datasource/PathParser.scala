package de.fuberlin.wiwiss.silk.datasource

import scala.util.parsing.combinator._

class PathParser extends RegexParsers
{
    def path = variable ~ (forwardOperator | backwardOperator | filterOperator)

    def variable = "?" ~> variableRegex
    def forwardOperator = "/" ~> property
    def backwardOperator = "\\" ~> property
    def filterOperator= "[" ~> (langFilterExp | propFilterExp) <~ "]"
    def langFilterExp = "@lang " ~> compOperator <~ " " ~> value
    def propFilterExp = property <~ " " ~> compOperator <~ " " ~> value

    def variableRegex = """[^\\/\[\]]*""".r
    def property = """[^\\/\[\]]*""".r
    def value = """[^\\/\[\]]*""".r
    def compOperator = ">" | "<" | ">=" | "<=" | "=" | "!="
}
