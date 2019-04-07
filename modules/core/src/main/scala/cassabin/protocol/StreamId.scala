package cassabin.protocol

import scodec.codecs._

import cassabin.protocol.StreamSource._

sealed abstract class StreamSource extends Product with Serializable
object StreamSource {
  case object InitiatedByClient extends StreamSource
  case object InitiatedByServer extends StreamSource
}
final case class StreamId(streamId:Short) {
  val initiatedBy:StreamSource = if (streamId >= 0) InitiatedByClient else InitiatedByServer
}
object StreamId {
  val codec = short16.xmap(StreamId.apply, (x:StreamId) => x.streamId)
}
