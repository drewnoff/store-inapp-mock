package io.github.drewnoff.inapp.core

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.{MongoClient, MongoCollection}
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import com.novus.salat.global._
import scala.util.Properties._


trait Storage {
  def put[T <: AnyRef](obj: T)(implicit m: Manifest[T]): Unit
  def update[T <: AnyRef, A <: String, B](obj: T)(elems: List[(A, B)])(implicit m: Manifest[T]): Unit
  def getOne[T <: AnyRef, A <: String, B](elems: List[(A, B)], colname: String)(implicit m: Manifest[T]): Option[T]

  def delete[T <: AnyRef, A <: String, B](elems: List[(A, B)])(implicit m: Manifest[T]): T
  def find[T <: AnyRef, A <: String, B](elems: List[(A, B)])(implicit m: Manifest[T]): List[T]
}

trait MongoStorage extends Storage {

  com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers()

  def getCollection: String

  val client = MongoClient(
    envOrElse("DB_PORT_27017_TCP_ADDR", "localhost"),
    envOrElse("DB_PORT_27017_TCP_PORT", "27017").toInt
  )
  val db = client(envOrElse("DB_DBNAME", "inapps"))
  def collection(colname: String) = db(colname)

  def put[T <: AnyRef](entity: T)(implicit m: Manifest[T]) =
    collection(getCollection).insert(grater[T].asDBObject(entity))

  def update[T <: AnyRef, A <: String, B](entity: T)(elems: List[(A, B)])(implicit m: Manifest[T]): Unit =
    collection(getCollection).update(
      MongoDBObject(elems),
      grater[T].asDBObject(entity),
      upsert = true
    )

  def getOne[T <: AnyRef, A <: String, B](elems: List[(A, B)], colname: String=getCollection)(implicit m: Manifest[T]): Option[T] =
    collection(colname).findOne(MongoDBObject(elems)) match {
      case Some(mobj) => Some(grater[T].asObject(mobj))
      case None => None
    }

  def delete[T <: AnyRef, A <: String, B](elems: List[(A, B)])(implicit m: Manifest[T]): T =
    grater[T].asObject(
            collection(getCollection).findAndRemove(MongoDBObject(elems)).get
    )

  def find[T <: AnyRef, A <: String, B](elems: List[(A, B)])(implicit m: Manifest[T]): List[T] =
    collection(getCollection).find(MongoDBObject(elems)).toList.map(grater[T].asObject(_))
}
