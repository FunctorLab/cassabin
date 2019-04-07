package cassabin.protocol

import cassabin.protocol.FlagsPayload.{TracingFlag, WarningFlag}
import cassabin.protocol.MessageDirection._
import cassabin.protocol.StreamSource._
import org.scalatest.FreeSpec
import scodec.bits._

final class ProtocolHeaderTests extends FreeSpec {
  "header" - {
    "version" - {
      val codec = Version.codec.complete
      "request" in {
        val r = codec.decodeValue(hex"0x05".toBitVector).getOrElse(fail())
        assert(r.direction == Request)
        assert(r.protocolVersion == 5)
      }

      "response" in {
        val r = codec.decodeValue(hex"0x85".toBitVector).getOrElse(fail())
        assert(r.direction == Response)
        assert(r.protocolVersion == 5)
      }

      "protocol version" in {
        val r = codec.decodeValue(hex"0x95".toBitVector).getOrElse(fail())
        assert(r.direction == Response)
        assert(r.protocolVersion == 16 + 5)
      }
    }

    "flags" - {
      val codec = FlagsPayload.codec.complete
      "true" in {
        val r = codec.decodeValue(hex"0xFF".toBitVector).getOrElse(fail())
        assert(FlagsPayload.flags.forall(x => r.flag(x)))
      }

      "false" in {
        val r = codec.decodeValue(hex"0x00".toBitVector).getOrElse(fail())
        assert(FlagsPayload.flags.forall(x => !r.flag(x)))
      }

      "tracing" in {
        val r = codec.decodeValue(BitVector.fromByte(FlagsPayload.TracingFlag.byte)).getOrElse(fail())
        assert(r.flag(TracingFlag))
        val vals = r.flagsMap.values
        assert(vals.count(_ == false) == vals.size - 1)
      }

      "tracing, warning" in {
        val r = codec.decodeValue(hex"0x0A".toBitVector).getOrElse(fail())
        assert(r.flag(TracingFlag))
        assert(r.flag(WarningFlag))
        val vals = r.flagsMap.values
        assert(vals.count(_ == false) == vals.size - 2)
      }
    }

    "stream" - {
      val codec = StreamId.codec.complete
      "client msg" in {
        val r = codec.decodeValue(hex"0x00FF".toBitVector).getOrElse(fail())
        assert(r.initiatedBy == InitiatedByClient)
        assert(r.streamId == 255)
      }

      "server msg" in {
        val r = codec.decodeValue(hex"0xFFF6".toBitVector).getOrElse(fail())
        assert(r.initiatedBy == InitiatedByServer)
        assert(r.streamId == -10)
      }

      "max id" in {
        val r = codec.decodeValue(hex"0x7FFF".toBitVector).getOrElse(fail())
        assert(r.initiatedBy == InitiatedByClient)
        assert(r.streamId == 32767)
      }

      "min id" in {
        val r = codec.decodeValue(hex"0x8000".toBitVector).getOrElse(fail())
        assert(r.initiatedBy == InitiatedByServer)
        assert(r.streamId == -32768)
      }
    }

    "opcode" - {
      val codec = OpCode.codec.complete
      "hardcoded" in {
        val r = codec.decodeValue(hex"0x0D".toBitVector).getOrElse(fail())
        assert(r == OpCode.OpBatch)
      }
      OpCode.all.foreach { opCode =>
        opCode.name in {
          val r = codec.decodeValue(BitVector.fromByte(opCode.id)).getOrElse(fail())
          assert(r == opCode)
        }
      }
    }

    "length" - {
      val codec = LengthPayload.codec.complete
      "value" in {
        val r = codec.decodeValue(hex"0x00F0F0F0".toBitVector).getOrElse(fail())
        assert(r.bodyLength == 15790320)
      }
    }
  }
}
