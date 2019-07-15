package com.sksamuel.elastic4s.requests.indexes

import com.sksamuel.elastic4s.requests.mappings.{GetMappingRequest, PutMappingRequest}
import com.sksamuel.elastic4s._

case class IndexMappings(index: String, mappings: Map[String, Any])

trait MappingHandlers {

  implicit object GetMappingHandler extends Handler[GetMappingRequest, Seq[IndexMappings]] {

    override def responseHandler: ResponseHandler[Seq[IndexMappings]] = new ResponseHandler[Seq[IndexMappings]] {

      override def handle(response: HttpResponse) : Either[ElasticError, Seq[IndexMappings]] = response.statusCode match {
        case 201 | 200       =>
          val raw = ResponseHandler.fromResponse[Map[String, Map[String, Map[String, Any]]]](response)
          val raw2 = raw.map {
            case (index, types) =>
              val mappings = getMappings(types("mappings"))
              IndexMappings(index, mappings.asInstanceOf[Map[String, Any]])
          }.toSeq
          Right(raw2)
        case _              =>
          try {
            Left(ElasticError.parse(response))
          }catch{
            case _ : Throwable => sys.error(s"""Failed to parse error response: \n${response.toString}""")
          }
      }
    }

    private def getMappings(mappings: Map[String, Any]) = {
      mappings
        .getOrElse("properties", getFieldMappings(mappings))
    }

    private def getFieldMappings(possibleFieldMapping: Map[String, Any]) = {
      possibleFieldMapping.size match {
        case 0 => Map.empty
        case _: Int => possibleFieldMapping.head._2 match {
          case map: Map[String, Any] =>
            map.getOrElse("mapping", Map.empty)
          case _ =>
            Map.empty
        }
      }
    }

    override def build(request: GetMappingRequest): ElasticRequest = {
      val baseEndpoint = request.indexes match {
        case Indexes(Nil)       => "/_mapping"
        case Indexes(indexes)   => s"/${indexes.mkString(",")}/_mapping"
      }

      val endpoint = request.fields match {
        case Nil => baseEndpoint
        case fields: Seq[String] => s"$baseEndpoint/field/${fields.mkString(",")}"
      }

      ElasticRequest("GET", endpoint)
    }
  }

  implicit object PutMappingHandler extends Handler[PutMappingRequest, PutMappingResponse] {

    override def build(request: PutMappingRequest): ElasticRequest = {

      val endpoint = s"/${request.indexesAndType.indexes.mkString(",")}/_mapping${request.indexesAndType.`type`.map("/" + _).getOrElse("")}"

      val params = scala.collection.mutable.Map.empty[String, Any]
      request.updateAllTypes.foreach(params.put("update_all_types", _))
      request.ignoreUnavailable.foreach(params.put("ignore_unavailable", _))
      request.allowNoIndices.foreach(params.put("allow_no_indices", _))
      request.expandWildcards.foreach(params.put("expand_wildcards", _))
      request.includeTypeName.foreach(params.put("include_type_name", _))

      val body   = PutMappingBuilderFn(request).string()
      val entity = HttpEntity(body, "application/json")

      ElasticRequest("PUT", endpoint, params.toMap, entity)
    }
  }
}

case class PutMappingResponse(acknowledged: Boolean) {
  def success: Boolean = acknowledged
}
