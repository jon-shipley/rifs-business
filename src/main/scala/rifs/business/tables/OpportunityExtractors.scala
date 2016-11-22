package rifs.business.tables

import cats.instances.option._
import cats.syntax.cartesian._
import rifs.business.models.{OpportunityRow, SectionRow}
import rifs.business.restmodels.{Opportunity, OpportunityDescriptionSection, OpportunityDuration, OpportunityValue}

object OpportunityExtractors {
  /**
    * Accepts the results of the left join across the Opportunity and Section tables and
    * builds them up into the REST model.
    *
    * @param joinResults the raw results from the left joins.
    */
  def extractOpportunities(joinResults: Seq[(OpportunityRow, Option[SectionRow])]): Set[Opportunity] = {
    val (oppRows, sectionRows) = unzipO(joinResults)

    oppRows.map { o =>
      Opportunity(
        o.id,
        o.title,
        o.startDate,
        durationFor(o),
        OpportunityValue(o.value, o.valueUnits),
        sectionsFor(o, sectionRows.toSet))
    }.toSet
  }

  def sectionsFor(o: OpportunityRow, ss: Set[SectionRow]): Set[OpportunityDescriptionSection] = {
    ss.filter(_.opportunityId == o.id).map(s => buildSection(s))
  }

  def buildSection(s: SectionRow): OpportunityDescriptionSection = {
    OpportunityDescriptionSection(s.sectionNumber, s.title, s.text)
  }

  def durationFor(opp: OpportunityRow): Option[OpportunityDuration] = {
    (opp.duration |@| opp.durationUnits).map(OpportunityDuration.apply)
  }

  /**
    * useful for extracting from leftJoin results where the second item in the pair is represented as
    * an Option
    */
  def unzipO[A, B](join: Seq[(A, Option[B])]): (Seq[A], Seq[B]) = join.unzip match {
    case (as, bs) => (as, bs.flatten)
  }
}
