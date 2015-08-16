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

import io.github.drewnoff.inapp.core.{
  InApp, User, Subscription, SubscriptionPeriod,
  Trial, Paid, Lapse, SchedulerActor}
import io.github.drewnoff.inapp.core.{CoreActors, Core}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import com.novus.salat.global._

import java.util.UUID
import com.github.nscala_time.time.Imports._


class SchedulerServiceSpec extends WordSpec with HttpService with ScalatestRouteTest with CoreActors with Core with SchedulerRoute with BeforeAndAfter {

  import SchedulerActor._

  def actorRefFactory = system
  implicit def executionContext = global

  val client = MongoClient("localhost", 27017)
  val db = client("inapps")
  val collection = db("subscriptions")
  val inappCollection = db("inapps")
  val userCollection = db("users")

  val inappId = "io.github.drewnoff.inapp.1month"
  val inapp = InApp(inappId, s"Title $inappId",
                    s"Author $inappId", true, 5)
  val user = User(UUID.randomUUID())
  val plan = List[SubscriptionPeriod](Trial, Paid, Paid, Lapse(2.0), Paid)

  before {
    collection.drop()
    userCollection.drop()
    inappCollection.drop()

    insertInApp(inapp)
    insertUser(user)
  }

  val uri = "/api/v1/inapps/subs"

  "Scheduler API" when {
    "using routing infrastructure" should {
      "support the most simple and direct route" in {
        Get(uri) ~> complete(HttpResponse()) ~> check {
          assert(response === HttpResponse())
        }
      }
    }

    s"POST $uri/subscribe" should {


      "return OK" in {
        Post(s"$uri/subscribe", Subscribe(user._id, inappId, plan)) ~> route ~> check {
          assert(response.status === OK)
        }
      }

      "return Subscribed" in {
        Post(s"$uri/subscribe", Subscribe(user._id, inappId, plan)) ~> route ~> check {
          assert(response.entity !== None)
          val res = responseAs[Subscribed.type]
          assert(res === Subscribed)
        }
      }

      // TODO add already subscribed case
      "return Subscribed(AlreadySubscribed)" in {
        insertSubscription(Subscription(user._id.toString, inappId,
          Nil, DateTime.now))
        Post(s"$uri/subscribe", Subscribe(user._id, inappId, plan)) ~> route ~> check {
          assert(response.entity !== None)
          val res = responseAs[Subscribed.type]
          assert(res === Subscribed)
        }
      }

      
      // TODO add not subscribed case

    //   "insert subscripbion to the DB" in {
    //     Post(uri, ???) ~> route ~> check {
    //       assert(response.status === OK)
    //       val sub = getSubscription(???)
    //       assert(sub === ???)
    //     }
    //   }
    }
  }

  def insertInApp(inapp: InApp) {
    inappCollection.insert(grater[InApp].asDBObject(inapp))
  }

  def insertUser(user: User) {
    userCollection.insert(grater[User].asDBObject(user))
  }

  def insertSubscription(sub: Subscription) {
    collection.insert(grater[Subscription].asDBObject(sub))
  }

  def getSubscription(uid: UUID, pid: String): Subscription = {
    val dbObject = collection.findOne(MongoDBObject("pid" -> pid, "uid" -> uid))
    grater[Subscription].asObject(dbObject.get)
  }
}
