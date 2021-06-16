package controllers.openapi

import com.iheart.playSwagger.{NamingStrategy, SwaggerSpecGenerator}
import play.api.mvc._

import javax.inject.Inject
import scala.util.{Failure, Success}

class OpenApiController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  private implicit val cl: ClassLoader = getClass.getClassLoader

  private lazy val generator = SwaggerSpecGenerator(NamingStrategy.SnakeCase, swaggerV3 = true, domainNameSpaces = "models")

  private lazy val swagger = Action {
    generator.generate("doc.routes") match {
      case Success(value) => Ok(value)
      case Failure(ex) => throw ex
    }
  }

  def spec: Action[AnyContent] = swagger

  def ui: Action[AnyContent] = Action { implicit request =>
    Redirect("api/index.html?url=" + routes.OpenApiController.spec().absoluteURL())
  }
}