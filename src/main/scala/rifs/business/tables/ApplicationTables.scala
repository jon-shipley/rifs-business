package rifs.business.tables

import javax.inject.Inject

import cats.data.OptionT
import cats.instances.future._
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.JsObject
import rifs.business.controllers.JsonHelpers
import rifs.business.data.{ApplicationDetails, ApplicationOps}
import rifs.business.models._
import rifs.business.restmodels.{Application, ApplicationSection}
import rifs.business.slicks.modules.{ApplicationFormModule, ApplicationModule, OpportunityModule, PgSupport}
import rifs.business.slicks.support.DBBinding

import scala.concurrent.{ExecutionContext, Future}

class ApplicationTables @Inject()(val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends ApplicationOps
    with ApplicationModule
    with ApplicationFormModule
    with OpportunityModule
    with DBBinding
    with PgSupport {

  import driver.api._

  override def byId(id: ApplicationId): Future[Option[ApplicationRow]] = db.run(applicationTable.filter(_.id === id).result.headOption)

  override def gatherDetails(id: ApplicationId): Future[Option[ApplicationDetails]] = db.run {
    val q = for {
      a <- applicationTable.filter(_.id === id)
      f <- applicationFormTable.filter(_.id === a.applicationFormId)
      o <- opportunityTable.filter(_.id === f.opportunityId)
    } yield (a, f, o)

    q.result.headOption.map(_.map(ApplicationDetails.tupled))
  }

  override def forForm(applicationFormId: ApplicationFormId): Future[Option[ApplicationRow]] = {
    val appFormF = db.run(applicationFormTable.filter(_.id === applicationFormId).result.headOption)

    for {
      _ <- OptionT(appFormF)
      app <- OptionT.liftF(fetchOrCreate(applicationFormId))
    } yield ApplicationRow(app.id, app.applicationFormId, app.personalReference)
  }.value

  override def application(applicationId: ApplicationId): Future[Option[Application]] = db.run {
    applicationWithSectionsC(applicationId).result
  }.map { ps =>
    val (as, ss) = ps.unzip
    as.map(a => buildApplication(a, ss.flatten)).headOption
  }

  override def delete(id: ApplicationId): Future[Unit] = db.run {
    for {
      _ <- applicationSectionTable.filter(_.applicationId === id).delete
      _ <- applicationTable.filter(_.id === id).delete
    } yield ()
  }

  override def deleteAll: Future[Unit] = db.run {
    for {
      _ <- applicationSectionTable.delete
      _ <- applicationTable.delete
    } yield ()
  }

  def applicationWithSectionsQ(id: Rep[ApplicationId]) =
    (applicationTable joinLeft applicationSectionTable on (_.id === _.applicationId)).filter(_._1.id === id)

  val applicationWithSectionsC = Compiled(applicationWithSectionsQ _)

  def applicationWithSectionsForFormQ(id: Rep[ApplicationFormId]) =
    (applicationTable joinLeft applicationSectionTable on (_.id === _.applicationId)).filter(_._1.applicationFormId === id)

  val applicationWithSectionsForFormC = Compiled(applicationWithSectionsForFormQ _)

  private def fetchOrCreate(applicationFormId: ApplicationFormId): Future[Application] = {
    db.run(applicationWithSectionsForFormC(applicationFormId).result).flatMap {
      case Seq() => createApplicationForForm(applicationFormId).map { id => Application(id, applicationFormId, None, Seq()) }
      case ps =>
        val (as, ss) = ps.unzip
        Future.successful(as.map(a => buildApplication(a, ss.flatten)).head)
    }
  }

  private def buildApplication(app: ApplicationRow, secs: Seq[ApplicationSectionRow]): Application = {
    val sectionOverviews: Seq[ApplicationSection] = secs.map { s =>
      ApplicationSection(s.sectionNumber, s.answers, s.completedAt)
    }

    Application(app.id, app.applicationFormId, app.personalReference, sectionOverviews)
  }

  private def createApplicationForForm(applicationFormId: ApplicationFormId): Future[ApplicationId] = db.run {
    (applicationTable returning applicationTable.map(_.id)) += ApplicationRow(ApplicationId(0), applicationFormId, None)
  }

  override def fetchAppWithSection(id: ApplicationId, sectionNumber: Int): Future[Option[(ApplicationRow, Option[ApplicationSectionRow])]] = db.run {
    appWithSectionC(id, sectionNumber).result.headOption
  }

  override def fetchSection(id: ApplicationId, sectionNumber: Int): Future[Option[ApplicationSectionRow]] = db.run {
    appSectionC(id, sectionNumber).result.headOption
  }

  override def fetchSections(id: ApplicationId): Future[Set[ApplicationSectionRow]] = db.run(appSectionsC(id).result).map(_.toSet)

  override def saveSection(id: ApplicationId, sectionNumber: Int, answers: JsObject, completedAt: Option[DateTime] = None): Future[Int] = {
    fetchAppWithSection(id, sectionNumber).flatMap {
      case Some((app, Some(section))) => if (areDifferent(section.answers, answers) || completedAt.isDefined) {
        db.run(appSectionC(id, sectionNumber).update(section.copy(answers = answers, completedAt = completedAt)))
      } else {
        Future.successful(1)
      }
      case Some((app, None)) => db.run(applicationSectionTable += ApplicationSectionRow(ApplicationSectionId(0), id, sectionNumber, answers, completedAt))
      case None => Future.successful(0)
    }
  }

  override def clearSectionCompletedDate(id: ApplicationId, sectionNumber: Int): Future[Int] = {
    fetchAppWithSection(id, sectionNumber).flatMap {
      case Some((app, Some(section))) => db.run(appSectionC(id, sectionNumber).update(section.copy(completedAt = None)))
      case _ => Future.successful(0)
    }
  }

  def areDifferent(obj1: JsObject, obj2: JsObject): Boolean = {
    val flat1 = JsonHelpers.flatten("", obj1).filter { case (_, v) => v.trim != "" }
    val flat2 = JsonHelpers.flatten("", obj2).filter { case (_, v) => v.trim != "" }
    flat1 != flat2
  }

  def joinedAppWithSection(id: Rep[ApplicationId], sectionNumber: Rep[Int]) = for {
    as <- applicationTable joinLeft applicationSectionTable on ((a, s) => a.id === s.applicationId && s.sectionNumber === sectionNumber) if as._1.id === id
  } yield as

  def appWithSectionQ(id: Rep[ApplicationId], sectionNumber: Rep[Int]) = joinedAppWithSection(id, sectionNumber)

  lazy val appWithSectionC = Compiled(appWithSectionQ _)

  override def deleteSection(id: ApplicationId, sectionNumber: Int): Future[Int] = db.run {
    appSectionC(id, sectionNumber).delete
  }

  override def submit(id: ApplicationId): Future[Option[SubmittedApplicationRef]] = {
    // dummy method
    play.api.Logger.info(s"Dummy application submission for $id")
    byId(id).flatMap(appRow => Future.successful(appRow.map(_.id)))
  }

  def appSectionQ(id: Rep[ApplicationId], sectionNumber: Rep[Int]) = applicationSectionTable.filter(a => a.applicationId === id && a.sectionNumber === sectionNumber)

  lazy val appSectionC = Compiled(appSectionQ _)

  def appSectionsQ(id: Rep[ApplicationId]) = applicationSectionTable.filter(_.applicationId === id)

  lazy val appSectionsC = Compiled(appSectionsQ _)

  override def updatePersonalReference(id: SubmittedApplicationRef, reference: Option[String]): Future[Int] = {
    db.run( applicationTable.filter(_.id === id).map(_.personalReference).update(reference) )
  }
}

