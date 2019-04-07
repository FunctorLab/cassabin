package cassabin

import cats.effect.{ContextShift, IO, Timer}
import org.scalatest.FreeSpec

class FpSpec extends FreeSpec {
  def ec = scala.concurrent.ExecutionContext.global
  implicit def contextShift:ContextShift[IO] = IO.contextShift(ec)
  implicit def timer:Timer[IO] = IO.timer(ec)
}
