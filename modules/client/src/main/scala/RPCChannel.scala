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
package internal

import cats.effect.Concurrent
import cats.effect.kernel.Deferred
import cats.effect.std.Queue
import cats.effect.std.QueueSink
import cats.effect.std.QueueSource
import cats.effect.std.Semaphore
import cats.implicits.*
import fs2.Stream
import lepus.protocol.ConnectionClass.Start
import lepus.protocol.Frame
import lepus.protocol.*
import lepus.protocol.constants.ErrorCode
import lepus.protocol.constants.ReplyCode
import lepus.protocol.domains.*
import scodec.bits.ByteVector

private[client] trait RPCChannel[F[_]] {
  def sendWait(m: Method): F[Method]
  def sendNoWait(m: Method): F[Unit]
  def recv(m: Method): F[Unit | ErrorCode]
}

private[client] object RPCChannel {
  def apply[F[_]](
      publisher: SequentialOutput[F, Frame],
      channelNumber: ChannelNumber,
      maxMethods: Int = 10
  )(using F: Concurrent[F]): F[RPCChannel[F]] =
    for {
      waitlist <- Waitlist[F, Method](maxMethods)
    } yield new {

      def sendWait(m: Method): F[Method] = for {
        d <- waitlist.checkinAnd(sendNoWait(m))
        out <- d.get
      } yield out

      def sendNoWait(m: Method): F[Unit] =
        publisher.writeOne(Frame.Method(channelNumber, m))

      def recv(m: Method): F[Unit | ErrorCode] =
        waitlist.nextTurn(m).ifM(unit, syntaxError)

      private val syntaxError: F[Unit | ErrorCode] = ReplyCode.SyntaxError.pure
      private val unit: F[Unit | ErrorCode] = ().pure
    }
}
