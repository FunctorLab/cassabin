package cassabin.common

import cats.effect.Sync
import cats._
import cats.implicits._
import java.util.concurrent.atomic._

import cassabin.protocol.StreamId

trait StreamIdGenerator[F[_]] {
  def next:F[StreamId]
}

private final class StreamIdCounterImpl[F[_]](private val back:AtomicInteger)(implicit sync:Sync[F]) extends StreamIdGenerator[F] {
  def next:F[StreamId] = sync.delay {
    StreamId((back.getAndIncrement() % Short.MaxValue).toShort)
  }
}

object StreamIdGenerator {
  def apply[F[_]](implicit sync:Sync[F]):F[StreamIdGenerator[F]] = {
    sync.delay(new StreamIdCounterImpl(new AtomicInteger(0)))
  }
}

