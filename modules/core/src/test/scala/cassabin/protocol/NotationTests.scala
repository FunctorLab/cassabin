package cassabin.protocol

import java.util.UUID

import cassabin.protocol.notations.{BytesPayload, Consistency, InetType, ValuePayload}
import scodec.bits._
import scodec.{Codec, codecs => s}
import cassabin.protocol.{notations => n}
import org.scalatest.FreeSpec

/** spec according to section 3. */
final class NotationTests extends FreeSpec {
  "[int]" - {
    val codec = n.int.complete
    "zero" in {
      val r = codec.decodeValue(hex"0x00000000".toBitVector).getOrElse(fail())
      assert(r == 0)
    }
    "not enough bits" in {
      val r = codec.decodeValue(hex"0x000F".toBitVector)
      assert(r.isFailure)
    }
    "too many bits" in {
      val r = codec.complete.decodeValue(hex"0x123412341234".toBitVector)
      assert(r.isFailure)
    }
  }

  "[long]" - {
    val codec = n.long.complete
    "zero" in {
      val r = codec.decodeValue(hex"0x0000000000000000".toBitVector).getOrElse(fail())
      assert(r == 0)
    }
    "not enough bits" in {
      val r = codec.decodeValue(hex"0x000F".toBitVector)
      assert(r.isFailure)
    }
    "too many bits" in {
      val r = codec.decodeValue(hex"0x0000000000000000F".toBitVector)
      assert(r.isFailure)
    }
  }

  "[byte]" - {
    val codec = n.byte.complete
    "max" in {
      val r = codec.decodeValue(hex"0xFF".toBitVector).getOrElse(fail())
      assert(r == 255)
    }
    "too many bits" in {
      val r = codec.decodeValue(hex"0x00F".toBitVector)
      assert(r.isFailure)
    }
  }

  "[short]" - {
    val codec = n.short.complete
    "zero" in {
      val r = codec.decodeValue(hex"0x0000".toBitVector).getOrElse(fail())
      assert(r == 0)
    }
    "unsigned" in {
      val r = codec.decodeValue(hex"0xFFFF".toBitVector).getOrElse(fail())
      assert(r == 65535)
    }
    "not enough bits" in {
      val r = codec.decodeValue(hex"0x0F".toBitVector)
      assert(r.isFailure)
    }
    "too many bits" in {
      val r = codec.decodeValue(hex"0x0000F".toBitVector)
      assert(r.isFailure)
    }
  }

  "[string]" - {
    val codec = n.string.complete
    "'hello'" in {
      val r = codec.decodeValue((hex"0x0005" ++ hex"68656c6c6f").toBitVector).getOrElse(fail())
      assert(r == "hello")
    }

    "too short" in {
      val r = codec.decodeValue((hex"0x0005" ++ hex"68656").toBitVector)
      assert(r.isFailure)
    }
  }

  "[long string]" - {
    val codec = n.longString.complete
    "'hello'" in {
      val r = codec.decodeValue((hex"0x00000005" ++ hex"68656c6c6f").toBitVector).getOrElse(fail())
      assert(r == "hello")
    }

    "too short" in {
      val r = codec.decodeValue((hex"0x0005" ++ hex"68656").toBitVector)
      assert(r.isFailure)
    }
  }

  "[uuid]" - {
    val codec = n.uuid.complete
    "16bytes" in {
      val fourByte = hex"0x000F"
      val eightByte = fourByte ++ fourByte
      val r = codec.decodeValue((eightByte++eightByte).toBitVector).getOrElse(fail())
      assert(r.payload == 4222189076152335L)
    }
  }

  "[string list]" - {
    val codec = n.stringList.complete
    "['hello']" in {
      val r = codec.decodeValue((hex"0x0001" ++ hex"0005" ++ hex"68656c6c6f").toBitVector).getOrElse(fail())
      assert(r == List("hello"))
    }

    "['hello', 'world']" in {
      val r = codec.decodeValue((hex"0x0002" ++ hex"0005" ++ hex"68656c6c6f" ++ hex"0005" ++ hex"776f726c64").toBitVector).getOrElse(fail())
      assert(r == List("hello", "world"))
    }
  }

