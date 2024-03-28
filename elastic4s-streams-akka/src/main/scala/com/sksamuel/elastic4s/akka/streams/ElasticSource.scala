package com.sksamuel.elastic4s.akka.streams

import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.sksamuel.elastic4s.ElasticApi.clearScroll
import com.sksamuel.elastic4s.ElasticDsl.searchScroll
import com.sksamuel.elastic4s.requests.searches._
import com.sksamuel.elastic4s._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * An Akka [[akka.stream.scaladsl.Source]], that publishes documents using an elasticsearch
  * scroll cursor. The initial query must be provided to the source, and there are helpers to create
  * a query for all documents in an index.
  *
  * @param client   a client for the cluster
  * @param settings settings for how documents are queried
  */
class ElasticSource(client: ElasticClient, settings: SourceSettings)
                   (implicit ec: ExecutionContext) extends GraphStage[SourceShape[SearchHit]] {
  require(settings.search.keepAlive.isDefined, "The SearchRequest must have a scroll defined (a keep alive time)")

  private val out: Outlet[SearchHit] = Outlet.create("ElasticSource.out")
  override val shape: SourceShape[SearchHit] = SourceShape.of(out)

  private implicit val searchHandler: Handler[SearchRequest, SearchResponse] = SearchHandlers.SearchHandler
  private implicit val scrollHandler: Handler[SearchScrollRequest, SearchResponse] = SearchScrollHandlers.SearchScrollHandler
  private implicit val clearScrollHandler: Handler[ClearScrollRequest, ClearScrollResponse] = SearchScrollHandlers.ClearScrollHandler
  private implicit val executor: Executor[Future] = Executor.FutureExecutor
  private implicit val functor: Functor[Future] = Functor.FutureFunctor

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with OutHandler {

    private val buffer = scala.collection.mutable.Queue.empty[SearchHit]
    private var scrollId: String = _
    private var fetching = false

    // Parse the keep alive setting out of the original query.
    private val keepAlive = settings.search.keepAlive.map(_.toString).getOrElse("1m")

    if (settings.warm)
      fetch()

    private val populateHandler = getAsyncCallback[Try[Response[SearchResponse]]] {
      case Failure(e) => fail(out, e)
      case Success(response) => response match {
        case RequestFailure(_, _, _, error) => fail(out, error.asException)
        case RequestSuccess(_, _, _, searchr) =>
          searchr.scrollId match {
            case None => fail(out, new RuntimeException("Search response did not include a scroll id"))
            case Some(id) =>
              scrollId = id
              fetching = false
              buffer ++= searchr.hits.hits
              if (buffer.nonEmpty && isAvailable(out)) {
                push(out, buffer.dequeue)
                maybeFetch()
              }
              // complete when no more elements to emit
              if (searchr.hits.hits.length == 0) {
                complete(out)
              }
          }
      }
    }

    // check if the buffer has dropped below threshold (or is empty) and if so, trigger a fetch
    private def maybeFetch(): Unit = {
      if (buffer.isEmpty || buffer.size <= settings.fetchThreshold)
        fetch()
    }

    // if no fetch is in progress then fire one
    private def fetch(): Unit = {
      if (!fetching) {
        Option(scrollId) match {
          case None => client.execute(settings.search).onComplete(populateHandler.invoke)
          case Some(id) => client.execute(searchScroll(id).keepAlive(keepAlive)).onComplete(populateHandler.invoke)
        }
        fetching = true
      }
    }

    override def onPull(): Unit = {
      if (buffer.nonEmpty)
        push(out, buffer.dequeue)
      maybeFetch()
    }

    override def postStop(): Unit = {
      Option(scrollId) match {
        case Some(id) => client.execute(clearScroll(id))
        case _ => ()
      }
    }

    setHandler(out, this)
  }
}

case class SourceSettings(search: SearchRequest, maxItems: Long, fetchThreshold: Int = 0, warm: Boolean)
