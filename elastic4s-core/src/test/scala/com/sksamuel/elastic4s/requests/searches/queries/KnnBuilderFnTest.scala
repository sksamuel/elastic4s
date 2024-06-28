package com.sksamuel.elastic4s.requests.searches.queries

import com.sksamuel.elastic4s.handlers.searches.knn.KnnBuilderFn
import com.sksamuel.elastic4s.requests.searches.HighlightField
import com.sksamuel.elastic4s.requests.searches.knn.Knn
import com.sksamuel.elastic4s.requests.searches.sort.FieldSort
import com.sksamuel.elastic4s.requests.searches.term.TermQuery
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class KnnBuilderFnTest extends AnyFunSuite with Matchers {

  test("Basic Knn request generates proper query.") {
    val request = Knn("image-vector", 50, Seq(54,10,-2))
    KnnBuilderFn(request).string shouldBe
      """{"field":"image-vector","query_vector":[54.0,10.0,-2.0],"k":1,"num_candidates":50,"boost":1.0}"""
  }
  test("Knn with all fields generates proper query.") {
    val request = Knn("image-vector", 50, Seq(54,10,-2)) k 5 filter TermQuery("file-type", "png") similarity 10 boost .4
    KnnBuilderFn(request).string shouldBe
    """{"field":"image-vector","query_vector":[54.0,10.0,-2.0],"k":5,"num_candidates":50,"similarity":10.0,"filter":{"term":{"file-type":{"value":"png"}}},"boost":0.4}"""
  }
  test("Knn with inner_hits generates proper query.") {
    val innerHit = InnerHit("inners")
      .from(2)
      .explain(false)
      .trackScores(true)
      .version(true)
      .size(2)
      .docValueFields(List("df1", "df2"))
      .sortBy(FieldSort("sortField"))
      .storedFieldNames(List("field1", "field2"))
      .highlighting(HighlightField("hlField"))
      .fields(List("f1", "f2"))

    val request = Knn("image-vector", 50, Seq(54,10,-2), inner = Some(innerHit)) k 5 filter TermQuery("file-type", "png") similarity 10 boost .4
    KnnBuilderFn(request).string shouldBe
    """{"field":"image-vector","query_vector":[54.0,10.0,-2.0],"k":5,"num_candidates":50,"similarity":10.0,"filter":{"term":{"file-type":{"value":"png"}}},"boost":0.4,"inner_hits":{"name":"inners","from":2,"explain":false,"track_scores":true,"version":true,"size":2,"docvalue_fields":["df1","df2"],"sort":[{"sortField":{"order":"asc"}}],"stored_fields":["field1","field2"],"fields":["f1","f2"],"highlight":{"fields":{"hlField":{}}}}}"""
  }
}
