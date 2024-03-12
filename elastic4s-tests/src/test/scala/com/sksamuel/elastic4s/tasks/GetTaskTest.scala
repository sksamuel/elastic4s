package com.sksamuel.elastic4s.tasks

import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.searches.queries.compound.BoolQuery
import com.sksamuel.elastic4s.requests.searches.term.TermQuery
import com.sksamuel.elastic4s.requests.task.{GetTask, GetTaskResponse, Retries}
import com.sksamuel.elastic4s.testkit.DockerTests
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration.DurationInt

class GetTaskTest extends AnyWordSpec with Matchers with DockerTests {

  cleanIndex("get_task_a")
  cleanIndex("get_task_b")

  client.execute {
    bulk(
      indexInto("get_task_a").fields(Map("foo" -> "far")),
      indexInto("get_task_a").fields(Map("moo" -> "mar")),
      indexInto("get_task_a").fields(Map("moo" -> "mar")),
      indexInto("get_task_a").fields(Map("goo" -> "gar"))
    ).refresh(RefreshPolicy.Immediate)
  }.await

  "get task" should {
    "return task information" in {

      val start = System.currentTimeMillis()

      // kick off a task
      val resp = client.execute {
        reindex("get_task_a", "get_task_b").waitForCompletion(false)
      }.await.result.right.get

      // use the task id from the above task
      val task = client.execute {
        getTask(resp.nodeId, resp.taskId)
      }.await.result.task

      task.node shouldBe resp.nodeId
      task.id shouldBe resp.taskId
      task.`type` shouldBe "transport"
      task.action shouldBe "indices:data/write/reindex"
      task.description shouldBe "reindex from [get_task_a] to [get_task_b]"
      task.startTimeInMillis >= start shouldBe true
      task.status.versionConflicts should be(0)
      task.status.throttledTime should be(0.millis)
      task.status.noops should be(0)
      task.status.retries should be(Retries(0, 0))
    }

    "return task information when error occurs" in {

      val start = System.currentTimeMillis()

      // Build an invalid query to make the update by query task fail
      val query = (0 until 200000)
        .flatMap(i => Seq(TermQuery("foo", i.toString), TermQuery("moo", (i+1).toString), TermQuery("goo" ,(i+2).toString)))

      // kick off a task
      val resp: GetTask = client.execute {
        updateByQueryAsync("get_task_a", BoolQuery(should = query))
      }.await.result.task

      val before = System.currentTimeMillis()

      // Sorry for this
      var result: GetTaskResponse = null
      var found = false

      // Retry to get the task until it is executed
       while (!found) {
         // use the task id from the above task
         result = client.execute {
          getTask(resp.nodeId, resp.taskId)
        }.await.result
         val elapsed = System.currentTimeMillis() - before
         found = result.completed || (elapsed > 60000)
         Thread.sleep(1000)
      }

      result.task.node shouldBe resp.nodeId
      result.task.id shouldBe resp.taskId
      result.task.`type` shouldBe "transport"
      result.task.action shouldBe "indices:data/write/update/byquery"
      result.task.startTimeInMillis >= start shouldBe true
      result.error.get.failedShards.length should be > 0
      val reason = result.error.get.failedShards.head.reason.get
      reason.`type` shouldBe "query_shard_exception"
    }
  }
}


