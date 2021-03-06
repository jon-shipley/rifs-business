package rifs.business

import com.wellfactored.playbindings.ValueClassFormats
import play.api.libs.json._
import rifs.business.models._
import rifs.business.restmodels._

package object controllers extends ValueClassFormats {
  private val dtPattern = "dd MMM yyyy HH:mm:ss"
  implicit val dtReads = Reads.jodaDateReads(dtPattern)
  implicit val dtWrites = Writes.jodaDateWrites(dtPattern)

  implicit val sectionFormat = Json.format[SectionRow]
  implicit val opportunityFormat = Json.format[OpportunityRow]
  implicit val questionFormat = Json.format[Question]

  implicit val oppDurFormat = Json.format[OpportunityDuration]
  implicit val oppValueFormat = Json.format[OpportunityValue]
  implicit val oppDescFormat = Json.format[OpportunityDescriptionSection]
  implicit val oppSummaryFormat = Json.format[OpportunitySummary]
  implicit val oppFormat = Json.format[Opportunity]

  implicit val appFormSecFormat = Json.format[ApplicationFormSection]
  implicit val appFormFormat = Json.format[ApplicationForm]

  implicit val appRowSecFormat = Json.format[ApplicationSectionRow]
  implicit val appRowFormat = Json.format[ApplicationRow]
  implicit val appSecFormat = Json.format[ApplicationSection]
  implicit val appFormat = Json.format[Application]
  implicit val appDetailFormat = Json.format[ApplicationDetail]
  implicit val appSecDetailFormat = Json.format[ApplicationSectionDetail]
}
