import javax.inject.Inject
import org.silkframework.workbench.filters.HttpCaching
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.gzip.GzipFilter

/**
  * Filter layer for HTTP requests.
  */
class Filters @Inject()(gzipFilter: GzipFilter,
                        cachingFilter: HttpCaching
                        ) extends HttpFilters {

  override def filters: Seq[EssentialFilter] = Seq(gzipFilter, cachingFilter)
}
