package com.sksamuel.elastic4s.handlers.searches.knn

import com.sksamuel.elastic4s.handlers.searches.queries.QueryBuilderFn
import com.sksamuel.elastic4s.handlers.searches.queries.nested.InnerHitQueryBodyBuilder
import com.sksamuel.elastic4s.json.{XContentBuilder, XContentFactory}
import com.sksamuel.elastic4s.requests.searches.knn.Knn

object KnnBuilderFn {
  def apply(knn: Knn): XContentBuilder = {
    val builder: XContentBuilder = XContentFactory.jsonBuilder()
    builder.field("field", knn.field)
    builder.array("query_vector", knn.queryVector.toArray)
    builder.field("k", knn.k)
    builder.field("num_candidates", knn.numCandidates)
    knn.similarity match {
      case Some(value) => builder.field("similarity", value)
      case _ =>
    }
    knn.filter.foreach(filter => builder.rawField("filter", QueryBuilderFn(filter)))
    builder.field("boost", knn.boost)
    knn.inner.foreach(inner => builder.field("inner_hits", InnerHitQueryBodyBuilder.toJson(inner)))
    builder.endObject()
    builder
  }
}
