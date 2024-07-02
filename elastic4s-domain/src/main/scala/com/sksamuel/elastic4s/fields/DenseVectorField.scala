package com.sksamuel.elastic4s.fields

object DenseVectorField {
  val `type`: String = "dense_vector"
}
sealed trait Similarity {
  def name: String
}
case object L2Norm extends Similarity { val name = "l2_norm" }
case object DotProduct extends Similarity { val name = "dot_product" }
case object Cosine extends Similarity { val name = "cosine" }

case class DenseVectorField(name: String,
                            dims: Int,
                            index: Boolean = false,
                            similarity: Similarity = L2Norm,
                            indexOptions: Option[DenseVectorIndexOptions] = None) extends ElasticField {
  override def `type`: String  = DenseVectorField.`type`
}

sealed trait DenseVectorIndexOptions {
  def `type`: String
}
case class HnswIndexOptions(m: Option[Int] = None, efConstruction: Option[Int] = None) extends DenseVectorIndexOptions {
  val `type`: String = "hnsw"
}
case class Int8HnswIndexOptions(m: Option[Int] = None,
                                efConstruction: Option[Int] = None,
                                confidenceInterval: Option[Double] = None) extends DenseVectorIndexOptions {
  val `type`: String = "int8_hnsw"
}
case class FlatIndexOptions() extends DenseVectorIndexOptions {
  val `type`: String = "flat"
}
case class Int8FlatIndexOptions(confidenceInterval: Option[Double] = None) extends DenseVectorIndexOptions {
  val `type`: String = "int8_flat"
}
