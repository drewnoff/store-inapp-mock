package io.github.drewnoff.inapp.core

import akka.actor.Actor


object RegistrationActor {

  /**
    *  Registers the specified ``user``
    *  @param user the user to be registered
    */
  case class Register(user: User)
  case object Registered
  case object NotRegistered
}

/**
  *  Registers the users. Replies with
  */
class RegistrationActor extends Actor with MongoStorage{
  import RegistrationActor._

  def getCollection = "users"

  def receive: Receive = {
    case Register(user)     => {
      update(user)(List(("_id", user._id)))
      sender ! Right(Registered)}
  }

}
