package org.silkframework.util

import scala.reflect._
import scala.reflect.runtime.{universe => ru}
import scala.reflect.api
import scala.util.Try

/**
  * Utility function for scala reflection
  */
object ScalaReflectUtils {

  //as a unified reference for undefined type
  type CA >: Any <: Any

  /**
    * tests if the implementation of a given symbol is of type def (and not val)
    * @param symbolName - name of the symbol
    * @param clazz - the class for which to check
    * @return
    */
  def implementedAsDef(symbolName: String, clazz: Class[_]): Boolean = {
    val runtimeMirror = runtime.currentMirror
    val typeSymbol = runtimeMirror.classSymbol(clazz).toType
    typeSymbol.decls.exists(d => d.name.encodedName.toString == symbolName && d.isMethod)
  }

  /**
    * Will create a TypeTag from a given (full) class name
    * @param name - the full class name
    * @return
    */
  def stringToTypeTag(name: String): Try[ru.TypeTag[CA]] = Try{
    val c = Class.forName(name)  // obtain java.lang.Class object from a string
    val mirror = ru.runtimeMirror(c.getClassLoader)  // obtain runtime mirror
    val sym = mirror.staticClass(name)  // obtain class symbol for `c`
    val tpe = sym.selfType  // obtain type object for `c`
    // create a type tag which contains above type object
    ru.TypeTag(mirror, new api.TypeCreator {
      def apply[U <: api.Universe with Singleton](m: api.Mirror[U]): U#Type = if (m eq mirror) {
        tpe.asInstanceOf[U#Type]
      }
      else {
        throw new IllegalArgumentException(s"Type tag defined in $mirror cannot be migrated to other mirrors.")
      }
    }).asInstanceOf[ru.TypeTag[CA]]
  }

  /**
    * Will extract a companion object for the given type
    * @param tt - the TypeTag identifying the type in question
    * @tparam T - the type
    * @return
    */
  def companionOf[T](implicit tt: ru.TypeTag[T]): Any  = {
    val companionMirror = ru.runtimeMirror(getClass.getClassLoader).reflectModule(ru.typeOf[T].typeSymbol.companion.asModule)
    companionMirror.instance
  }

  /**
    * Will invoke a function in an companion object of the given name with the provided arguments
    * @param functionName -  the function name
    * @param args - the arguments to be used for invocation
    * @param tt - the TypeTag identifying the companion
    * @tparam Res - the result type
    * @return - an Option of the searched result type
    */
  def invokeCompanionFunction[Typ, Res](functionName: String, args: Array[Any])(implicit tt: ru.TypeTag[Typ]): Option[Res] ={
    val c = companionOf[Typ](tt)
    val mirror = ru.runtimeMirror(c.getClass.getClassLoader).reflect(c)
    val func = mirror.symbol.typeSignature.member(ru.TermName(functionName)).asMethod
    val res = mirror.reflectMethod(func)(args:_*).asInstanceOf[Option[_]]
    res.map(_.asInstanceOf[Res])
  }
}
