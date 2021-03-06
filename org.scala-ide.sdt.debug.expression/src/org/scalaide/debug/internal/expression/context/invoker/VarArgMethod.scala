/*
 * Copyright (c) 2015 Contributor. All rights reserved.
 */
package org.scalaide.debug.internal.expression
package context.invoker

import org.scalaide.debug.internal.expression.context.JdiContext
import org.scalaide.debug.internal.expression.proxies.JdiProxy
import org.scalaide.debug.internal.expression.proxies.primitives.PrimitiveJdiProxy

import com.sun.jdi.ObjectReference
import com.sun.jdi.Value

/**
 * Basic support for invoking vararg methods.
 * Provides a `packToVarArg` which converts sequence of JdiProxies to ObjectReference of a Seq.
 */
trait VarArgSupport {
  self: BaseMethodInvoker =>

  protected def context: JdiContext

  private def seqObjectRef: ObjectReference = context.objectByName(Names.Scala.seq)

  private def emptyListRef: ObjectReference = {
    def emptyMethod = context.methodOn(seqObjectRef, "empty", arity = 0)
    seqObjectRef.invokeMethod(context.currentThread(), emptyMethod, List[Value]()).asInstanceOf[ObjectReference]
  }

  protected def packToVarArg(proxies: Seq[JdiProxy]): ObjectReference = {
    def addMethod(that: ObjectReference) = context.methodOn(that.referenceType.name, "+:", arity = 2)
    def canBuildFromMethod = context.methodOn(seqObjectRef, "canBuildFrom", arity = 0)
    def canBuildFrom = seqObjectRef.invokeMethod(context.currentThread(), canBuildFromMethod, List[Value]())

    proxies.foldRight(emptyListRef) {
      (proxy, current) =>
        // always box arguments to pack in sequence
        val value = proxy match {
          case primitive: PrimitiveJdiProxy[_, _, _] => primitive.boxed
          case other => other.__value
        }
        val args = List(value, canBuildFrom)
        current.invokeMethod(context.currentThread(), addMethod(current), args).asInstanceOf[ObjectReference]
    }
  }
}
