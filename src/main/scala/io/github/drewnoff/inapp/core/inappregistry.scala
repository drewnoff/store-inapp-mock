package io.github.drewnoff.inapp.core

import akka.actor.Actor


object InAppRegistryActor {

  /**
    *  Registers the specified ``inapp product``
    *  @param inapp the inapp to be registered
    */
  case class Register(inapp: InApp)
  case class Registered(inapp: InApp)
  case object NotRegistered
  case object ListAll
  case class RegistryList(inapps: List[InApp])
  case class GetInApp(product_id: String)
  case class Delete(product_id: String)
  case class Deleted(inapp: InApp)
}

/**
  *  Registers the inapp. Replies with
  */
class InAppRegistryActor extends Actor with MongoStorage{
  import InAppRegistryActor._

  def getCollection = "inapps"

  def receive: Receive = {
    case Register(inapp)     => {
      // update(inapp)(List(("_id", inapp._id)))
      update(inapp)(List(("product_id", inapp.product_id)))
      sender ! Registered(inapp)
    }
    case Delete(productId)         => {
      val inapp = delete[InApp, String, String](List(("product_id", productId)))
      sender ! Deleted(inapp)
    }
    case ListAll            => {
      sender ! RegistryList(find[InApp, String, Any](List()))
    }
    case GetInApp(productId)            => {
      val inapp = getOne[InApp, String, String](List(("product_id", productId)))
      sender ! inapp.get // TODO proper Option[T] handling
    }
  }

}
