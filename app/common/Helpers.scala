package common

object Helpers {
  implicit class BooleanToOption(val b: Boolean) extends AnyVal {
    final def option[A](a: => A): Option[A] = if (b) Some(a) else None
  }

  implicit class BooleanToEither(val b: Boolean) extends AnyVal {
    final def either[L,R](l: => L, r: => R): Either[L,R] = if (b) Left(l) else Right(r)
  }

  def compose[A, B, C](f: PartialFunction[A, B], g: PartialFunction[B, C]): PartialFunction[A, C] = Function.unlift(f.lift(_).flatMap(g.lift))
}
