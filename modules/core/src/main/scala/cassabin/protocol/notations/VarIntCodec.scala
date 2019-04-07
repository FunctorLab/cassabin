package cassabin.protocol.notations

import scodec.bits.BitVector
import scodec.{Attempt, Codec, DecodeResult, SizeBound}

private[notations] final class VarIntCodec extends Codec[Int] {
  override def encode(value: Int): Attempt[BitVector] = ???
  override def sizeBound: SizeBound = ???
  override def decode(bits: BitVector): Attempt[DecodeResult[Int]] = ???
  override def toString = "variable-length integer"
}

object VarIntCodec {
}

