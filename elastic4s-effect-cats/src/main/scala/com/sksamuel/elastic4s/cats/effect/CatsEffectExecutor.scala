package com.sksamuel.elastic4s.cats.effect

import cats.effect.{Async, IO}
import com.sksamuel.elastic4s.{ElasticRequest, Executor, HttpClient, HttpResponse}

class CatsEffectExecutor[F[_]: Async] extends Executor[F] {
  override def exec(client: HttpClient, request: ElasticRequest): F[HttpResponse] =
    Async[F].async[HttpResponse] { k =>
      client.send(request, k)
      Async[F].pure(Some(Async[F].unit))
    }
}

@deprecated("Use CatsEffectExecutor[IO] instead")
class IOExecutor extends CatsEffectExecutor[IO]
