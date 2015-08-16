package io.github.drewnoff.inapp.core

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.ActorSystem
import org.scalatest.WordSpecLike
import org.scalatest.BeforeAndAfter

import java.util.UUID
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import com.novus.salat.global._

import akka.util.Timeout

import com.github.nscala_time.time.Imports._



class AccountantActorSpec extends TestKit(ActorSystem()) with WordSpecLike with CoreActors with Core with ImplicitSender with BeforeAndAfter {
  import AccountantActor._

  com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers()

  val client = MongoClient("localhost", 27017)
  val db = client("inapps")
  val collection = db("receipts")
  val subsCollection = db("subscriptions")
  val inappCollection = db("inapps")
  val userCollection = db("users")

  val inappId = "io.github.drewnoff.inapp.1month"

  val inapp = InApp(inappId, s"Title $inappId",
                    s"Author $inappId", true, 10)
  val user = User(UUID.randomUUID())
  val subsPlan = List(Trial, Paid, Paid, Lapse(0.5), Paid)
  val sub = Subscription(user._id.toString, inappId, subsPlan, DateTime.now)
  val origTransaction = mkTransaction
  val receipt = InAppReceipt(
    1, inappId, origTransaction, origTransaction,
    sub.startDateTime,
    sub.startDateTime,
    sub.startDateTime + (inapp.period).seconds,
    user._id.toString
  )

  before {
    subsCollection.drop()
    userCollection.drop()
    inappCollection.drop()
    collection.drop()
    insertInApp(inapp)
    insertUser(user)
  }

