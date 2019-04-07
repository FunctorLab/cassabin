package cassabin.net

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import cats._
import cats.implicits._
import cats.effect._
import fs2.Chunk
import scodec.bits.BitVector
import fs2.io.tcp

import scala.concurrent.duration._

trait BitVectorSocket {
  /** writes BitVector into socket */
  def write(b:BitVector):IO[Unit]
  /** read n bits from socket */
  def read(numBytes:Int):IO[BitVector]
}

private class BitVectorSocketImpl(socket:tcp.Socket[IO], readTimeout:FiniteDuration, writeTimeout:FiniteDuration)
                                 (implicit me:MonadError[IO, Throwable]) extends BitVectorSocket {
  override def read(n: Int): IO[BitVector] = socket.readN(n, Some(readTimeout)).flatMap {
    case Some(b) =>
      if (b.size == n) BitVector(b.toArray).pure[IO]
      else me.raiseError(new Exception(s"Required $n bytes but received ${b.size}"))
    case None =>
      me.raiseError(new Exception(s"Required $n bytes but received none"))
  }

  override def write(b: BitVector): IO[Unit] = {
    val chunk = Chunk.array[Byte](b.toByteArray)
    socket.write(chunk, Some(writeTimeout))
  }
}

object BitVectorSocket {
  def apply(host:String, port:Int): Resource[IO, BitVectorSocket] = {
    val ec = scala.concurrent.ExecutionContext.global
    implicit val cs:ContextShift[IO] = IO.contextShift(ec)

    val channelGroup:Resource[IO, AsynchronousChannelGroup] = {
      val acq = IO.delay(AsynchronousChannelGroup.withThreadPool(Executors.newCachedThreadPool()))
      val rel = (x:AsynchronousChannelGroup) => IO.delay(x.shutdownNow())
      Resource.make(acq)(rel)
    }

    val socket:Resource[IO, tcp.Socket[IO]] = channelGroup.flatMap { implicit ag =>
      tcp.Socket.client[IO](new InetSocketAddress(host, port))
    }

    socket.map(fromSocket(_, 5.days, 10.seconds))
  }

  def fromSocket(socket:tcp.Socket[IO], readTimeout:FiniteDuration, writeTimeout:FiniteDuration):BitVectorSocket = {
    new BitVectorSocketImpl(socket, readTimeout, writeTimeout)
  }
}
