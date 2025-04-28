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
    val indent = " " * indentation

    def formatField(p: Product, field: Field) = {
      field.setAccessible(true)
      s"\n$indent  ${field.getName} = ${prettyPrint(field.get(p), indentation + 4)}"
    }

    a match {
      // Make Strings look similar to their literal form.
      case s: String =>
        s""""${Seq("\n" -> "\\n", "\r" -> "\\r", "\t" -> "\\t", "\"" -> "\\\"", "\\" -> "\\\\").foldLeft(s) {
          case (acc, (c, r)) => acc.replace(c, r) }}""""
      case xs: Seq[_] =>
        xs.map(e => prettyPrint(e, indentation)).toString
      case xs: Array[_] =>
        s"Array(${xs.map(e => prettyPrint(e, indentation)) mkString ", "})"
      // This covers case classes.
      case p: Product =>
        val fields = getFields(p.getClass)
        if(fields.isEmpty) {
          s"${p.productPrefix}"
        } else {
          s"\n$indent${p.productPrefix}(${fields.map(formatField(p, _)).mkString(",")}\n$indent)"
        }
      // General objects and primitives end up here.
      case q =>
        Option(q).map(_.toString).getOrElse("¡null!")
    }
  }

  /**
    *   Gets all fields of a case class
    */
  def getFields(cls: Class[_]): List[Field] =
    Option(cls.getSuperclass).map(getFields).getOrElse(Nil) ++
      cls.getDeclaredFields.toList.filterNot(f =>
        f.isSynthetic || java.lang.reflect.Modifier.isStatic(f.getModifiers))

}
