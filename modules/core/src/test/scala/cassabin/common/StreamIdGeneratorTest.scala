package cassabin.common

import cassabin.FpSpec
import cats.effect.IO
import cats._
import cats.implicits._

final class StreamIdGeneratorTest extends FpSpec {

  "StreamIdGenerator works" - {
    "concurrently for whole short range" in {
      val counter = StreamIdGenerator[IO].unsafeRunSync()

      val prog = for {
        p1 <- counter.next
        others <- (0 until Short.MaxValue).map(_ => counter.next).toList.parSequence
      } yield (p1, others)

      val (p1, others) = prog.unsafeRunSync()
      assert(p1.streamId == 0)
      assert(others.size == others.distinct.size)
    }
  }
}
