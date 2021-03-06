package rifs.business.controllers

import play.api.libs.json.Json

case class ErrorResult(errorKey: String, arguments: Seq[String], fieldName: Option[String] = None,
                       fieldRejectedValue: Option[String] = None, details: Option[String] = None)

object ErrorResult {
  implicit val errorFormat = Json.format[ErrorResult]

  def apply(errorKey: String, status: Int): ErrorResult = ErrorResult(errorKey, Seq("", "", status.toString))

  def apply(error: RuntimeException): ErrorResult = {
    ErrorResult(error.getClass.getName, Nil, None, None, Some(error.getMessage))
  }
}
