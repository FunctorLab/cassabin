package cassabin

import cassabin.protocol.{Header, LengthPayload}
import scodec._
import scodec.bits.BitVector

package object protocol {
  def emptyEncoder[A]:Encoder[A] = Encoder[A]{ _:A => Attempt.successful(BitVector.empty)}
}

trait RequestEncoder[A] {
  def headerEncoder:Encoder[A]
  def bodyEncoder:Encoder[A]

  final def encode(a:A):Attempt[BitVector] = {
    for {
      header <- headerEncoder.encode(a)
      bodyVec <- bodyEncoder.encode(a)
      lenVec <- LengthPayload.codec.encode(LengthPayload(bodyVec.toByteVector.size.toInt)) // todo: toInt
    } yield header ++ lenVec ++ bodyVec
  }
}

object RequestEncoder {
}
