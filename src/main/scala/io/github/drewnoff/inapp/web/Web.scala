package io.github.drewnoff.inapp.web

import io.github.drewnoff.inapp.core.{CoreActors, Core}
import io.github.drewnoff.inapp.api.{RoutedHttpService, Api}
import akka.io.IO
import spray.can.Http
import akka.actor.{ActorSystem, Props}

/**
  *  Provides the web server (spray-can) for the REST api in ``Api``, using the actor system
  *  defined in ``Core``.
  * 
  *  You may sometimes wish to construct separate ``ActorSystem`` for the web server machinery.
  *  However, for this simple application, we shall use the same ``ActorSystem`` for the
  *  entire application.
  * 
  *  Benefits of separate ``ActorSystem`` include the ability to use completely different
  *  configuration, especially when it comes to the threading model.
  */
trait Web extends StaticResources with CoreActors with Core with Api
