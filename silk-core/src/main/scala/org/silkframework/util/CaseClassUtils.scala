package org.silkframework.util

import java.lang.reflect.Field

/**
  * Utility functions on case classes.
  */
object CaseClassUtils {

  /**
    * Pretty prints case classes.
    */
  def prettyPrint(a: Any, indentation: Int = 0): String = {
    // Recursively get all the fields; this will grab vals declared in parents of case classes.
    def getFields(cls: Class[_]): List[Field] =
      Option(cls.getSuperclass).map(getFields).getOrElse(Nil) ++
        cls.getDeclaredFields.toList.filterNot(f =>
          f.isSynthetic || java.lang.reflect.Modifier.isStatic(f.getModifiers))

    val indent = " " * indentation

    a match {
      // Make Strings look similar to their literal form.
      case s: String =>
        '"' + Seq("\n" -> "\\n", "\r" -> "\\r", "\t" -> "\\t", "\"" -> "\\\"", "\\" -> "\\\\").foldLeft(s) {
          case (acc, (c, r)) => acc.replace(c, r) } + '"'
      case xs: Seq[_] =>
        xs.map(e => prettyPrint(e, indentation)).toString
      case xs: Array[_] =>
        s"Array(${xs.map(e => prettyPrint(e, indentation)) mkString ", "})"
      // This covers case classes.
      case p: Product =>
        s"\n$indent${p.productPrefix}(\n${
          (getFields(p.getClass) map { f =>
            f setAccessible true
            s"$indent  ${f.getName} = ${prettyPrint(f.get(p), indentation + 4)}"
          }) mkString ",\n"
        }\n$indent)"
      // General objects and primitives end up here.
      case q =>
        Option(q).map(_.toString).getOrElse("¡null!")
    }
  }

}
