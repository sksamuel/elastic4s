package com.sksamuel.elastic4s.handlers.searches.queries.nested

import com.sksamuel.elastic4s.BodyBuilder
import com.sksamuel.elastic4s.handlers.common.FetchSourceContextBuilderFn
import com.sksamuel.elastic4s.handlers.searches
import com.sksamuel.elastic4s.handlers.searches.queries.sort.SortBuilderFn
import com.sksamuel.elastic4s.json.{JsonValue, XContentFactory}
import com.sksamuel.elastic4s.requests.searches.queries.InnerHit

object InnerHitQueryBodyBuilder extends BodyBuilder[InnerHit] {

  override def toJson(d: InnerHit): JsonValue = {
    val builder = XContentFactory.jsonBuilder()
    if (d.name.trim.nonEmpty)
      builder.field("name", d.name)
    d.from.foreach(builder.field("from", _))
    d.explain.foreach(builder.field("explain", _))

    // source filtering
    d.fetchSource.foreach(FetchSourceContextBuilderFn(builder, _))

    d.trackScores.foreach(builder.field("track_scores", _))
    d.version.foreach(builder.field("version", _))
    d.size.foreach(builder.field("size", _))
    if (d.docValueFields.nonEmpty)
      builder.array("docvalue_fields", d.docValueFields.toArray)
    if (d.sorts.nonEmpty) {
      builder.startArray("sort")
      d.sorts.foreach { sort =>
        builder.rawValue(SortBuilderFn(sort))
      }
      builder.endArray()
    }
    if (d.storedFieldNames.nonEmpty)
      builder.array("stored_fields", d.storedFieldNames.toArray)

    if (d.fields.nonEmpty)
      builder.array("fields", d.fields.toArray)

    d.highlight.foreach { highlight =>
      builder.rawField("highlight", searches.HighlightBuilderFn(highlight))
    }
    builder.endObject()
    builder.value
  }
}
