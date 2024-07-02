package com.sksamuel.elastic4s.handlers.update

import com.sksamuel.elastic4s.ext.OptionImplicits.RichOption
import com.sksamuel.elastic4s.handlers.ElasticErrorParser
import com.sksamuel.elastic4s.handlers.common.FetchSourceContextQueryParameterFn
import com.sksamuel.elastic4s.handlers.script.ScriptBuilderFn
import com.sksamuel.elastic4s.handlers.searches.queries
import com.sksamuel.elastic4s.json.{XContentBuilder, XContentFactory}
import com.sksamuel.elastic4s.requests.common.{AutoSlices, NumericSlices, RefreshPolicyHttpValue}
import com.sksamuel.elastic4s.requests.task.GetTask
import com.sksamuel.elastic4s.requests.update.{BaseUpdateByQueryRequest, UpdateByQueryAsyncRequest, UpdateByQueryAsyncResponse, UpdateByQueryRequest, UpdateByQueryResponse, UpdateByQueryTask, UpdateRequest, UpdateResponse}
import com.sksamuel.elastic4s.{BulkIndexByScrollFailure, ElasticError, ElasticRequest, ElasticUrlEncoder, Handler, HttpEntity, HttpResponse, ResponseHandler}

object UpdateByQueryBodyFn {
  def apply(request: BaseUpdateByQueryRequest): XContentBuilder = {
    val builder = XContentFactory.jsonBuilder()
    builder.rawField("query", queries.QueryBuilderFn(request.query))
    request.script.map(ScriptBuilderFn.apply).foreach(builder.rawField("script", _))

    request.slice.foreach { slice =>
      builder.startObject("slice")
      builder.field("id", slice.id)
      builder.field("max", slice.max)
      builder.endObject()
    }

    builder.endObject()
  }
}

object UpdateHandlers extends UpdateHandlers

trait UpdateHandlers {

  implicit object UpdateHandler extends Handler[UpdateRequest, UpdateResponse] {

    override def responseHandler: ResponseHandler[UpdateResponse] = new ResponseHandler[UpdateResponse] {
      override def handle(response: HttpResponse): Either[ElasticError, UpdateResponse] = response.statusCode match {
        case 200 | 201 =>
          val json = response.entity.getOrError("Update responses must include a body")
          Right(ResponseHandler.fromEntity[UpdateResponse](json))
        case _ => Left(ElasticErrorParser.parse(response))
      }
    }

    override def build(request: UpdateRequest): ElasticRequest = {

      val endpoint =
        s"/${ElasticUrlEncoder.encodeUrlFragment(request.index.index)}/_update/${ElasticUrlEncoder.encodeUrlFragment(request.id)}"

      val params = scala.collection.mutable.Map.empty[String, Any]
      request.fetchSource.foreach { context =>
        FetchSourceContextQueryParameterFn(context).foreach { case (key, value) => params.put(key, value) }
      }
      request.retryOnConflict.foreach(params.put("retry_on_conflict", _))
      request.parent.foreach(params.put("parent", _))
      request.routing.foreach(params.put("routing", _))
      request.refresh.map(RefreshPolicyHttpValue.apply).foreach(params.put("refresh", _))
      request.version.map(_.toString).foreach(params.put("version", _))
      request.ifPrimaryTerm.map(_.toString).foreach(params.put("if_primary_term", _))
      request.ifSeqNo.map(_.toString).foreach(params.put("if_seq_no", _))
      request.versionType.foreach(params.put("version_type", _))
      request.waitForActiveShards.foreach(params.put("wait_for_active_shards", _))

      val body = UpdateBuilderFn(request)
      val entity = HttpEntity(body.string, "application/json")

      ElasticRequest("POST", endpoint, params.toMap, entity)
    }
  }

  abstract class UpdateByQueryHandler[Q <: BaseUpdateByQueryRequest, R: Manifest] extends Handler[Q, R] {

    override def responseHandler: ResponseHandler[R] = ResponseHandler.default[R]

    override def build(request: Q): ElasticRequest = {

      val endpoint = s"/${request.indexes.values.mkString(",")}/_update_by_query"

      val params = scala.collection.mutable.Map.empty[String, Any]
      if (request.proceedOnConflicts.getOrElse(false))
        params.put("conflicts", "proceed")
      request.refresh.map(RefreshPolicyHttpValue.apply).foreach(params.put("refresh", _))
      request.requestsPerSecond.foreach(params.put("requests_per_second", _))
      request.timeout.map(_.toMillis + "ms").foreach(params.put("timeout", _))
      request.scroll.foreach(params.put("scroll", _))
      request.scrollSize.foreach(params.put("scroll_size", _))
      request.waitForActiveShards.foreach(params.put("wait_for_active_shards", _))
      request.waitForCompletion.foreach(params.put("wait_for_completion", _))
      request.slices.foreach {
        case AutoSlices            => params.put("slices", "auto")
        case NumericSlices(slices) => params.put("slices", slices)
      }

      val body = UpdateByQueryBodyFn(request)
      logger.debug(s"Update by query ${body.string}")
      val entity = HttpEntity(body.string, "application/json")

      ElasticRequest("POST", endpoint, params.toMap, entity)
    }
  }

  implicit object SyncUpdateByQueryHandler extends UpdateByQueryHandler[UpdateByQueryRequest, UpdateByQueryResponse] {
    override def responseHandler: ResponseHandler[UpdateByQueryResponse] = ResponseHandler.default[UpdateByQuerySnakeCase].map { resp =>
      UpdateByQueryResponse(
        resp.took,
        resp.timed_out,
        resp.total,
        resp.updated,
        resp.deleted,
        resp.batches,
        resp.version_conflicts,
        resp.noops,
        resp.throttled_millis,
        resp.requests_per_second,
        resp.throttled_until_millis,
        resp.failures
      )
    }
  }

  implicit object AsyncUpdateByQueryHandler extends UpdateByQueryHandler[UpdateByQueryAsyncRequest, UpdateByQueryTask] {
    override def responseHandler: ResponseHandler[UpdateByQueryTask] =
      ResponseHandler.default[UpdateByQueryAsyncResponse]
        .map { case UpdateByQueryAsyncResponse(task) =>
          task.split(":") match {
            case Array(nodeId, taskId) => UpdateByQueryTask(GetTask(nodeId, taskId))

            // Not the cleaner way to handle error, but this would mean a bigger refactoring
            case _ => sys.error(s"Task id returned has an invalid format : $task")
          }
        }
  }
}


case class UpdateByQuerySnakeCase(
                                   took: Long,
                                   timed_out: Boolean,
                                   total: Long,
                                   updated: Long,
                                   deleted: Long,
                                   batches: Long,
                                   version_conflicts: Long,
                                   noops: Long,
                                   throttled_millis: Long,
                                   requests_per_second: Long,
                                   throttled_until_millis: Long,
                                   failures: Option[Seq[BulkIndexByScrollFailure]]
                                 )
