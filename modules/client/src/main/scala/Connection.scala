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

import cats.effect.*
import cats.effect.implicits.*
import cats.effect.std.Queue
import cats.effect.std.QueueSource
import cats.implicits.*
import fs2.Pipe
import fs2.Stream
import fs2.concurrent.Signal
import fs2.concurrent.SignallingRef
import lepus.client.Connection.Status
import lepus.client.apis.*
import lepus.protocol.*
import lepus.protocol.constants.ErrorCode
import lepus.protocol.constants.ErrorType
import lepus.protocol.domains.ChannelNumber

import internal.*

trait Connection[F[_]] {
  def channel: Resource[F, Channel[F, NormalMessagingChannel[F]]]
  def reliableChannel
      : Resource[F, Channel[F, ReliablePublishingMessagingChannel[F]]]
  def transactionalChannel
      : Resource[F, Channel[F, TransactionalMessagingChannel[F]]]

  def status: Signal[F, Connection.Status]
  def channels: Signal[F, Set[ChannelNumber]]
}

object Connection {
  def from[F[_]: Concurrent](
      transport: Transport[F],
      bufferSize: Int = 100
  ): Resource[F, Connection[F]] = for {
    sendQ <- Resource.eval(Queue.bounded[F, Frame](bufferSize))
    state: ConnectionLowLevel[F] = ???
    con = ConnectionImpl[F](state)
    _ <- run(transport, state, bufferSize).compile.drain.background
  } yield con

  enum Status {
    case New, Connecting, Connected, Closed
  }

  private[client] final class ConnectionImpl[F[_]](
      state: ConnectionLowLevel[F]
  )(using F: Concurrent[F])
      extends Connection[F] {

    // private def invalidState[T]: F[T] = F.raiseError(Exception())

    // private def expectOpen: F[State.Open[F]] =
    //   state.get.flatMap {
    //     case s: State.Open[F] => s.pure[F]
    //     case State.New(w) =>
    //       w.get.flatMap {
    //         case s: State.Open[F] => s.pure[F]
    //         case _                => invalidState
    //       }
    //     case _ => invalidState
    //   }

    // private def addChannel[MC <: MessagingChannel](
    //     f: ChannelTransmitter[F] => Resource[F, Channel[F, MC]]
    // ): Resource[F, Channel[F, MC]] =
    //   for {
    //     s <- Resource.eval(expectOpen)
    //     ch <- Resource.eval(s.mkCh)
    //     chNr <- s.handler.add(ch)
    //     out <- f(ch)
    //   } yield out

    def channel: Resource[F, Channel[F, NormalMessagingChannel[F]]] =
      state.addChannel(Channel.normal)

    def reliableChannel
        : Resource[F, Channel[F, ReliablePublishingMessagingChannel[F]]] =
      state.addChannel(Channel.reliable)

    def transactionalChannel
        : Resource[F, Channel[F, TransactionalMessagingChannel[F]]] =
      state.addChannel(Channel.transactional)

    def status: Signal[F, Connection.Status] = state.signal
    def channels: Signal[F, Set[ChannelNumber]] = state.channels
  }

  private[client] def run[F[_]: Concurrent](
      transport: Transport[F],
      state: ConnectionLowLevel[F],
      bufferSize: Int
  ): Stream[F, Nothing] =
    def handleError(f: F[Unit | ErrorCode]) = f.flatMap {
      case () => Concurrent[F].unit
      case e: ErrorCode =>
        if e.errorType == ErrorType.Connection then ???
        else ???
    }
    val mkQ = Queue.bounded[F, Frame](bufferSize)
    val process = for {
      sendQ <- Stream.eval(mkQ)
      negotiation <- Stream.eval(StartupNegotiation(???))
      handler <- Stream.eval(FrameDispatcher[F])
      frames = Stream
        .fromQueueUnterminated(sendQ, bufferSize)
        .through(transport)
        .through(negotiation.pipe)

      statusHandler = Stream.eval(negotiation.config).evalMap(state.onConnected)

      frameHandler = frames.foreach {
        case f: Frame.Body   => handleError(handler.body(f))
        case f: Frame.Header => handleError(handler.header(f))
        case m: Frame.Method if m.channel != ChannelNumber(0) =>
          handleError(handler.invoke(m))
        case m: Frame.Method => // Global methods like connection class
          ???
        case Frame.Heartbeat => sendQ.offer(Frame.Heartbeat)
      }

      a <- frameHandler.concurrently(statusHandler)
    } yield a

    process.onFinalize(state.onClosed) // TODO closing might need extra work

}
