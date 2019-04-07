package cassabin.common

import cassabin.protocol.FlagsPayload._
import cassabin.protocol._
import cassabin.{FpSpec, LocalCassandra}
import cats.effect.IO

final class ConcurrentMapImplTests extends FpSpec {

  case class Holder(x:Int)

  "ConcMap" - {
    "putIfAbsent, get" in {
      val map = ConcurrentMap[IO,String,Holder].unsafeRunSync()

      val prog = for {
        _ <- map.getOrPutIfAbsent("key", Holder(100))
        p <- map.get("key")
      } yield p

      val res = prog.unsafeRunSync()
      assert(res.contains(Holder(100)))
    }
  }
}
