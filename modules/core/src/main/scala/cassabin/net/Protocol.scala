package cassabin.net

import cassabin.common._
import cassabin.protocol.FlagsPayload._
import cassabin.protocol._
import cassabin.protocol.frame.StartupRequest
import fs2._
import cats._
import cats.effect.Concurrent
import cats.effect.concurrent.Semaphore
import cats.implicits._

/** safe to be used by multiple threads */
trait Protocol[F[_]] {

  def startup:F[Unit]

  def events(bufferSize:Int):Stream[F, Any]

}

private final class ProtocolImpl[F[_]:Concurrent](msSocket:ProtocolledMessageSocket[F],
                                                  idGen:StreamIdGenerator[F],
                                                  semaphore:Semaphore[F]) extends Protocol[F] {
  def startup: F[Unit] = for {
    id <- idGen.next
    startupReq = StartupRequest(4, FlagsPayload(TracingFlag), id, Map("CQL_VERSION" -> "3.0.0"))
    _ <- msSocket.send(startupReq)
/*    resp <- msSocket.receiveA(id) {
      case ReadyResponse(h) => ???
      //case Authenticate
    }*/
  } yield ()

  def events(bufferSize: Int): Stream[F, Any] = ???
}

object Protocol {
  def fromMessageSocket[F[_]:Concurrent](x:ProtocolledMessageSocket[F]):F[Protocol[F]] = for {
    idGen <- StreamIdGenerator[F]
    sema <- Semaphore[F](1)
  } yield new ProtocolImpl(x, idGen, sema)
}


