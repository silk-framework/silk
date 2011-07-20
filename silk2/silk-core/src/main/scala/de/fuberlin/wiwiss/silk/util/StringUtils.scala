package de.fuberlin.wiwiss.silk.util

object StringUtils {
  implicit def toStringUtils(str: String) = new StringUtils(str)

  object IntLiteral {
    def apply(x: Int) = x.toString

    def unapply(x: String): Option[Int] = try {
      Some(x.toInt)
    } catch {
      case _ => None
    }
  }

  object DoubleLiteral {
    def apply(x: Double) = x.toString

    def unapply(x: String): Option[Double] = try {
      Some(x.toDouble)
    } catch {
      case _ => None
    }
  }

  object BooleanLiteral {
    def apply(x: Boolean) = x.toString

    def unapply(x: String): Option[Boolean] = try {
      Some(x.toBoolean)
    } catch {
      case _ => None
    }
  }

}

class StringUtils(str: String) {
  def qGrams(q: Int): Stream[String] = {
    val boundary = "#" * (q - 1)

    (boundary + str + boundary).sliding(q).toStream
  }
}
