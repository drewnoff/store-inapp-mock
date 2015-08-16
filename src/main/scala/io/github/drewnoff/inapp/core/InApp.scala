package io.github.drewnoff.inapp.core

case class InApp(
  product_id: String,
  title: String,
  author: String,
  autorenewable: Boolean,
  period: Int
)
