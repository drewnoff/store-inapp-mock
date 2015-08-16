package io.github.drewnoff.inapp.core

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.ActorSystem
import org.scalatest.WordSpecLike
import org.scalatest.BeforeAndAfter

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import com.novus.salat.global._


class InAppRegistryActorSpec extends TestKit(ActorSystem()) with WordSpecLike with CoreActors with Core with ImplicitSender with BeforeAndAfter {
  import InAppRegistryActor._

  val client = MongoClient("localhost", 27017)
  val db = client("inapps")
  val collection = db("inapps")

  before {
    collection.drop()
  }

  "An InAppRegistry Actor" when {

    "Register" should {
      val inappId = "io.github.drewnoff.inapp.1month"

      val inapp = InApp(inappId, s"Title $inappId",
      	  	  		 s"Author $inappId", true, 5)

      "accept new inapp to be registered" in {
        inappregistry ! Register(inapp)
        expectMsg(Registered(inapp))
      }

      "accept old inapp to be registered" in {
        insertInApp(inapp)
        inappregistry ! Register(inapp)
        expectMsg(Registered(inapp))
      }

      "insert new inapp to the DB while registering" in {
        inappregistry ! Register(inapp)
        expectMsg(Registered(inapp))
        val newInApp = getInApp(inapp.product_id)
        assert(newInApp === inapp)
      }

      "update inapp when it exists in the DB" in {
        insertInApp(inapp)
        inappregistry ! Register(inapp)
        expectMsg(Registered(inapp))
        val newInApp = getInApp(inapp.product_id)
        assert(newInApp === inapp)
      }
    }

    "RegistryList" should {

      "return all inapp products" in {
        val expected = insertInApps(3).map { inapp =>
          InApp(inapp.product_id, inapp.title, inapp.author, true, 5)
        }
        inappregistry ! ListAll
        expectMsg(RegistryList(expected))
      }
    }

    "Delete" should {
      val inappId = "io.drewnoff.inapp.1hour"
      val inapp = InApp(inappId, s"Title $inappId",
      	  	  		 s"Author $inappId", true, 5)

      "return deleted inapp" in {
        insertInApp(inapp)
        inappregistry ! Delete(inapp.product_id)
        expectMsg(Deleted(inapp))
      }

      "remove deleted inapp from the DB" in {
        insertInApp(inapp)
        inappregistry ! Delete(inapp.product_id)
        expectMsg(Deleted(inapp))
        assert(getInApps().size === 0)
      }
    }
  }

  def insertInApp(inapp: InApp) {
    collection.insert(grater[InApp].asDBObject(inapp))
  }

  def insertInApps(quantity: Int): List[InApp] = {
    val inapps = List.tabulate(quantity)(id =>
      InApp("io.drewnoff.inapp." + id, s"Title $id", s"Author $id", true, 5))
    for (inapp <- inapps) {
      collection.insert(grater[InApp].asDBObject(inapp))
    }
    inapps
  }

  def getInApp(id: String): InApp = {
    val dbObject = collection.findOne(MongoDBObject("product_id" -> id))
    grater[InApp].asObject(dbObject.get)
  }

  def getInApps(): List[InApp] = {
    collection.find().toList.map(grater[InApp].asObject(_))
  }
}
