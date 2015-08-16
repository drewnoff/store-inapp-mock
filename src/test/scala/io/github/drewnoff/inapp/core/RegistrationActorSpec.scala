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

class RegistrationActorSpec extends TestKit(ActorSystem()) with WordSpecLike with CoreActors with Core with ImplicitSender with BeforeAndAfter {
  import RegistrationActor._

  val client = MongoClient("localhost", 27017)
  val db = client("inapps")
  val collection = db("users")

  before {
    collection.drop()
  }
  import scala.concurrent.duration._
  implicit val timeout = Timeout(4.seconds)

  private def mkUser(): User = User(UUID.randomUUID())

  "A Registration Service" when {

    "Registration" should {

      val user = mkUser()

      "accept new user to be registered" in {
        registration ! Register(user)
        expectMsg(Right(Registered))
      }

      "accept old user to be registered" in {
        insertUser(user)
        registration ! Register(user)
        expectMsg(Right(Registered))
      }

      "insert new user to the DB while registering" in {
        registration ! Register(user)
        expectMsg(Right(Registered))
        val newUser = getUser(user._id)
        assert(newUser === user)
      }
    }
  }

  def insertUser(user: User) {
    collection.insert(grater[User].asDBObject(user))
  }

  def getUser(id: UUID): User = {
    val dbObject = collection.findOne(MongoDBObject("_id" -> id))
    grater[User].asObject(dbObject.get)
  }
}
