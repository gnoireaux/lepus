package lepus.protocol.domains

import java.time.Instant

trait TaggedOpaqueComp[U, T <: U](using ev: U =:= T) {
  def apply(u: U): T = ev(u)
  def lift[F[_]](f: F[U]): F[T] = ev.liftCo(f)
}

opaque type ChannelNumber <: Short = Short
object ChannelNumber extends TaggedOpaqueComp[Short, ChannelNumber]

opaque type ShortString <: String = String
object ShortString {
  def empty: ShortString = ""
  def apply(str: String): Either[String, ShortString] =
    Either.cond(
      str.length <= 256,
      str,
      s"Maximum size for short strings is 256 characters! cannot create short string with length of ${str.length}"
    )
}

opaque type LongString <: String = String
private val LongStringSize: Long = BigDecimal(2).pow(32).toLongExact
object LongString {
  def empty: LongString = ""
  def apply(str: String): Either[String, LongString] =
    Either.cond(
      str.length <= LongStringSize,
      str,
      s"Maximum size for long strings is $LongStringSize characters! cannot create short string with length of ${str.length}"
    )
}

opaque type Timestamp <: Long = Long
object Timestamp {
  def apply(t: Long): Timestamp = t
  extension (t: Timestamp) {
    def toInstant: Instant = Instant.ofEpochMilli(t)
  }
}

final case class Decimal(scale: Byte, value: Int)

type FieldData =
  ShortString | LongString | Boolean | Byte | Short | Int | Long | Float |
    Double | Decimal | Timestamp | FieldTable

final case class FieldTable(values: Map[ShortString, FieldData])

opaque type ClassId <: Short = Short
object ClassId extends TaggedOpaqueComp[Short, ClassId]

/** Identifier for the consumer, valid within the current c hannel.
  */
opaque type ConsumerTag <: ShortString = ShortString
object ConsumerTag extends TaggedOpaqueComp[ShortString, ConsumerTag]

/** The server-assigned and channel-specific delivery tag
  */
opaque type DeliveryTag <: Long = Long
object DeliveryTag extends TaggedOpaqueComp[Long, DeliveryTag]

private val namePattern = "^[a-zA-Z0-9-_.:]*$".r
private def validateName(name: String): Either[String, String] =
  Either.cond(namePattern.matches(name), name, "Invalid name!")
private def validateNameSize(name: String): Either[String, String] =
  Either.cond(
    name.length <= 127,
    name,
    "Maximum name length is 127 characters!"
  )

/** The exchange name is a client-selected string that identifies the exchange
  * for publish methods.
  */
opaque type ExchangeName <: ShortString = String
object ExchangeName {
  def apply(name: String): Either[String, ExchangeName] =
    validateName(name).flatMap(validateNameSize)
}

opaque type MethodId <: Short = Short
object MethodId extends TaggedOpaqueComp[Short, MethodId]

/** If this field is set the server does not expect acknowledgements for
  * messages. That is, when a message is delivered to the client the server
  * assumes the delivery will succeed and immediately dequeues it. This
  * functionality may increase performance but at the cost of reliability.
  * Messages can get lost if a client dies before they are delivered to the
  * application.
  */
type NoAck = Boolean

/** If the no-local field is set the server will not send messages to the
  * connection that published them.
  */
type NoLocal = Boolean

/** If set, the server will not respond to the method. The client should not
  * wait for a reply method. If the server could not complete the method it will
  * raise a channel or connection exception.
  */
type NoWait = Boolean

/** Unconstrained.
  */
opaque type Path <: ShortString = String
object Path {
  def apply(str: String): Either[String, Path] =
    Either.cond(str.length <= 127, str, "Maximum length is 127 characters!")
}

/** This table provides a set of peer properties, used for identification,
  * debugging, and general information.
  */
type PeerProperties = FieldTable

/** The queue name identifies the queue within the vhost. In methods where the
  * queue name may be blank, and that has no specific significance, this refers
  * to the 'current' queue for the channel, meaning the last queue that the
  * client declared on the channel. If the client did not declare a queue, and
  * the method needs a queue name, this will result in a 502 (syntax error)
  * channel exception.
  */
opaque type QueueName <: ShortString = String
object QueueName {
  def apply(name: String): Either[String, QueueName] =
    validateName(name).flatMap(validateNameSize)
}

/** This indicates that the message has been previously delivered to this or
  * another client.
  */
type Redelivered = Boolean

/** The number of messages in the queue, which will be zero for newly-declared
  * queues. This is the number of messages present in the queue, and committed
  * if the channel on which they were published is transacted, that are not
  * waiting acknowledgement.
  */
type MessageCount = Int

/** The localised reply text. This text can be logged as an aid to resolving
  * issues.
  */
type ReplyText = ShortString

enum DeliveryMode(val value: Byte) {
  case NonPersistent extends DeliveryMode(1)
  case Persistent extends DeliveryMode(2)
}

type Priority = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
object Priority {
  def apply(b: Int): Either[String, Priority] = b match {
    case p: Priority => Right(p)
    case _           => Left("Valid priorities are 0-9")
  }
}
