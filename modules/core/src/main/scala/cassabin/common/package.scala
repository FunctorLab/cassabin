package cassabin

import scodec.bits.BitVector

package object common {

  implicit class BitVectorOps(val b: BitVector) extends AnyVal {
    /** just like [[scodec.bits.BitVector.consumeThen]] except instead of specific amount of bits
      * there is a condition
      */
    def consumeWhileBit[R](conditionLeft: BitVector => Boolean)(f: (BitVector, BitVector) => R): R = {
      // todo: not tailrec - probably slow
      def go(ll: BitVector, rr: BitVector): (BitVector, BitVector) = {
        val non = (ll, rr)
        rr.consumeThen(1)(_ => non, (l, r) => {
          val lll = ll ++ l
          if (conditionLeft(lll)) {
            go(lll, r)
          } else {
            non
          }
        })
      }

      val (r1, r2) = go(BitVector.empty, b)
      f(r1, r2)
    }
  }

  implicit class ByteUtils(val b: Byte) extends AnyVal {
    def msb: Boolean = b != 0

    def lsb: Boolean = (b & 1) == 1

    def zeroMsb: Byte = ((b << 1) >> 1).toByte

    def flag(a: Byte): Boolean = (b & a) != 0
  }

  implicit class IntUtils(val b: Int) extends AnyVal {
    def flag(a: Int): Boolean = (b & a) != 0
  }
}
