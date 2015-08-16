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

import io.github.drewnoff.inapp.core.InApp
import io.github.drewnoff.inapp.core.{CoreActors, Core}
import io.github.drewnoff.inapp.core.InAppRegistryActor.{
  Register, Registered, NotRegistered, ListAll, RegistryList, Delete, Deleted
}

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import com.novus.salat.global._



class InAppRegistryServiceSpec extends WordSpec with HttpService with ScalatestRouteTest with CoreActors with Core with InAppRegistryRoute with BeforeAndAfter {


  def actorRefFactory = system
  implicit def executionContext = global

  val client = MongoClient("localhost", 27017)
  val db = client("inapps")
  val collection = db("inapps")

  before {
    collection.drop()
  }

  val uri = "/api/v1/inapps"
  val inappId = "io.github.drewnoff.inapp.1month"

  "InAppRegistry API" when {
    "using routing infrastructure" should {
      "support the most simple and direct route" in {
        Get(uri) ~> complete(HttpResponse()) ~> check {
          assert(response === HttpResponse())
        }
      }
    }

    s"GET $uri" should {
      "return OK" in {
        Get(uri) ~> route ~> check {
          assert(response.status === OK)
        }
      }

      "return all inapp products" in {
        val expected = insertInApps(3).map { inapp =>
          InApp(inapp.product_id, inapp.title, inapp.author, true, 5)
        }
        Get(uri) ~> route ~> check {
          assert(response.entity !== None)
          val inapps = responseAs[RegistryList]
          assert(inapps.inapps.size === expected.size)
          assert(inapps === RegistryList(expected))
        }
      }
    }

    s"GET $uri/_id_$inappId" should {
      val expected = InApp(inappId, "Boost", "Supercell", true, 5)

      "return OK" in {
        insertInApp(expected)
        Get(s"$uri/_id/$inappId") ~> route ~> check {
          assert(response.status === OK)
        }
      }

      "return subscription" in {
        insertInApp(expected)
        Get(s"$uri/_id/$inappId") ~> route ~> check {
          assert(response.entity !== None)
          val inapp = responseAs[InApp]
          assert(inapp === expected)
        }
      }
    }

    s"POST $uri" should {

      val expected = InApp(inappId, "Boost", "Supercell", true, 5)

      "return OK" in {
        Post(uri, expected) ~> route ~> check {
          assert(response.status === OK)
        }
      }

      "return InApp" in {
        Post(uri, expected) ~> route ~> check {
          assert(response.entity !== None)
          val sub = responseAs[Registered]
          assert(sub === Registered(expected))
        }
      }

      "insert inapp to the DB" in {
        Post(uri, expected) ~> route ~> check {
          assert(response.status === OK)
          val sub = getInApp(inappId)
          assert(sub === expected)
        }
      }

      "update inapp when it exists in the DB" in {
        collection.insert(grater[InApp].asDBObject(expected))
        Post(uri, expected) ~> route ~> check {
          assert(response.status === OK)
          val inapp = getInApp(inappId)
          assert(inapp === expected)
        }
      }
    }

    s"DELETE $uri" should {

      val expected = InApp(inappId, "Boost", "Supercell", true, 5)

      "return OK" in {
        insertInApp(expected)
        Delete(s"$uri/_id/$inappId") ~> route ~> check {
          assert(response.status === OK)
        }
      }

      "return inapp" in {
        insertInApp(expected)
        Delete(s"$uri/_id/$inappId") ~> route ~> check {
          assert(response.entity !== None)
          val inapp = responseAs[Deleted]
          assert(inapp === Deleted(expected))
        }
      }

      "remove inapp from the DB" in {
        insertInApp(expected)
        Delete(s"$uri/_id/$inappId") ~> route ~> check {
          assert(response.status === OK)
          assert(getInApps().size === 0)
        }
      }
    }
  }

  def insertInApp(inapp: InApp) {
    collection.insert(grater[InApp].asDBObject(inapp))
  }

  def insertInApps(quantity: Int): List[InApp] = {
    val inapps = List.tabulate(quantity)(id =>
      InApp("io.github.drewnoff.inapp." + id, s"Title $id", s"Author $id", true, 5))
    for (inapp <- inapps) {
      collection.insert(grater[InApp].asDBObject(inapp))
    }
    inapps
  }

  def getInApps(): List[InApp] = {
    collection.find().toList.map(grater[InApp].asObject(_))
  }

  def getInApp(id: String): InApp = {
    val dbObject = collection.findOne(MongoDBObject("product_id" -> id))
    grater[InApp].asObject(dbObject.get)
  }
}
