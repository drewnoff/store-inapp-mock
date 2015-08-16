package io.github.drewnoff.inapp.core

import com.github.nscala_time.time.Imports._

import com.novus.salat.annotations._

@Salat
abstract class SubscriptionPeriod

case object Paid extends SubscriptionPeriod;

case object Trial extends SubscriptionPeriod;

// case object OptIn extends SubscriptionPeriod;

case class Lapse(idle: Double) extends SubscriptionPeriod;

case class Subscription(uid: String,
                        pid: String,
                        plan: List[SubscriptionPeriod],
                        startDateTime: DateTime)
