package cassabin.protocol.notations

import cassabin.common._
import scodec.bits.BitVector
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound, codecs => s}

import scala.util.Try

private[notations] final class VarUIntCodec extends Codec[Long] {
  override def encode(value: Long): Attempt[BitVector] = ???
  override def sizeBound: SizeBound = ???
  override def decode(bits: BitVector): Attempt[DecodeResult[Long]] = {
    bits.consumeWhileBit(x => x.last && x.sizeLessThanOrEqual(8)) { (l, r) =>
      highBits(l).flatMap { sizeBits =>
        val noZeroDelimiter = if (sizeBits < 8) r.drop(1) else r
        val paddingLeft = Math.min(sizeBits + 1, 8) // number of ones + delimiter
        val remainder = 8-paddingLeft + sizeBits * 8
        s.long(remainder).decode(noZeroDelimiter)
      }
    }
  }

  private def highBits(vec:BitVector):Attempt[Int] = {
    val p = vec.populationCount
    def failure = Attempt.failure[Int](Err.General(s"$p has too many bits", Nil))
    Try(p.toInt).fold(_ => failure, x => Attempt.successful(x))
  }
  override def toString = "variable-length unsigned integer"
}

object VarUIntCodec {
}

