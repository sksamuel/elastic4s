package com.sksamuel.elastic4s.requests.task

import com.fasterxml.jackson.annotation.JsonProperty
import com.sksamuel.elastic4s.FailedShard

case class TaskError(`type`: String,
                     reason: String,
                     phase: String,
                     grouped: Boolean,
                     @JsonProperty("caused_by") causedBy: Option[Cause],
                     @JsonProperty("failed_shards") failedShards: Seq[FailedShard] = Seq()
                    )
