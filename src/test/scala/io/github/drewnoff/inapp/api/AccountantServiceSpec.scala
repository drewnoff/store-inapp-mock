package io.github.drewnoff.inapp.api

import org.scalatest.WordSpec
import org.scalatest.BeforeAndAfter
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import akka.actor.ActorRef
import spray.routing.Directives
import spray.http.HttpResponse
import spray.http.StatusCodes._
import spray.testkit.ScalatestRouteTest
import spray.routing.HttpService

import io.github.drewnoff.inapp.core._
import io.github.drewnoff.inapp.core.{CoreActors, Core}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import com.novus.salat.global._

import java.util.UUID
import com.github.nscala_time.time.Imports._



class AccountantServiceSpec extends WordSpec with HttpService with ScalatestRouteTest with CoreActors with Core with AccountantRoute with BeforeAndAfter {

  import AccountantActor._

  def actorRefFactory = system
  implicit def executionContext = global

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
    insertInApp(inapp)
    insertUser(user)
    insertSubscription(sub)
    insertReceipt(receipt)
  }

  after {
    subsCollection.drop()
    userCollection.drop()
    inappCollection.drop()
    collection.drop()
  }

  val uri = "/api/v1/inapps/receipts"

  "Accountant API" when {
    "using routing infrastructure" should {
      "support the most simple and direct route" in {
        Get(uri) ~> complete(HttpResponse()) ~> check {
          assert(response === HttpResponse())
        }
      }
    }

    s"POST $uri/restore" should {

      "return OK" in {
        insertSubscription(sub)
        Post(s"$uri/restore", Restore(user._id.toString, inappId)) ~> route ~> check {
          assert(response.status === OK)
        }
      }

      "return Restored with first receipt at the very beginning" in {
        insertSubscription(sub)
        Post(s"$uri/restore", Restore(user._id.toString, inappId)) ~> route ~> check {
          assert(response.entity !== None)
          val res = responseAs[Restored]
          // assert(res === Restored) // TODO
        }
      }
    }

    s"POST $uri/verifyReceipt" should {

      "return OK" in {
        Post(s"$uri/verifyReceipt", Validate(receipt)) ~> route ~> check {
          assert(response.status === OK)
        }
      }

      // "return Restored with first receipt at the very beginning" in {
      //   insertSubscription(sub)
      //   Post(s"$uri/restore", Restore(user._id.toString, inappId)) ~> route ~> check {
      //     assert(response.entity !== None)
      //     val res = responseAs[Restored]
      //     // assert(res === Restored) // TODO
      //   }
      // }
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
}
