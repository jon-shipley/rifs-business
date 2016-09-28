package ifs.data.restmodels

import ifs.models.OpportunityId

case class OpportunityDescriptionSection(sectionNumber:Int, title: String, paragraphs: Seq[String])

case class OpportunityValue(amount: BigDecimal, unit: String)

case class OpportunityDuration(duration: Int, units: String)

case class Opportunity(
                        id: OpportunityId,
                        title: String,
                        startDate: String,
                        duration: Option[OpportunityDuration],
                        value: OpportunityValue,
                        description: Seq[OpportunityDescriptionSection]
                      )
