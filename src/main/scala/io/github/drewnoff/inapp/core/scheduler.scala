package io.github.drewnoff.inapp.core

import akka.actor.{Props, Actor}
import scala.util.{Try, Success, Failure}
import java.util.UUID

import com.github.nscala_time.time.Imports._


object SchedulerActor {

  /**
    *  Creates a schedule for specified inapp subscription product and user
    *  @param inapp the inapp to be registered
    */
  case class Subscribe(userId: UUID, productId: String, subscriptionPlan: List[SubscriptionPeriod])

  abstract class SubscribeResult
  case object Subscribed extends SubscribeResult
  case object AlreadySubscribed extends SubscribeResult
  case object NotSubscribed extends SubscribeResult

  case class Unsubscribe(userId: UUID, productId: String)
  class UnsubscribeResult
  case class Unsubscribed() extends UnsubscribeResult
}

/**
  * Registers subscription.
  */
class SchedulerActor extends Actor with MongoStorage {
  import SchedulerActor._

  def getCollection = "subscriptions"

  def receive: Receive = {
    case Subscribe(userId, productId, subscriptionPlan) =>  {
      sender ! (subscribe(userId, productId, subscriptionPlan) match {
        case Success(result) => result
        case Failure(ex) => NotSubscribed
      })
    }
  }

  def subscribe(userId: UUID, productId: String,
         subscriptionPlan: List[SubscriptionPeriod]): Try[SubscribeResult] = Try {
    getOne[Subscription, String, String](List(("uid", userId.toString),
                                              ("pid", productId))) match {
      case Some(subs) => Subscribed// AlreadySubscribed TODO
      case None =>  {
        put(Subscription(userId.toString, productId, subscriptionPlan, DateTime.now))
        Subscribed
      }
    }
  }
}