  "An Accountant Actor" when {

    "Restore" should {

      "accept original receipt to be created while restoring new subscription" in {
        insertSubscription(sub)
        accountant ! Restore(user._id.toString, inappId)
        assert(expectMsgPF(){
          case Restored(receipts) => receipts.length
        } === 1)
      }

      "accept original receipt to be returned while restoring existing subscription" in {
        insertSubscription(sub)
        insertReceipt(receipt)
        accountant ! Restore(user._id.toString, inappId)
        assert(expectMsgPF(){
          case Restored(receipts) => receipts map(_.transaction_id)
        } === List(receipt.transaction_id))
      }

    }

    "accept all used receipt to be restored" in {
      insertSubscription(
        sub.copy(startDateTime=DateTime.now - (inapp.period * 2).seconds)
      )
      accountant ! Restore(user._id.toString, inappId)
      assert(expectMsgPF(){
        case Restored(receipts) => receipts.length
      } === 3)
    }

    "accept all used receipt to be restored while in lapse period" in {
      insertSubscription(
        sub.copy(startDateTime=DateTime.now - (inapp.period * 3).seconds)
      )
      accountant ! Restore(user._id.toString, inappId)
      assert(expectMsgPF(){
        case Restored(receipts) => receipts.length
      } === 3)
    }

    "accept all used receipt to be restored after lapse period" in {
      insertSubscription(
        sub.copy(startDateTime=DateTime.now - (inapp.period * 4).seconds)
      )
      accountant ! Restore(user._id.toString, inappId)
      assert(expectMsgPF(){
        case Restored(receipts) => receipts.length
      } === 4)
    }

    "returns all restored receipt with proper purchase dates" in {
      val newsub = sub.copy(startDateTime=DateTime.now - (inapp.period * 2).seconds)
      insertSubscription(newsub)
      accountant ! Restore(user._id.toString, inappId)
      assert(expectMsgPF(){
        case Restored(receipts) => receipts map(_.purchase_date)
      } === List(newsub.startDateTime + (inapp.period * 2).seconds,
        newsub.startDateTime + (inapp.period).seconds,
        newsub.startDateTime))
    }

    "returns all restored receipt after lapse with proper purchase dates" in {
      val newsub = sub.copy(startDateTime=DateTime.now - (inapp.period * 4).seconds)
      insertSubscription(newsub)
      accountant ! Restore(user._id.toString, inappId)
      assert(expectMsgPF(){
        case Restored(receipts) => receipts map(_.purchase_date)
      } === List(
        newsub.startDateTime + ((inapp.period * 3.5).toInt).seconds,
        newsub.startDateTime + (inapp.period * 2).seconds,
        newsub.startDateTime + (inapp.period).seconds,
        newsub.startDateTime))
    }

    "returns all restored receipt after lapse with proper expires dates" in {
      val newsub = sub.copy(startDateTime=DateTime.now - (inapp.period * 4).seconds)
      insertSubscription(newsub)
        accountant ! Restore(user._id.toString, inappId)
        assert(expectMsgPF(){
          case Restored(receipts) => receipts map(_.expires_date)
        } === List(
          newsub.startDateTime + ((inapp.period * 4.5).toInt).seconds,
          newsub.startDateTime + (inapp.period * 3).seconds,
          newsub.startDateTime + (inapp.period * 2).seconds,
          newsub.startDateTime + (inapp.period).seconds))
    }

    "should creates new receipts in the DB while restoring them" in {
      val newsub = sub.copy(startDateTime=DateTime.now - (inapp.period * 4).seconds)
      insertSubscription(newsub)
        accountant ! Restore(user._id.toString, inappId)
        assert(expectMsgPF(){
          case Restored(receipts) => receipts.length
        } === getReceipts.length)
    }

    "should creates new receipts in the DB while restoring except for original receipt" in {
      val newsub = sub.copy(startDateTime=DateTime.now - (inapp.period * 2).seconds)
      insertSubscription(newsub)
      insertReceipt(receipt.copy(
        purchase_date = newsub.startDateTime,
        original_purchase_date = newsub.startDateTime,
        expires_date = newsub.startDateTime + (inapp.period).seconds
      ))
      accountant ! Restore(user._id.toString, inappId)
      assert(expectMsgPF(){
        case Restored(receipts) => receipts.length
      } === 3)
      assert(getReceipts.length === 3)
    }

    "should create new receipt in the DB while restoring already existing except for original one." in {
      val newsub = sub.copy(startDateTime=DateTime.now - (inapp.period * 2).seconds)
      insertSubscription(newsub)
      insertReceipt(receipt.copy(
        purchase_date = newsub.startDateTime,
        original_purchase_date = newsub.startDateTime,
        expires_date = newsub.startDateTime + (inapp.period).seconds
      ))
      insertReceipt(receipt.copy(
        transaction_id = mkTransaction,
        purchase_date = newsub.startDateTime + (inapp.period).seconds,
        original_purchase_date = newsub.startDateTime,
        expires_date = newsub.startDateTime + (inapp.period * 2).seconds,
        series = mkTransaction
      ))
      accountant ! Restore(user._id.toString, inappId)
      assert(expectMsgPF(){
        case Restored(receipts) => receipts.length
      } === 3)
      assert(getReceipts.length === 4)
    }

    "Validate" should {

      "accept original receipt to be retured as latest_receipt at the beginning of subscription " in {
        insertSubscription(sub)
        accountant ! Restore(user._id.toString, inappId)
        val origReceipt = expectMsgPF(){
          case Restored(receipts) => receipts.head
        }
        accountant ! Validate(origReceipt)
        expectMsg(Valid(origReceipt, origReceipt, "", 0))
      }

      "accept last receipt to be retured as latest_receipt while verifying original receipt " in {
        insertSubscription(
          sub.copy(startDateTime=DateTime.now - (inapp.period * 2).seconds)
        )
        accountant ! Restore(user._id.toString, inappId)
        val receipts = expectMsgPF(){
          case Restored(receipts) => receipts
        }
        accountant ! Validate(receipts.reverse.head)
        expectMsg(Valid(receipts.reverse.head, receipts.head, "", 0))
      }

      "accept last receipt from last restored series to be retured as latest_receipt while verifying original receipt " in {
        insertSubscription(
          sub.copy(startDateTime=DateTime.now - (inapp.period * 2).seconds)
        )
        accountant ! Restore(user._id.toString, inappId)
        val receipts = expectMsgPF(){
          case Restored(receipts) => receipts
        }
        val origReceipt = receipts.reverse.head
        accountant ! Validate(origReceipt)
        expectMsg(Valid(origReceipt, receipts.head, "", 0))

        accountant ! Restore(user._id.toString, inappId)
        val rsReceipts = expectMsgPF(){
          case Restored(receipts) => receipts
        }
        val rsOrigReceipt = rsReceipts.reverse.head
        assert(rsOrigReceipt.transaction_id === origReceipt.transaction_id)
        assert(rsOrigReceipt.series !== origReceipt.series)

        accountant ! Validate(receipts.tail.head)
        expectMsg(Valid(receipts.tail.head, receipts.head, "", 0))

        accountant ! Validate(rsReceipts.tail.head)
        expectMsg(Valid(rsReceipts.tail.head, rsReceipts.head, "", 0))

        accountant ! Validate(rsOrigReceipt)
        expectMsg(Valid(rsOrigReceipt, rsReceipts.head, "", 0))

        accountant ! Validate(origReceipt)
        expectMsg(Valid(origReceipt, receipts.head, "", 0))
        // TODO split on to different test cases
      }

      "accept last receipt to be created and return as latest_receipt while verifying receipt of yet unrestored or unverified series" in {
        val now = DateTime.now
        insertSubscription(
          sub.copy(startDateTime=now - (inapp.period * 2).seconds)
        )
        val origReceipt = receipt.copy(
          purchase_date = now - (inapp.period * 2).seconds,
          original_purchase_date = now - (inapp.period * 2).seconds,
          expires_date = now - (inapp.period).seconds)
        insertReceipt(origReceipt)
        accountant ! Validate(origReceipt)
        assert(expectMsgPF(){
          case Valid(rcp, latestrcp, _, _) =>
            List(rcp.purchase_date, latestrcp.purchase_date)
        } === List(now - (inapp.period * 2).seconds, now))
      }
    }
  }

  def insertInApp(inapp: InApp) {
    inappCollection.insert(grater[InApp].asDBObject(inapp))
  }

  def insertUser(user: User) {
    userCollection.insert(grater[User].asDBObject(user))
  }

  def insertSubscription(sub: Subscription) {
    subsCollection.insert(grater[Subscription].asDBObject(sub))
  }

  def insertReceipt(receipt: InAppReceipt) {
    collection.insert(grater[InAppReceipt].asDBObject(receipt))
  }

  def getReceipts(): List[InAppReceipt] = {
    collection.find().toList.map(grater[InAppReceipt].asObject(_))
  }
}