  "[bytes]" - {
    val codec = n.bytes.complete

    "n < 0" in {
      val r = codec.decodeValue(hex"0xffffffff".toBitVector).getOrElse(fail())
      assert(r == BytesPayload.NullBytes)
    }

    "n = 0" in {
      val r = codec.decodeValue(hex"0x00000000".toBitVector).getOrElse(fail())
      assert(r == BytesPayload.BytesHolder(Nil))
    }

    "n > 0" in {
      val r = codec.decodeValue((hex"0x00000002" ++ hex"FF" ++ hex"FF").toBitVector).getOrElse(fail())
      assert(r == BytesPayload.BytesHolder(List(255, 255)))
    }
  }

  "[value]" - {
    val codec = n.value.complete
    "n = -1" in {
      val r = codec.decodeValue(hex"0xffffffff".toBitVector).getOrElse(fail())
      assert(r == ValuePayload.NullValue)
    }

    "n = -2" in {
      val r = codec.decodeValue(hex"0xfffffffe".toBitVector).getOrElse(fail())
      assert(r == ValuePayload.NotSetValue)
    }

    "n <= -3" in {
      val r = codec.decodeValue(hex"0xfffffff0".toBitVector).getOrElse(fail())
      assert(r == ValuePayload.ErrValue)
    }

    "n = 0" in {
      val r = codec.decodeValue(hex"0x00000000".toBitVector).getOrElse(fail())
      assert(r == ValuePayload.ValueHolder(Nil))
    }

    "n = 2" in {
      val r = codec.decodeValue((hex"0x00000002" ++ hex"FF" ++ hex"FF").toBitVector).getOrElse(fail())
      assert(r == ValuePayload.ValueHolder(List(255, 255)))
    }
  }

  "[short bytes]" - {
    val codec = n.shortBytes.complete
    "n = 0" in {
      val r = codec.decodeValue(hex"0x0000".toBitVector).getOrElse(fail())
      assert(r == List.empty)
    }

    "n > 0" in {
      val r = codec.decodeValue((hex"0x0002" ++ hex"FF" ++ hex"FF").toBitVector).getOrElse(fail())
      assert(r == List(255, 255))
    }
  }

  "[unsigned vint]" - {
    val codec = n.unsignedVInt.complete
    "from spec" in {
      val r = codec.decodeValue(bin"110" ++ bin"00011" ++ bin"11101000" ++ bin"00000000").getOrElse(fail())
      assert(r == 256000)
    }

    "min" in {
      val r = codec.decodeValue(bin"00000000").getOrElse(fail())
      assert(r == 0)
    }

    // todo: discrepancy in spec, if it's unsigned it can't fit into long
    //       should it be BigInt?
    "max" ignore {
      val number = (hex"0xff" ++ hex"0xff" ++ hex"0xff" ++ hex"0xff" ++ hex"0xff" ++ hex"0xff" ++ hex"0xff" ++ hex"0xff").toBitVector
      val r = codec.decodeValue(bin"11111111" ++ number).getOrElse(fail())
      assert(r == -1)
    }
  }

