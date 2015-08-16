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



class SchedulerActorSpec extends TestKit(ActorSystem()) with WordSpecLike with CoreActors with Core with ImplicitSender with BeforeAndAfter {
  import SchedulerActor._

  com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers()

  val client = MongoClient("localhost", 27017)
  val db = client("inapps")
  val collection = db("subscriptions")
  val inappCollection = db("inapps")
  val userCollection = db("users")

  val inappId = "io.github.drewnoff.inapp.1month"

  val inapp = InApp(inappId, s"Title $inappId",
                    s"Author $inappId", true, 5)
  val user = User(UUID.randomUUID())

  before {
    collection.drop()
    userCollection.drop()
    inappCollection.drop()

    insertInApp(inapp)
    insertUser(user)
  }

  "An Scheduler Actor" when {

    "Subscribe" should {

      "accept new subscription to be scheduled" in {
        scheduler ! Subscribe(user._id, inappId,
           List[SubscriptionPeriod](Trial, Paid, Paid, Lapse(2.0), Paid))
        expectMsg(Subscribed)
      }

      "accept to be already subscribed to old subscription" in {
        insertSubscription(Subscription(user._id.toString, inappId,
                                                    Nil, DateTime.now))
        scheduler ! Subscribe(user._id, inappId, Nil)
        // expectMsg(AlreadySubscribed) TODO
        expectMsg(Subscribed)
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
    collection.insert(grater[Subscription].asDBObject(sub))
  }

  def getSubscription(uid: UUID, pid: String): Subscription = {
    val dbObject = collection.findOne(MongoDBObject("pid" -> pid, "uid" -> uid))
    grater[Subscription].asObject(dbObject.get)
  }
}
