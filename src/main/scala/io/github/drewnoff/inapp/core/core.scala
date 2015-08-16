package io.github.drewnoff.inapp.core

import akka.actor.{Props, ActorRefFactory, ActorSystem}
import io.github.drewnoff.inapp.api.{Api, RoutedHttpService} 
import akka.io.IO
import spray.can.Http
import io.github.drewnoff.inapp.web.StaticResources


/**
  *  Core is type containing the ``system: ActorSystem`` member. This enables us to use it in our
  *  apps as well as in our tests.
  */
trait Core {

  protected implicit def system: ActorSystem

}

/**
  *  This trait implements ``Core`` by starting the required ``ActorSystem`` and registering the
  *  termination handler to stop the system when the JVM exits.
  */
trait BootedCore extends Core with Api with StaticResources {
  def system: ActorSystem = ActorSystem("store-inapp-subscriptions")
  def actorRefFactory: ActorRefFactory = system

  val rootService = system.actorOf(Props(new RoutedHttpService(routes ~ staticResources )))

    IO(Http)(system) ! Http.Bind(rootService, "0.0.0.0", port = 8080)

  /**
    *  Ensure that the constructed ActorSystem is shut down when the JVM shuts down
    */
  sys.addShutdownHook(system.shutdown())
}

/**
  *  This trait contains the actors that make up our application; it can be mixed in with
  *  ``BootedCore`` for running code or ``TestKit`` for unit and integration tests.
  */
trait CoreActors {
  this: Core =>

  val registration = system.actorOf(Props[RegistrationActor])
  val inappregistry = system.actorOf(Props[InAppRegistryActor])
  val scheduler = system.actorOf(Props[SchedulerActor])
  val accountant = system.actorOf(Props[AccountantActor])
}
