package rifs.business.notifications

import javax.inject.Inject

import com.google.inject.ImplementedBy
import play.api.libs.mailer.{Email, MailerClient}
import rifs.business.data.ApplicationOps
import rifs.business.models.{ApplicationFormRow, ApplicationId, OpportunityRow}

import scala.concurrent.{ExecutionContext, Future}

object Notifications {
  trait NotificationId {
    def id: String
  }

  case class EmailId(id: String) extends NotificationId
}

@ImplementedBy(classOf[EmailSenderImpl])
trait EmailSender {
  def send(email: Email): String
}

class EmailSenderImpl @Inject()(mailerClient: MailerClient) extends EmailSender{
  override def send(email: Email): String = mailerClient.send(email)
}

@ImplementedBy(classOf[EmailNotifications])
trait NotificationService {

  import Notifications._

  def notifyPortfolioManager(applicationFormId: ApplicationId, from: String, to: String): Future[Option[NotificationId]]
}

class EmailNotifications @Inject()(sender: EmailSender, applications: ApplicationOps)(implicit ec: ExecutionContext) extends NotificationService {

  import Notifications._
  import play.api.libs.mailer._

  override def notifyPortfolioManager(applicationId: ApplicationId, from: String, to: String): Future[Option[EmailId]] = {

    def createEmail(appForm: ApplicationFormRow, opportunity: OpportunityRow) = {
      val emailSubject = "Application submitted"
      val applicantEMail = to
      val applicantLastName = "?"
      val applicantFirstName = "Eric"

      val text = emails.txt.applicationSubmittedToPortfolioMgr(
        portFolioMgrName = "Portfolio",
        applicantTitle = "Mr",
        applicantLastName,
        applicantFirstName,
        applicantOrg = "Association of Medical Research Charities",
        applicationRefNum = appForm.id.id.toString,
        opportunityRefNumber = appForm.opportunityId.id.toString,
        opportunityTitle = opportunity.title,
        submissionLink = "http://todo.link"
      )

      Email(
        subject = emailSubject,
        from = from,
        to = Seq(s"$applicantFirstName $applicantLastName <$applicantEMail>"),
        // adds attachment
        attachments = Nil,
        // sends text, HTML or both...
        bodyText = Some(text.body),
        bodyHtml = None
      )
    }

    applications.gatherDetails(applicationId).map {
      _.map(d => EmailId(sender.send(createEmail(d.form, d.opp))))
    }
  }
}