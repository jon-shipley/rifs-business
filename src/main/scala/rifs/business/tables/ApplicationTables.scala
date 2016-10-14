package rifs.business.tables

import javax.inject.Inject

import play.api.db.slick.DatabaseConfigProvider
import rifs.business.data.ApplicationOps
import rifs.business.restmodels.{Application, ApplicationSection}
import rifs.models.{ApplicationId, ApplicationRow, ApplicationSectionRow, OpportunityId}
import rifs.slicks.modules.{ApplicationModule, OpportunityModule}
import rifs.slicks.support.DBBinding
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class ApplicationTables @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends ApplicationModule with OpportunityModule with DBBinding with ApplicationOps {

  override val dbConfig: DatabaseConfig[JdbcProfile] = dbConfigProvider.get[JdbcProfile]

  import driver.api._

  override def byId(id: ApplicationId): Future[Option[Application]] = db.run {
    byIdWithSectionsC(id).result.map { rs =>
      val (as, ss) = rs.unzip
      as.map(a => Application(a.id, a.opportunityId, sectionsFor(a, ss)))
    }.map(_.headOption)
  }

  override def forOpportunity(opportunityId: OpportunityId): Future[Option[Application]] = db.run {
    applicationByOppIdC(opportunityId).result.map { rs =>
      val (as, ss) = rs.unzip
      as.map(a => Application(a.id, a.opportunityId, sectionsFor(a, ss)))
    }.map(_.headOption)
  }

  def sectionsFor(applicationRow: ApplicationRow, sectionRows: Seq[ApplicationSectionRow]): Seq[ApplicationSection] = {
    sectionRows.filter(_.applicationId == applicationRow.id).map { s => ApplicationSection(s.sectionNumber, s.title, s.started) }
  }

  /*
    ******************************
    * Queries and compiled queries
     */
  def byIdQ(id: Rep[ApplicationId]): ApplicationQuery = for {a <- applicationTable if a.id == id} yield a

  val byIdC = Compiled(byIdQ _)

  type ApplicationWithSectionsJoin = Query[(ApplicationTable, ApplicationSectionTable), (ApplicationRow, ApplicationSectionRow), Seq]

  def byIdWithSectionsQ(id: Rep[ApplicationId]): ApplicationWithSectionsJoin = for {
    a <- applicationTable if a.id === id
    s <- applicationSectionTable if s.applicationId === a.id
  } yield (a, s)

  val byIdWithSectionsC = Compiled(byIdWithSectionsQ _)

  def applicationByOppIdQ(opportunityId: Rep[OpportunityId]): ApplicationWithSectionsJoin = for {
    a <- applicationTable if a.opportunityId === opportunityId
    s <- applicationSectionTable if s.applicationId === a.id
  } yield (a, s)

  val applicationByOppIdC = Compiled(applicationByOppIdQ _)
}
