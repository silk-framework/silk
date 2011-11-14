package de.fuberlin.wiwiss.silk.server.model

import de.fuberlin.wiwiss.silk.datasource.DataSource
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.{SparqlRestriction, Path, Entity, EntityDescription}

/**
  * ${DESCRIPTION}
  *
  * <p><b>Company:</b>
  * SAT, Research Studios Austria</p>
  *
  * <p><b>Copyright:</b>
  * (c) 2011</p>
  *
  * <p><b>last modified:</b><br/>
  * $Author: $<br/>
  * $Date: $<br/>
  * $Revision: $</p>
  *
  * @author fkleedorfer
  */

/**
 * DataSource which doesn't retrieve any entities at all
 */
@Plugin(id = "nop", label = "inactive datasource", description = "DataSource which doesn't retrieve any entities at " +
  "all")
class NopDataSource extends DataSource {
  override def retrieve(entityDesc: EntityDescription, entities: Seq[String]) = {
    Traversable.empty[Entity]
  }

  override def retrievePaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    Traversable.empty[(Path, Double)]
  }

}

