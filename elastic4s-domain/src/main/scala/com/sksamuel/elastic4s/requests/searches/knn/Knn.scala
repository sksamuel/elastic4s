package com.sksamuel.elastic4s.requests.searches.knn

import com.sksamuel.elastic4s.ext.OptionImplicits.RichOptionImplicits
import com.sksamuel.elastic4s.requests.searches.queries.{InnerHit, Query}

case class Knn(
     field: String,
     numCandidates: Int,
     queryVector: Seq[Double],
     k: Int = 1,
     similarity: Option[Float] = None,
     filter: Option[Query] = None,
     boost: Double = 1.0,
     inner: Option[InnerHit] = None) {

  def k(k: Int): Knn = copy(k = k)

  def similarity(similarity: Float): Knn = copy(similarity = similarity.some)

  def filter(filter: Query): Knn = copy(filter = filter.some)

  def boost(boost: Double): Knn = copy(boost = boost)

  def inner(inner: InnerHit): Knn = copy(inner = Option(inner))
}
