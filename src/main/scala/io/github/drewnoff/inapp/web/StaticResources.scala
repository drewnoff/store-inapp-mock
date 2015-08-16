package io.github.drewnoff.inapp.web

import spray.routing.HttpService
import spray.http.StatusCodes

trait StaticResources extends HttpService {

  val staticResources = get {
    path("") {
      redirect("/index.html", StatusCodes.MovedPermanently)
    } ~
    path("favicon.ico") {
      complete(StatusCodes.NotFound)
    } ~
    path(Rest) {
      path => getFromResource("root/%s" format path)
    }

  }

}
