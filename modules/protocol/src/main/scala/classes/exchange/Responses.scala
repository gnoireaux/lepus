/*
                           /!\ HUMANS BEWARE /!\
=================================================================================
||                                                                             ||
|| THIS FILE IS GENERATED BY MACHINES AND ANY EDITING BY HUMANS IS PROHIBITED! ||
||                                                                             ||
=================================================================================
 */

package lepus.protocol.classes.exchange

import lepus.protocol.Method
import lepus.protocol.domains.*
import lepus.protocol.constants.*

enum Responses(classId: ClassId, methodId: MethodId)
    extends Method(classId, methodId) {

  case DeclareOk extends Responses(ClassId(40), MethodId(11))

  case DeleteOk extends Responses(ClassId(40), MethodId(21))

}
