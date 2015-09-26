package model

import java.nio.ByteBuffer
import org.apache.commons.codec.binary.Base64

object Helpers {

  implicit class BooleanWithImplies(bool:Boolean) {
    final def implies(that:Boolean) = !this.bool || that
  }

  def uuidBase64 = {
    val uuid = java.util.UUID.randomUUID
    val bb = ByteBuffer.wrap(new Array[Byte](16))
    bb.putLong(uuid.getMostSignificantBits)
    bb.putLong(uuid.getLeastSignificantBits)
    // url safe replaces + and / by - and _
    Base64.encodeBase64URLSafeString(bb.array())
  }

  def hashColor(obj:Any) = obj.hashCode % 360
  def tagTitleColor(title:String) = hashColor(title.toLowerCase)
}
