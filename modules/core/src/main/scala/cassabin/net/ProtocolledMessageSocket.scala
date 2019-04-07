package cassabin.net
import cassabin.RequestEncoder
import cassabin.protocol.frame._
import cassabin.protocol._
import cassabin.common.AwaitableKvBuffer
import cassabin.protocol.frame.{FrameRequest, FrameResponse}
import cats._
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import fs2._
import fs2.concurrent._

/** messages paired with request-response by their ids */
trait ProtocolledMessageSocket[F[_]] {
  def send[A <: FrameRequest : RequestEncoder](a: A): F[Unit]
  def receive(id:StreamId): F[FrameResponse]
  def receiveA[B](id:StreamId)(f:PartialFunction[FrameResponse, B]):F[B]
  def flatReceiveA[B](id:StreamId)(f:PartialFunction[FrameResponse, F[B]]):F[B]
}

private[net] final class ProtocolledMessageSocketImpl[F[_]]
(socket:MessageSocket, buffer:AwaitableKvBuffer[F, StreamId, FrameResponse])
(implicit liftIO:Concurrent[F]) extends ProtocolledMessageSocket[F] {
  def send[A <: FrameRequest : RequestEncoder](a: A): F[Unit] = socket.send(a).to[F]
  def receive(id:StreamId): F[FrameResponse] = buffer.waitFor(id)
  def receiveA[B](id:StreamId)(f:PartialFunction[FrameResponse, B]):F[B] = buffer.waitFor(id).flatMap {
    case m if f.isDefinedAt(m) => f(m).pure[F]
    case m => liftIO.raiseError(new Throwable(s"Received invalid message type $m for $id"))
  }
  def flatReceiveA[B](id:StreamId)(f:PartialFunction[FrameResponse, F[B]]):F[B] = receiveA(id)(f).flatten
}

object ProtocolledMessageSocket {
  val maxEntries:Int = 10

  def fromMessageSocket[F[_]](socket:MessageSocket)(implicit conc:Concurrent[F]):F[ProtocolledMessageSocket[F]] = for {
    q <- AwaitableKvBuffer[F, StreamId, FrameResponse](maxEntries)
    _ <- Stream.eval(socket.receive.to[F]).map(x => x.header.stream -> x).repeat
            .through(q.put).compile.drain
            .attempt.flatMap {
              case Left(e) => conc.delay(e.printStackTrace)
              case Right(x) => x.pure[F]
            }
      .start
  } yield new ProtocolledMessageSocketImpl(socket, q)

  def apply[F[_]:Concurrent](host:String, port:Int):Resource[F, ProtocolledMessageSocket[F]] = {
    val socket = MessageSocket(host, port)
    for {
      ms <- socket.mapK(λ[IO ~> F](_.to))
      pms <- Resource.liftF(fromMessageSocket(ms))
    } yield pms
  }
}
