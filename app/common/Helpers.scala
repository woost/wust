package common

object Helpers {

  def compose[A,B,C](f: PartialFunction[A, B], g: PartialFunction[B, C]) : PartialFunction[A, C] = Function.unlift(f.lift(_).flatMap(g.lift))

}
