package io.github.drewnoff.inapp.api

import spray.json._
import io.github.drewnoff.inapp.core._

import com.github.nscala_time.time.Imports._


trait DomainJsonFormats extends DefaultJsonFormats {

  /**
    *  Instance of the ``RootJsonFormat`` for the ``nscala time DateTime``
    */
  implicit object DateTimeFormat extends RootJsonFormat[DateTime] {
    def write(x: DateTime) = JsString(x.toString)
    def read(value: JsValue) = value match {
      case JsString(x) => DateTime.parse(x)
      case x => deserializationError("Expected DateTime as JsString, but got " + x)
    }
  }

  implicit val inappFormat = jsonFormat5(InApp)
  implicit val paidFormat = jsonObjectFormat[Paid.type]
  implicit val trialFormat = jsonObjectFormat[Trial.type]
  implicit val lapseFormat = jsonFormat1(Lapse)

  // implicit val inappReceiptFormat = jsonFormat9(InAppReceipt)
  implicit val jsReceiptFormat = jsonFormat15(JsReceipt)

  implicit object InAppReceiptJsonFormat extends JsonFormat[InAppReceipt] {
    def write(r: InAppReceipt) = Receipt.fromInAppReceipt(r).toJson
    def read(value: JsValue) = value match {
      case JsObject(x) => Receipt.toInAppReceipt(JsObject(x).convertTo[JsReceipt])
      case x           => deserializationError("Expected JsReceipt as JsObject, but got " + x)
    }
  }

  implicit object SubscriptionPeriodJsonFormat extends JsonFormat[SubscriptionPeriod] {
    def write(x: SubscriptionPeriod) = x match {
      case Paid => Paid.toJson
      case Trial => Trial.toJson
      case Lapse(idle) => Lapse(idle).toJson
    }
    def read(value: JsValue) = value match {
      case JsObject(x) => x.toList match {
        case List(("value", JsString("Paid$"))) => Paid
        case List(("value", JsString("Trial$"))) => Trial
        case List(("idle", idle)) => Lapse(idle.convertTo[Double])
      }
      case x           => deserializationError("Expected SubscriptionPeriod as JsObject, but got " + x)
    }
  }
}
