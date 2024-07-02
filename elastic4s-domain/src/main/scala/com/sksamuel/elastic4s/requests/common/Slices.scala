package com.sksamuel.elastic4s.requests.common

sealed trait Slices

case object AutoSlices extends Slices
case class NumericSlices(slices: Int) extends Slices
