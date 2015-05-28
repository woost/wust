package common

import java.nio.ByteBuffer
import org.apache.commons.codec.binary.Base64

object Helpers {

  def compose[A, B, C](f: PartialFunction[A, B], g: PartialFunction[B, C]): PartialFunction[A, C] = Function.unlift(f.lift(_).flatMap(g.lift))

  def uuidBase64 = {
    val uuid = java.util.UUID.randomUUID
    val bb = ByteBuffer.wrap(new Array[Byte](16))
    bb.putLong(uuid.getMostSignificantBits)
    bb.putLong(uuid.getLeastSignificantBits)
    // url safe replaces + and / by - and _
    Base64.encodeBase64URLSafeString(bb.array())
  }
}
