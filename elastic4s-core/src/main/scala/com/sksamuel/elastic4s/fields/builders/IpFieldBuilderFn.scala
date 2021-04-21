package com.sksamuel.elastic4s.fields.builders

import com.sksamuel.elastic4s.fields.IpField
import com.sksamuel.elastic4s.json.{XContentBuilder, XContentFactory}

object IpFieldBuilderFn {

  def build(field: IpField): XContentBuilder = {
    val builder = XContentFactory.jsonBuilder()

    builder.field("type", field.`type`)
    field.boost.foreach(builder.field("boost", _))
    field.docValues.foreach(builder.field("doc_values", _))
    field.ignoreMalformed.foreach(builder.field("ignore_malformed", _))
    field.index.foreach(builder.field("index", _))
    field.nullValue.foreach(builder.field("null_value", _))
    field.store.foreach(builder.field("store", _))
    builder.endObject()
  }
}

