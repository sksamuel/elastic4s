package com.sksamuel.elastic4s.requests.update

import com.sksamuel.elastic4s.Response
import com.sksamuel.elastic4s.requests.common.RefreshPolicy
import com.sksamuel.elastic4s.requests.task.{GetTask, GetTaskResponse}
import com.sksamuel.elastic4s.testkit.DockerTests
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class UpdateByQueryTest
  extends AnyFlatSpec
    with Matchers
    with DockerTests
    with OptionValues {

  Try {
    client.execute {
      deleteIndex("pop")
    }.await
  }

  Try {
    client.execute {
      createIndex("pop")
    }.await
  }

  client.execute {
    bulk(
      indexInto("pop").fields("name" -> "sprite", "type" -> "lemonade", "foo" -> "f"),
      indexInto("pop").fields("name" -> "fanta", "type" -> "orangeade", "foo" -> "f"),
      indexInto("pop").fields("name" -> "pepsi", "type" -> "cola", "foo" -> "f")
    ).refreshImmediately
  }.await

  "an update by query request" should "support script based update" in {

    client.execute {
      updateByQuery("pop", matchAllQuery()).script(script("ctx._source.foo = 'a'").lang("painless")).refreshImmediately
    }.await.result.updated shouldBe 3

    client.execute {
      count("pop").query(termQuery("foo", "a"))
    }.await.result.count shouldBe 3
  }

  it should "return errors if the update fails" in {
    client.execute {
      updateByQuery("pop", prefixQuery("name", "spr")).script("wibble")
    }.await.error.`type` shouldBe "script_exception"
  }

  it should "support RefreshPolicy.IMMEDIATE" in {
    client.execute {
      updateByQuery("pop", matchAllQuery()).script(script("ctx._source.foo = 'b'").lang("painless")).refresh(RefreshPolicy.IMMEDIATE)
    }.await.result.updated shouldBe 3

    client.execute {
      count("pop").query(termQuery("foo", "b"))
    }.await.result.count shouldBe 3
  }

  it should "support slices auto" in {
    client.execute {
      updateByQuery("pop", matchAllQuery()).script(script("ctx._source.foo = 'd'").lang("painless")).slicesAuto().refresh(RefreshPolicy.IMMEDIATE)
    }.await.result.updated shouldBe 3

    client.execute {
      count("pop").query(termQuery("foo", "d"))
    }.await.result.count shouldBe 3
  }
  
  it should "support RefreshPolicy.NONE" in {
    client.execute {
      updateByQuery("pop", matchAllQuery()).script(script("ctx._source.foo = 'c'").lang("painless")).refresh(RefreshPolicy.NONE)
    }.await.result.updated shouldBe 3

    client.execute {
      count("pop").query(termQuery("foo", "c"))
    }.await.result.count shouldBe 0
  }

  it should "support asynchronous update" in {
    client.execute {
      bulk(
        indexInto("pop").fields("name" -> "coca", "type" -> "cola", "foo" -> "g"),
      ).refreshImmediately
    }.await

    val task = client.execute {
      updateByQueryAsync("pop", termsQuery("name", "coca")).script(script("ctx._source.foo = 'h'").lang("painless")).refreshImmediately
    }.await.result.task

    // A bit ugly way to poll the task until it's complete
    Stream.continually{
      Thread.sleep(100)
      client.execute(task).await.result.completed
    }.takeWhile(!_)

    client.execute {
      count("pop").query(termQuery("foo", "h"))
    }.await.result.count shouldBe 1
  }
}
