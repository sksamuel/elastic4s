package com.sksamuel.elastic4s.handlers.fields

import com.sksamuel.elastic4s.fields.{DenseVectorField, DenseVectorIndexOptions, FlatIndexOptions, HnswIndexOptions, Int8FlatIndexOptions, Int8HnswIndexOptions}
import com.sksamuel.elastic4s.json.{XContentBuilder, XContentFactory}

object DenseVectorFieldBuilderFn {

  private def getIndexOptions(values: Map[String, Any]): DenseVectorIndexOptions=
    values("type").asInstanceOf[String] match {
      case "hnsw" => HnswIndexOptions(
        values.get("m").map(_.asInstanceOf[Int]),
        values.get("ef_construction").map(_.asInstanceOf[Int])
      )
      case "int8_hnsw" => Int8HnswIndexOptions(
        values.get("m").map(_.asInstanceOf[Int]),
        values.get("ef_construction").map(_.asInstanceOf[Int]),
        values.get("confidence_interval").map(_.asInstanceOf[Double])
      )
      case "flat" => FlatIndexOptions()
      case "int8_flat" => Int8FlatIndexOptions(values.get("confidence_interval").map(_.asInstanceOf[Double]))
    }

  def toField(name: String, values: Map[String, Any]): DenseVectorField = DenseVectorField(
    name,
    values("dims").asInstanceOf[Int],
    values("index").asInstanceOf[Boolean],
    indexOptions = values.get("index_options").map(_.asInstanceOf[Map[String, Any]]).map(getIndexOptions)
  )

  def build(field: DenseVectorField): XContentBuilder = {

    val builder = XContentFactory.jsonBuilder()
    builder.field("type", field.`type`)
    builder.field("dims", field.dims)
    builder.field("index", field.index)
    builder.field("similarity", field.similarity.name)
    field.indexOptions.filter(_ => field.index).foreach { options =>
      builder.startObject("index_options")
      builder.field("type", options.`type`)
      options match {
        case HnswIndexOptions(m, efConstruction) =>
          m.foreach(builder.field("m", _))
          efConstruction.foreach(builder.field("ef_construction", _))
        case Int8HnswIndexOptions(m, efConstruction, confidenceInterval) =>
          m.foreach(builder.field("m", _))
          efConstruction.foreach(builder.field("ef_construction", _))
          confidenceInterval.foreach(builder.field("confidence_interval", _))
        case FlatIndexOptions() => ()
        case Int8FlatIndexOptions(confidenceInterval) =>
          confidenceInterval.foreach(builder.field("confidence_interval", _))
      }
      builder.endObject()
    }
    builder.endObject()
  }
}
