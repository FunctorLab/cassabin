package cassabin.net

import cassabin.{FpSpec, LocalCassandra}
import cassabin.protocol.FlagsPayload._
import cassabin.protocol._
import cassabin.protocol.frame.{OptionsRequest, StartupRequest}
import cats.implicits._
import cats.effect._
import org.scalatest.FreeSpec

final class MessageSocketIntegTests extends FpSpec with LocalCassandra {
  def socket = MessageSocket(cassandraIp, cassandraPort)

  val flags = FlagsPayload(TracingFlag, WarningFlag)
  val version = Version(MessageDirection.Request, 4)

  "can send and receive" - {
    "OPTIONS" in {
      val request = OptionsRequest(version.protocolVersion, flags, StreamId(1))

      val r = socket.use { socket =>
        for {
          _ <- socket.send(request)
          r <- socket.receive
        } yield r
      }.unsafeRunSync

      assert(r.header.stream.streamId == request.header.stream.streamId)
      assert(r.header.version.direction == MessageDirection.Response)
      assert(r.header.version.protocolVersion == request.header.version.protocolVersion)
      assert(r.header.opCode == OpCode.OpSupported)
    }

    "STARTUP" in {
      val request = StartupRequest(version.protocolVersion, flags, StreamId(2), Map("CQL_VERSION" -> "3.0.0"))

      val r = socket.use { socket =>
        for {
          _ <- socket.send(request)
          r <- socket.receive
        } yield r
      }.unsafeRunSync

      assert(r.header.stream.streamId == request.header.stream.streamId)
      assert(r.header.version.direction == MessageDirection.Response)
      assert(r.header.version.protocolVersion == request.header.version.protocolVersion)
      assert(r.header.opCode == OpCode.OpReady)
    }
  }

}
