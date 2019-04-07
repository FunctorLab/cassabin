package cassabin.net

import cassabin.{FpSpec, LocalCassandra}
import cassabin.protocol.FlagsPayload._
import cassabin.protocol._
import cassabin.protocol.frame.{OptionsRequest, StartupRequest}
import cats.effect.{IO, Resource}
import org.scalatest.FreeSpec
import cats.implicits._

import scala.concurrent.duration._

final class ProtocolledMessageSocketIntegTests extends FpSpec with LocalCassandra {
  def socket:Resource[IO, ProtocolledMessageSocket[IO]] = ProtocolledMessageSocket(cassandraIp, cassandraPort)

  val flags = FlagsPayload(TracingFlag, WarningFlag)
  val version = Version(MessageDirection.Request, 4)

  val request1 = OptionsRequest(version.protocolVersion, flags, StreamId(1))
  val request2 = StartupRequest(version.protocolVersion, flags, StreamId(2), Map("CQL_VERSION" -> "3.0.0"))

  "can send and receive" - {
    "multiple" in {
      val prog = socket.use { sckt =>
        for {
          _ <- sckt.send(request1)
          _ <- sckt.send(request2)
          a <- sckt.receive(request1.streamId)
          b <- sckt.receive(request2.streamId)
        } yield (a,b)
      }

      val (resp1,resp2) = prog.unsafeRunSync()
      assert(resp1.header.stream == request1.streamId)
      assert(resp2.header.stream == request2.streamId)
    }

    "reverse" in {
      val prog = socket.use { sckt =>
        for {
          c <- sckt.receive(request1.streamId).start
          _ <- sckt.send(request1)
          cc <- c.join
        } yield cc
      }

      val res = prog.unsafeRunSync()
      assert(res.header.stream == request1.streamId)
    }

    "consumes msg" in {
      val prog = socket.use { sckt =>
        for {
          _ <- sckt.send(request1)
          a <- sckt.receive(request1.streamId)
          b <- IO.race(IO.sleep(500.millis), sckt.receive(request1.streamId)).map(_.toOption)
        } yield (a,b)
      }

      val (resp1,resp2) = prog.unsafeRunSync()
      assert(resp1.header.stream == request1.streamId)
      assert(resp2.isEmpty)
    }

    "reach limit" ignore {
      val prog = socket.use { sckt =>
        for {
          t <- (2 to ProtocolledMessageSocket.maxEntries+5).toList
            .map(id => sckt.send(request1.copy(streamId = StreamId(id.toShort))))
            .sequence
          a <- sckt.receive(request1.streamId)
        } yield a
      }

      val resp = prog.unsafeRunSync()
    }
  }

}
