package rifs.business.restmodels

import rifs.business.models.{ApplicationFormId, OpportunityId}

case class ApplicationFormSection(sectionNumber: Int, title: String, started: Boolean)

case class ApplicationForm(id: ApplicationFormId, opportunityId: OpportunityId, sections: Seq[ApplicationFormSection])