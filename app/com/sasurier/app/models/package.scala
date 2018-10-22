package com.sasurier.app

import scala.reflect.ClassTag

package object common {

  case class Id[T: ClassTag](id: Long) {
    override def toString = id.toString
    def `type` = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]].getName
  }

  object Id {
    def uninitialized[T: ClassTag] = Id[T](-1)
  }

  case class UID[T: ClassTag](value: String) {
    def `type` = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]].getName
  }

}
