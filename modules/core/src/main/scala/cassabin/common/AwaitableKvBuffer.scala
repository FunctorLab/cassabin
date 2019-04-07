package cassabin.common

import cats.effect._
import cats.effect.concurrent.Deferred
import cats.implicits._
import fs2.Pipe

trait AwaitableKvBuffer[F[_], K, V] {
  def put1(k: K, v: V): F[Unit]
  def put: Pipe[F, (K, V), Unit]
  def waitFor(k:K):F[V]
}

private final class AwaitableKvBufferImpl[F[_], K, V](maxEntries:Int, concMap:ConcurrentMap[F, K, Deferred[F, V]])(implicit sync:Concurrent[F]) extends AwaitableKvBuffer[F, K, V] {
  def put1(k: K, v: V): F[Unit] = for {
    _ <- sync.ifM(concMap.size.map(_ > maxEntries))(sync.raiseError[Unit](new Exception(s"Buffer is full, reached number of $maxEntries")), sync.pure(()))
    dr <- Deferred[F, V]
    p <- concMap.getOrPutIfAbsent(k, dr)
    pg <- p.complete(v)
  } yield pg

  def put: Pipe[F, (K, V), Unit] = _.evalMap(x => put1(x._1, x._2))

  def waitFor(k: K): F[V] = for {
    dr <- Deferred[F, V]
    p <- concMap.getOrPutIfAbsent(k, dr)
    pg <- p.get <* concMap.remove(k)
  } yield pg
}

object AwaitableKvBuffer {
  def apply[F[_], K, V](maxEntries:Int)(implicit sync:Concurrent[F]):F[AwaitableKvBuffer[F, K, V]] = {
    for {
      conc <- ConcurrentMap[F, K, Deferred[F, V]]
    } yield new AwaitableKvBufferImpl(maxEntries, conc)
  }
}