  "[vint]" - {
    val codec = n.vint.complete
    // todo: implement
    "from spec" ignore {
      assert(codec.decodeValue(hex"0x00".toBitVector).getOrElse(fail()) == 0)
      assert(codec.decodeValue(hex"0x01".toBitVector).getOrElse(fail()) == -1)
      assert(codec.decodeValue(hex"0x02".toBitVector).getOrElse(fail()) == 1)
      assert(codec.decodeValue(hex"0x03".toBitVector).getOrElse(fail()) == -2)
      assert(codec.decodeValue(hex"0x04".toBitVector).getOrElse(fail()) == 2)
      assert(codec.decodeValue(hex"0x05".toBitVector).getOrElse(fail()) == -3)
      assert(codec.decodeValue(hex"0x06".toBitVector).getOrElse(fail()) == 3)
    }
  }
  "[options]" - {
    val codec = n.options.complete
  }
  "[option list]" - {
    val codec = n.optionList.complete
  }
  "[inet addr]" - {
    val codec = n.inetAddr.complete
    "invalid addrSize" in {
      val r = codec.decodeValue((hex"0x03" ++ hex"ff" ++ hex"ff" ++ hex"ff").toBitVector)
      assert(r.isFailure)
    }
    "ipv4" in {
      val r = codec.decodeValue((hex"0x04" ++ hex"ff" ++ hex"ff" ++ hex"ff" ++ hex"ff").toBitVector).getOrElse(fail())
      assert(r.tpe == InetType.Ipv4Type)
      assert(r.addresses == List(255, 255, 255, 255))
    }
    "ipv6" in {
      val fourBytes = hex"ff" ++ hex"ff" ++ hex"ff" ++ hex"ff"
      val r = codec.decodeValue((hex"0x10" ++ fourBytes ++ fourBytes ++ fourBytes ++ fourBytes).toBitVector).getOrElse(fail())
      assert(r.tpe == InetType.Ipv16Type)
      assert(r.addresses == List(
        255,255,255,255,
        255,255,255,255,
        255,255,255,255,
        255,255,255,255))
    }
  }
  "[inet]" - {
    val codec = n.inet.complete
    "invalid addrSize" in {
      val r = codec.decodeValue((hex"0x03" ++ hex"ff" ++ hex"ff" ++ hex"ff").toBitVector)
      assert(r.isFailure)
    }
    "missing port" in {
      val r = codec.decodeValue((hex"0x04" ++ hex"ff" ++ hex"ff" ++ hex"ff" ++ hex"ff").toBitVector)
      assert(r.isFailure)
    }
    "ipv4" in {
      val port = hex"0x00002000"
      val r = codec.decodeValue((hex"0x04" ++ hex"ff" ++ hex"ff" ++ hex"ff" ++ hex"ff" ++ port).toBitVector).getOrElse(fail())
      assert(r.port == 8192)
      assert(r.addr.tpe == InetType.Ipv4Type)
      assert(r.addr.addresses == List(255, 255, 255, 255))
    }
    "ipv6" in {
      val port = hex"0x00002000"
      val fourBytes = hex"ff" ++ hex"ff" ++ hex"ff" ++ hex"ff"
      val r = codec.decodeValue((hex"0x10" ++ fourBytes ++ fourBytes ++ fourBytes ++ fourBytes ++ port).toBitVector).getOrElse(fail())
      assert(r.port == 8192)
      assert(r.addr.tpe == InetType.Ipv16Type)
      assert(r.addr.addresses == List(
        255,255,255,255,
        255,255,255,255,
        255,255,255,255,
        255,255,255,255))
    }
  }
  "[consistency]" - {
    val codec = n.consistency.complete
    "hardcoded" in {
      val r = codec.decodeValue(hex"0x0005".toBitVector).getOrElse(fail())
      assert(r == Consistency.All)
    }
    Consistency.all.foreach { cons =>
      cons.name in {
        val r = codec.decodeValue(n.short.encode(cons.id).getOrElse(fail())).getOrElse(fail())
        assert(r == cons)
      }
    }
  }

  val helloStr = hex"0x0005" ++ hex"68656c6c6f"
  val worldStr = hex"0x0005" ++ hex"776f726c64"
  val hhStr = hex"0x0002" ++ hex"6868"
  val ooStr = hex"0x0002" ++ hex"6f6f"
  val llStr = hex"0x0002" ++ hex"6c6c"
  val eeStr = hex"0x0002" ++ hex"6565"

  "[string map]" - {
    val codec = n.stringMap.complete
    "basic" in {
      val amount = hex"0x0002"
      val r = codec.decodeValue((amount ++ helloStr ++ hhStr ++ ooStr ++ llStr).toBitVector).getOrElse(fail())
      assert(r.size == 2)
      assert(r("hello") == "hh")
      assert(r("oo") == "ll")
    }
  }
  "[string multimap]" - {
    val codec = n.stringMultiMap.complete
    "basic" in {
      val amount = hex"0x0002"
      val helloWorldStringList = hex"0x0002" ++ helloStr ++ worldStr
      val helloWorldHelloStringList = hex"0x0003" ++ helloStr ++ worldStr ++ helloStr
      val fst = hhStr ++ helloWorldStringList
      val snd = eeStr ++ helloWorldHelloStringList
      val r = codec.decodeValue((amount ++ fst ++ snd).toBitVector).getOrElse(fail())
      assert(r.size == 2)
      assert(r("hh") == List("hello", "world"))
      assert(r("ee") == List("hello", "world", "hello"))
    }
  }
  "[bytes map]" - {
    val codec = n.bytesMap.complete
    "basic" in {
      val amount = hex"0x0002"
      val fst = hex"0x00000002" ++ hex"FF" ++ hex"FF"
      val snd = hex"0x00000000"
      val r = codec.decodeValue((amount ++ helloStr ++ fst ++ worldStr ++ snd).toBitVector).getOrElse(fail())
      assert(r.size == 2)
      assert(r("hello") == BytesPayload.BytesHolder(List(255, 255)))
      assert(r("world") == BytesPayload.BytesHolder(Nil))
    }
  }
}
