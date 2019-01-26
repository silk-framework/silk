package org.silkframework.util

import scala.reflect._
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe._
import scala.reflect.api
import scala.util.{Failure, Success, Try}

/**
  * Utility function for scala reflection
  */
object ScalaReflectUtils {

  //as a unified reference for undefined type
  type CA >: Any <: Any

  /**
    * tests if the implementation of a given symbol is of type def (and not val)
    *
    * @param symbolName - name of the symbol
    * @param clazz      - the class for which to check
    * @return
    */
  def implementedAsDef(symbolName: String, clazz: Class[_]): Boolean = {
    val runtimeMirror = runtime.currentMirror
    val typeSymbol = runtimeMirror.classSymbol(clazz).toType
    typeSymbol.decls.exists(d => d.name.encodedName.toString == symbolName && d.isMethod)
  }

  /**
    * Will create a TypeTag from a given (full) class name
    *
    * @param name - the full class name
    * @return
    */
  def stringToTypeTag(name: String): Try[ru.TypeTag[CA]] = classToTypeTag(Class.forName(name))

  /**
    * Will create a TypeTag from a given class
    *
    * @param cls - the class
    * @return - the pertaining TypeTag
    */
  def classToTypeTag(cls: Class[_]): Try[ru.TypeTag[CA]] = Try {
    val mirror = ru.runtimeMirror(cls.getClassLoader) // obtain runtime mirror
    val sym = mirror.classSymbol(cls) // obtain class symbol for `c`
    val tpe = sym.selfType // obtain type object for `c`
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
    *
    * @param tt - the TypeTag identifying the type in question
    * @tparam T - the type
    * @return
    */
  def companionOf[T](implicit tt: ru.TypeTag[T]): Any = {
    val companionMirror = ru.runtimeMirror(getClass.getClassLoader).reflectModule(ru.typeOf[T].typeSymbol.companion.asModule)
    companionMirror.instance
  }

  /**
    * Extracts the member of an object by name
    *
    * @param memberName   - the name
    * @param obj          - the object
    * @param methodParams - if member is a method we might need its parameters
    * @tparam R - the expected type of the member
    */
  def retrieveClassMember[R](memberName: String, obj: Any, methodParams: Array[Any] = Array()): Option[R] =
    classToTypeTag(obj.getClass) match {
      case Success(typeTag) =>
        val symbol = typeTag.tpe.member(TermName(memberName)).asMethod
        val m = ru.runtimeMirror(obj.getClass.getClassLoader)
        val im = m.reflect(obj)
        if (symbol.isMethod) {
          Option(im.reflectMethod(symbol).apply(methodParams).asInstanceOf[R])
        }
        else { //else we assume you want a field
          Option(im.reflectField(symbol).get.asInstanceOf[R])
        }
      case Failure(f) => throw f
    }

  /**
    * Creates a new instance object of a given class
    *
    * @param cls        - the class
    * @param params     - the constructor parameters if needed
    * @param paramTypes - a type array aligning types with the given parameter objects (above)
    * @tparam R - the expected type of the new instance
    */
  def createNewInstance[R](cls: Class[_], params: Array[Object], paramTypes: Array[Class[_]] = Array()): Option[R] = {
    val types = if (params.nonEmpty && paramTypes.length != params.length) {
      params.map(_.getClass)
    }
    else {
      paramTypes
    }
    val zw = Try {
      val constructor = cls.getConstructor(types: _*)
      constructor.setAccessible(true)
      constructor.newInstance(params: _*).asInstanceOf[R]
    }
    zw.toOption
  }

  /**
    * Will invoke a function in an companion object of the given name with the provided arguments
    *
    * @param functionName -  the function name
    * @param args         - the arguments to be used for invocation
    * @param tt           - the TypeTag identifying the companion
    * @tparam Res - the result type
    * @return - an Option of the searched result type
    */
  def invokeCompanionFunction[Typ, Res](functionName: String, args: Array[Any])(implicit tt: ru.TypeTag[Typ]): Option[Res] = {
    val c = companionOf[Typ](tt)
    val mirror = ru.runtimeMirror(c.getClass.getClassLoader).reflect(c)
    val func = mirror.symbol.typeSignature.member(ru.TermName(functionName)).asMethod
    val res = mirror.reflectMethod(func)(args: _*).asInstanceOf[Option[_]]
    res.map(_.asInstanceOf[Res])
  }
}
