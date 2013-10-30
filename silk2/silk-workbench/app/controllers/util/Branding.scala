package controllers.util

import play.api.mvc.{Action, Controller}
import models.WorkbenchConfig
import java.io.{BufferedInputStream, FileInputStream}

object Branding extends Controller {

  def logo = Action {
    val imgStream = new BufferedInputStream(new FileInputStream(WorkbenchConfig.get.logo))
    val bytes = scala.Stream.continually(imgStream.read).takeWhile(_ != -1).map(_.toByte).toArray
    Ok(bytes).as("image/png")
  }

  def aboutDialog = Action {
    Ok(views.html.aboutDialog())
  }

}
