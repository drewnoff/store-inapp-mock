package io.github.drewnoff.inapp.core

import com.github.nscala_time.time.Imports._


case class InAppReceipt(
  quantity: Int = 1,
  product_id: String,
  transaction_id: String,
  original_transaction_id: String,
  purchase_date: DateTime,
  original_purchase_date: DateTime,
  expires_date: DateTime,
  uid: String,
  series: String = ""
)

case class JsReceipt(
  quantity: Int = 1,
  product_id: String,
  transaction_id: String,
  original_transaction_id: String,
  uid: String,
  series: String = "",

  expires_date: Long,
  expires_date_formatted: DateTime,
  expires_date_formatted_pst: DateTime,

  original_purchase_date: DateTime,
  original_purchase_date_ms: Long,
  original_purchase_date_pst: DateTime,

  purchase_date: DateTime,
  purchase_date_ms: Long,
  purchase_date_pst: DateTime

  // app_item_id ???
  // bid ???
  // bvrs ???
  // item_id ???
  // unique_identifier
  // version_external_identifier,
  // web_order_line_item_id
)

object Receipt {

  def toInAppReceipt(j: JsReceipt): InAppReceipt =
    InAppReceipt(
      quantity = j.quantity,
      product_id = j.product_id,
      transaction_id = j.transaction_id,
      original_transaction_id = j.original_transaction_id,
      purchase_date = j.purchase_date,
      original_purchase_date = j.original_purchase_date,
      expires_date = j.expires_date_formatted, // expires_date_formatted_pst ?
      uid = j.uid,
      series = j.series
    )

  def fromInAppReceipt(r: InAppReceipt): JsReceipt =
    JsReceipt(
      quantity = r.quantity,
      product_id = r.product_id,
      transaction_id = r.transaction_id,
      original_transaction_id = r.original_transaction_id,
      purchase_date = r.purchase_date,
      purchase_date_pst = r.purchase_date,
      purchase_date_ms = r.purchase_date.getMillis,
      original_purchase_date = r.original_purchase_date,
      original_purchase_date_pst = r.original_purchase_date,
      original_purchase_date_ms = r.original_purchase_date.getMillis,
      expires_date_formatted = r.expires_date,
      expires_date_formatted_pst = r.expires_date,
      expires_date = r.expires_date.getMillis,
      uid = r.uid,
      series = r.series
    )
}
