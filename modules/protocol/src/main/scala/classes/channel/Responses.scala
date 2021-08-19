/*
                           /!\ HUMANS BEWARE /!\
=================================================================================
||                                                                             ||
|| THIS FILE IS GENERATED BY MACHINES AND ANY EDITING BY HUMANS IS PROHIBITED! ||
||                                                                             ||
=================================================================================
 */

package lepus.protocol.classes.channel

import lepus.protocol.Method
import lepus.protocol.domains.*
import lepus.protocol.constants.*

enum Responses(classId: ClassId, methodId: MethodId)
    extends Method(classId, methodId) {

  case OpenOk() extends Responses(ClassId(20), MethodId(11))

  case Flow(active: Boolean) extends Responses(ClassId(20), MethodId(20))

  case FlowOk(active: Boolean) extends Responses(ClassId(20), MethodId(21))

  case Close(
      replyCode: ReplyCode,
      replyText: ReplyText,
      classId: ClassId,
      methodId: MethodId
  ) extends Responses(ClassId(20), MethodId(40))

  case CloseOk extends Responses(ClassId(20), MethodId(41))

}
