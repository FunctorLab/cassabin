package example

import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup

import cassabin.protocol.FlagsPayload._
import cassabin.protocol.frame.Frame._
import cassabin.protocol._
import cats.effect.{ContextShift, IO}
import scodec._
import scodec.bits._
import codecs._
import fs2.io.tcp._
import fs2._
import java.util.concurrent._

import cassabin.net.{BitVectorSocket, MessageSocket}
import cats.effect.concurrent.MVar


object Main extends App {
}

