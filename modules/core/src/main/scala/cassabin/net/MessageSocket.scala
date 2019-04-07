package cassabin.net

import cassabin.RequestEncoder
import cassabin.protocol._
import cassabin.protocol.frame.{FrameRequest, FrameResponse}
import cats._
import cats.implicits._
import cats.effect.{IO, Resource}

/* Messages socket in terms of sending requests and reading responses. They are not necessarily paired by their ids
 * they are read as they are arriving.
 */
trait MessageSocket {
  /** sends message to Cassandra */
  def send[A <: FrameRequest: RequestEncoder](a:A):IO[Unit]
  /** receives the next message in line */
  def receive:IO[FrameResponse]
}

private[net] final class MessageSocketImpl(socket:BitVectorSocket)(implicit me:MonadError[IO, Throwable]) extends MessageSocket {
  private val headerCodec = Header.codec
  private val lengthCodec = LengthPayload.codec

  override def send[A](a: A)(implicit encA:RequestEncoder[A]): IO[Unit] = {
    val encoded = encA.encode(a)
    encoded.fold(err => me.raiseError(new Exception(s"Unable to encode for sending: ${err.messageWithContext}")), socket.write)
  }

  // todo: a bit of mehs around
  override def receive:IO[FrameResponse] = {
    val bytes = (headerCodec.sizeBound.lowerBound/8).toInt
    val lengthBytes = (lengthCodec.sizeBound.lowerBound/8).toInt
    for {
      headerBits <- socket.read(bytes)
      header = headerCodec.decodeValue(headerBits).getOrElse(???)
      bodyLengthBits <- socket.read(lengthBytes)
      bodyLength = lengthCodec.decodeValue(bodyLengthBits).getOrElse(???)
      bodyBits <- socket.read(bodyLength.bodyLength)
      body = FrameResponse.decodeHeader(header).decodeValue(bodyBits).getOrElse(???)
    } yield body
  }
}

object MessageSocket {
  def fromBitVectorSocket(socket: BitVectorSocket): MessageSocket = {
    new MessageSocketImpl(socket)
  }

  def apply(host: String, port: Int): Resource[IO, MessageSocket] = BitVectorSocket(host, port).map(fromBitVectorSocket)
}
