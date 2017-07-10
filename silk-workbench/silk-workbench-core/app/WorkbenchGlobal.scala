
import play.api.GlobalSettings
import play.api.mvc._

// This is a dummy object now and can be removed as soon as the Globals are gone
trait WorkbenchGlobal extends GlobalSettings with Rendering with AcceptExtractors {

}
