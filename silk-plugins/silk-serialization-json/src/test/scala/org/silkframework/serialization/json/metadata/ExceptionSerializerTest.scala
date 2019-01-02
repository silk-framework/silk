package org.silkframework.serialization.json.metadata

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.metadata.ExceptionSerializer
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import play.api.libs.json.JsValue
import play.libs.Json

class ExceptionSerializerTest extends FlatSpec with Matchers {

  //* test objects //*
  val testException1 = new NoConstructorException() // see CMEM-1472, lead to trouble with some of Sparks exceptions
  val testException2 = UnknownCauseException("no known cause") // has lead to to NPE in situations

  "The Exception Serializer" should "not fail on exceptions without certain features occur (missing string constructor)" in {

    val testSerializer = new ExceptionSerializer

    val throwable1: Option[Throwable] = try {
      val nde = testSerializer.write(testException1)(WriteContext())
      val res = testSerializer.read(nde)(ReadContext())
      Some(res)
    }
    catch {
      case _: NoSuchMethodException =>
        None // tested specifically
      case t: Throwable =>
        throw t // unknown ex
    }

    val throwable2: Option[Throwable] = try {
      val nde = testSerializer.write(testException2)(WriteContext())
      val res = testSerializer.read(nde)(ReadContext())
      Some(res)
    }
    catch {
      case _: NullPointerException =>
        throw throw new Exception(s"The ExceptionSerializer failed serializing exceptions: npe")
      case _: Throwable =>
        None
    }

    assert(throwable1.nonEmpty && throwable2.nonEmpty)
    println(throwable1.get.getMessage)
    println(throwable2.get.getMessage)

  }

  "TheExceptionJSONSerializer" should "not fail JSON serialization when exceptions without a string constructor occur" in {

    val testSerializer = new ExceptionSerializerJson

    val throwable1: Option[Throwable] = try {
      val nde = testSerializer.write(testException1)(WriteContext())
      val res = testSerializer.read(nde)(ReadContext())
      Some(res)
    }
    catch {
      case _: NoSuchMethodException =>
        throw throw new Exception(s"The ExceptionSerializer failed serializing exceptions: NoConstructorException")
      case _: Throwable => None
    }

    val throwable2: Option[Throwable] = try {
      val nde = testSerializer.write(testException2)(WriteContext())
      val res = testSerializer.read(nde)(ReadContext())
      Some(res)
    }
    catch {
      case _: NullPointerException =>
        throw throw new Exception(s"The ExceptionSerializer failed serializing exceptions: npe")
      case _: Throwable =>
        None
    }
    assert(throwable1.nonEmpty && throwable2.nonEmpty)
  }

