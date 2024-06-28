package com.sksamuel.elastic4s.requests.searches.queries

import com.sksamuel.elastic4s.ext.OptionImplicits._
import com.sksamuel.elastic4s.requests.common.FetchSourceContext
import com.sksamuel.elastic4s.requests.searches.sort.Sort
import com.sksamuel.elastic4s.requests.searches.{Highlight, HighlightField, HighlightOptions}

case class InnerHit(name: String,
                    size: Option[Int] = None,
                    fetchSource: Option[FetchSourceContext] = None,
                    version: Option[Boolean] = None,
                    trackScores: Option[Boolean] = None,
                    explain: Option[Boolean] = None,
                    storedFieldNames: Seq[String] = Nil,
                    docValueFields: Seq[String] = Nil,
                    sorts: Seq[Sort] = Nil,
                    from: Option[Int] = None,
                    highlight: Option[Highlight] = None,
                    fields: Seq[String] = Nil) {

  def sortBy(sorts: Sort*): InnerHit = sortBy(sorts)
  def sortBy(sorts: Iterable[Sort]): InnerHit = copy(sorts = sorts.toSeq)

  def highlighting(first: HighlightField, rest: HighlightField*): InnerHit =
    highlighting(HighlightOptions(), first +: rest)

  def highlighting(fields: Iterable[HighlightField]): InnerHit =
    highlighting(HighlightOptions(), fields)

  def highlighting(options: HighlightOptions, first: HighlightField, rest: HighlightField*): InnerHit =
    highlighting(options, first +: rest)

  def highlighting(options: HighlightOptions, fields: Iterable[HighlightField]): InnerHit =
    copy(highlight = Highlight(options, fields).some)

  def trackScores(trackScores: Boolean): InnerHit = copy(trackScores = trackScores.some)
  def version(version: Boolean): InnerHit = copy(version = version.some)
  def from(from: Int): InnerHit = copy(from = from.some)
  def fetchSource(fetchSource: FetchSourceContext): InnerHit = copy(fetchSource = fetchSource.some)
  def size(size: Int): InnerHit = copy(size = size.some)

  def docValueFields(docValueFields: Iterable[String]): InnerHit =
    copy(docValueFields = docValueFields.toSeq)

  def storedFieldNames(storedFieldNames: Iterable[String]): InnerHit =
    copy(storedFieldNames = storedFieldNames.toSeq)

  def explain(explain: Boolean): InnerHit = copy(explain = explain.some)

  def fields(fields: Iterable[String]): InnerHit =
    copy(fields = fields.toSeq)
}
