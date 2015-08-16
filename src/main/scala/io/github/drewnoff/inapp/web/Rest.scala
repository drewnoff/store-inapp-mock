package io.github.drewnoff.inapp.web

import io.github.drewnoff.inapp.api.Api
import io.github.drewnoff.inapp.core.{Core, BootedCore, CoreActors}
import akka.actor.ActorSystem
import akka.io.IO
import spray.can.Http


object Rest extends App with BootedCore with Core with CoreActors with Api with StaticResources