  it should "not fail when a stacktrace is missing the class/fileName or message/cause fields" in {
    val testSerializer = new ExceptionSerializerJson
    val exceptionJSon: String =
      """       |   {
        |           "Class":"org.silkframework.serialization.json.metadata.NoConstructorException",
        |           "Message":null,
        |           "Cause":null,
        |           "StackTrace":[
        |         {
        |           "FileName":null,
        |           "ClassName":"org.silkframework.serialization.json.metadata.ExceptionSerializerTest$$anonfun$3",
        |           "MethodName":"apply$mcV$sp",
        |           "LineNumber":37
        |         },
        |         {
        |           "FileName":"ExceptionSerializerTest.scala",
        |           "ClassName":"org.silkframework.serialization.json.metadata.ExceptionSerializerTest$$anonfun$3",
        |           "MethodName":"apply","LineNumber":35
        |         },
        |         {
        |           "FileName":"ExceptionSerializerTest.scala",
        |           "ClassName":"org.silkframework.serialization.json.metadata.ExceptionSerializerTest$$anonfun$3",
        |           "MethodName":"apply","LineNumber":35
        |         },
        |         {
        |           "FileName":"Transformer.scala",
        |           "ClassName":"org.scalatest.Transformer$$anonfun$apply$1",
        |           "MethodName":"apply$mcV$sp","LineNumber":22
        |         },
        |         {
        |           "FileName":"OutcomeOf.scala",
        |           "ClassName":"org.scalatest.OutcomeOf$class",
        |           "MethodName":"outcomeOf",
        |           "LineNumber":8
        |         },
        |         {"FileName":"OutcomeOf.scala","ClassName":"org.scalatest.OutcomeOf$","MethodName":"outcomeOf","LineNumber":104},
        |         {"FileName":"Transformer.scala","ClassName":"org.scalatest.Transformer","MethodName":"apply","LineNumber":22},
        |         {"FileName":"Transformer.scala","ClassName":"org.scalatest.Transformer","MethodName":"apply","LineNumber":20},
        |         {"FileName":"FlatSpecLike.scala","ClassName":"org.scalatest.FlatSpecLike$$anon$1","MethodName":"apply","LineNumber":1647},
        |         {"FileName":"Suite.scala","ClassName":"org.scalatest.Suite$class","MethodName":"withFixture","LineNumber":1122},
        |         {"FileName":"FlatSpec.scala","ClassName":"org.scalatest.FlatSpec","MethodName":"withFixture","LineNumber":1683},
        |         {"FileName":"FlatSpecLike.scala","ClassName":"org.scalatest.FlatSpecLike$class","MethodName":"invokeWithFixture$1","LineNumber":1644},{"FileName":"FlatSpecLike.scala","ClassName":"org.scalatest.FlatSpecLike$$anonfun$runTest$1","MethodName":"apply","LineNumber":1656},
        |         {"FileName":"FlatSpecLike.scala","ClassName":"org.scalatest.FlatSpecLike$$anonfun$runTest$1","MethodName":"apply","LineNumber":1656},{"FileName":"Engine.scala","ClassName":"org.scalatest.SuperEngine","MethodName":"runTestImpl","LineNumber":306},{"FileName":"FlatSpecLike.scala","ClassName":"org.scalatest.FlatSpecLike$class","MethodName":"runTest","LineNumber":1656},{"FileName":"FlatSpec.scala","ClassName":"org.scalatest.FlatSpec","MethodName":"runTest","LineNumber":1683},{"FileName":"FlatSpecLike.scala","ClassName":"org.scalatest.FlatSpecLike$$anonfun$runTests$1","MethodName":"apply","LineNumber":1714},{"FileName":"FlatSpecLike.scala","ClassName":"org.scalatest.FlatSpecLike$$anonfun$runTests$1","MethodName":"apply","LineNumber":1714},{"FileName":"Engine.scala","ClassName":"org.scalatest.SuperEngine$$anonfun$traverseSubNodes$1$1","MethodName":"apply","LineNumber":413},{"FileName":"Engine.scala","ClassName":"org.scalatest.SuperEngine$$anonfun$traverseSubNodes$1$1","MethodName":"apply","LineNumber":401},{"FileName":"List.scala","ClassName":"scala.collection.immutable.List","MethodName":"foreach","LineNumber":392},{"FileName":"Engine.scala","ClassName":"org.scalatest.SuperEngine","MethodName":"traverseSubNodes$1","LineNumber":401},{"FileName":"Engine.scala","ClassName":"org.scalatest.SuperEngine","MethodName":"org$scalatest$SuperEngine$$runTestsInBranch","LineNumber":396},{"FileName":"Engine.scala","ClassName":"org.scalatest.SuperEngine","MethodName":"runTestsImpl","LineNumber":483},{"FileName":"FlatSpecLike.scala","ClassName":"org.scalatest.FlatSpecLike$class","MethodName":"runTests","LineNumber":1714},{"FileName":"FlatSpec.scala","ClassName":"org.scalatest.FlatSpec","MethodName":"runTests","LineNumber":1683},{"FileName":"Suite.scala","ClassName":"org.scalatest.Suite$class","MethodName":"run","LineNumber":1424},{"FileName":"FlatSpec.scala","ClassName":"org.scalatest.FlatSpec","MethodName":"org$scalatest$FlatSpecLike$$super$run","LineNumber":1683},{"FileName":"FlatSpecLike.scala","ClassName":"org.scalatest.FlatSpecLike$$anonfun$run$1","MethodName":"apply","LineNumber":1760},{"FileName":"FlatSpecLike.scala","ClassName":"org.scalatest.FlatSpecLike$$anonfun$run$1","MethodName":"apply","LineNumber":1760},{"FileName":"Engine.scala","ClassName":"org.scalatest.SuperEngine","MethodName":"runImpl","LineNumber":545},{"FileName":"FlatSpecLike.scala","ClassName":"org.scalatest.FlatSpecLike$class","MethodName":"run","LineNumber":1760},{"FileName":"FlatSpec.scala","ClassName":"org.scalatest.FlatSpec","MethodName":"run","LineNumber":1683},{"FileName":"SuiteRunner.scala","ClassName":"org.scalatest.tools.SuiteRunner","MethodName":"run","LineNumber":55},{"FileName":"Runner.scala","ClassName":"org.scalatest.tools.Runner$$anonfun$doRunRunRunDaDoRunRun$3","MethodName":"apply","LineNumber":2563},{"FileName":"Runner.scala","ClassName":"org.scalatest.tools.Runner$$anonfun$doRunRunRunDaDoRunRun$3","MethodName":"apply","LineNumber":2557},{"FileName":"List.scala","ClassName":"scala.collection.immutable.List","MethodName":"foreach","LineNumber":392},{"FileName":"Runner.scala","ClassName":"org.scalatest.tools.Runner$","MethodName":"doRunRunRunDaDoRunRun","LineNumber":2557},{"FileName":"Runner.scala","ClassName":"org.scalatest.tools.Runner$$anonfun$runOptionallyWithPassFailReporter$2","MethodName":"apply","LineNumber":1044},{"FileName":"Runner.scala","ClassName":"org.scalatest.tools.Runner$$anonfun$runOptionallyWithPassFailReporter$2","MethodName":"apply","LineNumber":1043},{"FileName":"Runner.scala","ClassName":"org.scalatest.tools.Runner$","MethodName":"withClassLoaderAndDispatchReporter","LineNumber":2722},{"FileName":"Runner.scala","ClassName":"org.scalatest.tools.Runner$","MethodName":"runOptionallyWithPassFailReporter","LineNumber":1043},{"FileName":"Runner.scala","ClassName":"org.scalatest.tools.Runner$","MethodName":"run","LineNumber":883},{"FileName":"Runner.scala","ClassName":"org.scalatest.tools.Runner","MethodName":"run","LineNumber":-1},{"FileName":"ScalaTestRunner.java","ClassName":"org.jetbrains.plugins.scala.testingSupport.scalaTest.ScalaTestRunner","MethodName":"runScalaTest2","LineNumber":131},{"FileName":"ScalaTestRunner.java","ClassName":"org.jetbrains.plugins.scala.testingSupport.scalaTest.ScalaTestRunner","MethodName":"main","LineNumber":28},{"FileName":"NativeMethodAccessorImpl.java","ClassName":"sun.reflect.NativeMethodAccessorImpl","MethodName":"invoke0","LineNumber":-2},{"FileName":"NativeMethodAccessorImpl.java","ClassName":"sun.reflect.NativeMethodAccessorImpl","MethodName":"invoke","LineNumber":62},{"FileName":"DelegatingMethodAccessorImpl.java","ClassName":"sun.reflect.DelegatingMethodAccessorImpl","MethodName":"invoke","LineNumber":43},{"FileName":"Method.java","ClassName":"java.lang.reflect.Method","MethodName":"invoke","LineNumber":498},{"FileName":"CommandLineWrapper.java","ClassName":"com.intellij.rt.execution.CommandLineWrapper","MethodName":"main","LineNumber":67}]}
        |      }
        |
    """.stripMargin
    val jsv =  play.api.libs.json.Json.parse(exceptionJSon)

    println(jsv)

    try {
      testSerializer.read(jsv)(ReadContext())
    }
    catch {
      case _: Throwable => fail("The exception serializer failed read a stacktrace from a JSon representation.")
    }



  }

}

class NoConstructorException extends Throwable

class NullsEveryWereException extends Throwable {
  override def getMessage: String = null
}