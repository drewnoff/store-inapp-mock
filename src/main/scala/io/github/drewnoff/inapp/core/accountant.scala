package io.github.drewnoff.inapp.core

import akka.actor.{Props, Actor}
import scala.util.{Try, Success, Failure}
import com.github.nscala_time.time.Imports._
import scala.util.Random.nextInt

object AccountantActor {

  case class Validate(receipt: InAppReceipt)
  class ValidateResult
  case class Valid(
    receipt: InAppReceipt,
    latest_receipt_info: InAppReceipt,
    latest_reciept: String,
  status: Int = 0) extends ValidateResult
  case class Invalid(status: Int, receipt: InAppReceipt) extends ValidateResult

  case class Restore(uid: String, pid: String)
  class RestoreResult
  case class Restored(receipts: List[InAppReceipt]) extends RestoreResult
  case object NotRestored extends RestoreResult

  def mkTransaction: String = DateTime.now.getMillis.toString + nextInt(10).toString

  def restoreReceipts(plan: List[SubscriptionPeriod], period: Int, base: InAppReceipt): List[InAppReceipt] = {
    val series = base.series
    val now = DateTime.now
    def loop(plan: List[SubscriptionPeriod], purchaseDate: DateTime,
                 acc: List[InAppReceipt]): List[InAppReceipt] =  plan match {
      case Paid::pl if purchaseDate < now => loop(
        pl,
        purchaseDate + (period).seconds,
        base.copy(
          transaction_id = mkTransaction,
          purchase_date = purchaseDate,
          expires_date = purchaseDate + (period).seconds,
          series=series
        )::acc)
      case Trial::pl if purchaseDate < now => loop(
        pl,
        purchaseDate + (period).seconds,
        base.copy(
          transaction_id = mkTransaction,
          purchase_date = purchaseDate,
          expires_date = purchaseDate + (period).seconds,
          series=series
        )::acc)
      case Lapse(idle)::pl => loop(
        pl, purchaseDate + ((period * idle).toInt).seconds, acc)
      case _ => acc
    }

    loop(plan, base.expires_date, List(base))
  }
}

class AccountantActor extends Actor with MongoStorage {
  import AccountantActor._

  def getCollection = "receipts"

  def receive: Receive = {
    case Restore(uid, pid) =>  {
      sender ! (restore(uid, pid) match {
        case Success(receipts) => Restored(receipts)
        case Failure(ex) => NotRestored
      })
    }

    case Validate(receipt) =>  {
      sender ! (validate(receipt) match {
        case Success(res) => res
        case Failure(ex) => {println(ex); ???} // TODO implement
      })
    }
  }

  def restore(uid: String, pid: String) = for {
    sub <- Try(getOne[Subscription, String, String](List(("uid", uid),
                                                         ("pid", pid)),
                                                      "subscriptions").get)
    inapp <- Try(getOne[InApp, String, String](List(("product_id", pid)),
                                               "inapps").get)
    base <- Try {
      find[InAppReceipt, String, String](
        List(("uid", uid), ("product_id", pid))
      ).sortWith(_.purchase_date < _.purchase_date) match {
        case r::_ => r
        case Nil  => {
          val transaction = mkTransaction
          val receipt = InAppReceipt(
            1, pid, transaction, transaction,
            sub.startDateTime,
            sub.startDateTime,
            sub.startDateTime + (inapp.period).seconds,
            uid
          )
          put(receipt)
          receipt
        }
      }
    }
    receipts <- Try {
      restoreReceipts(sub.plan.tail, inapp.period, base.copy(series=mkTransaction))
    }
    _ <- flushReceipts(receipts)
  } yield receipts

  def flushReceipts(receipts: List[InAppReceipt]): Try[Unit] = Try {
    for { // TODO use batch insert
      receipt <- receipts
    } yield update(receipt)(List(("transaction_id", receipt.transaction_id)))
  }

  def validate(receipt: InAppReceipt) =  for {
    inapp <- Try(getOne[InApp, String, String](
      List(("product_id", receipt.product_id)), "inapps").get)
    subscription <- Try(getOne[Subscription, String, String](
      List(("uid", receipt.uid), ("pid", receipt.product_id)),
      "subscriptions").get)
    receipts <- Try(
      find[InAppReceipt, String, String](
        List(("original_transaction_id", receipt.original_transaction_id),
          ("series", receipt.series))
      ).sortWith(_.purchase_date > _.purchase_date))
    } yield Valid(
      receipt,
      {val restored = restoreReceipts(subscription.plan.drop(receipts.length), inapp.period, receipts.head)
        restored.head},
      "")
}
