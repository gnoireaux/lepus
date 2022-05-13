/*
 * Copyright 2021 Hossein Naderi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lepus.client

import cats.Functor
import cats.MonadError
import cats.effect.*
import cats.effect.std.Console
import com.comcast.ip4s.Host
import com.comcast.ip4s.SocketAddress
import fs2.Pipe
import fs2.Stream
import fs2.Stream.*
import fs2.interop.scodec.*
import fs2.io.net.Network
import fs2.io.net.Socket
import fs2.io.net.SocketOption
import lepus.protocol.ProtocolVersion
import lepus.protocol.frame.*
import lepus.wire.FrameCodec
import scodec.codecs.logFailuresToStdOut

import scala.{Console => SCon}

type Transport[F[_]] = Pipe[F, Frame, Frame]
object Transport {
  def decoderServer[F[_]](using F: MonadError[F, Throwable]) =
    StreamDecoder
      .once(logFailuresToStdOut(FrameCodec.protocol))
      .flatMap(pv => StreamDecoder.many(logFailuresToStdOut(FrameCodec.frame)))
      .toPipeByte

  def decoder[F[_]](using F: MonadError[F, Throwable]) =
    StreamDecoder.many(logFailuresToStdOut(FrameCodec.frame)).toPipeByte

  def encoder[F[_]](using F: MonadError[F, Throwable]) =
    StreamEncoder.many(logFailuresToStdOut(FrameCodec.frame)).toPipeByte

  inline def debug[F[_]: Functor](
      send: Boolean
  )(using C: Console[F]): Pipe[F, Frame, Frame] =
    inline if send then
      _.evalTap(f => C.println(s"${SCon.CYAN}S> $f${SCon.RESET}"))
    else _.evalTap(f => C.println(s"${SCon.GREEN}C> $f${SCon.RESET}"))

  def build[F[_]: Concurrent](
      reads: Stream[F, Byte],
      writes: Pipe[F, Byte, Nothing]
  ): Transport[F] =
    toSend =>
      val recv = reads.through(decoderServer)
      val send = toSend.through(encoder).through(writes)
      recv mergeHaltBoth send

  def fromSocket[F[_]: Concurrent](socket: Socket[F]): Transport[F] =
    build(socket.reads, socket.writes)

  def connect[F[_]: Concurrent: Network](
      address: SocketAddress[Host],
      options: List[SocketOption] = List.empty
  ): Transport[F] = in =>
    resource(Network[F].client(address, options))
      .flatMap(fromSocket(_).apply(in))
}
