package geometry

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportAll}
import scala.annotation.meta.field
import js.JSConverters._

@JSExport
case class Vec2(
  @(JSExport @field) x:Double,
  @(JSExport @field) y:Double) {
  @JSExport def width = x
  @JSExport def height = y
  @JSExport("plus") def +(that:Vec2) = Vec2(this.x + that.x, this.y + that.y)
  @JSExport("minus") def -(that:Vec2) = Vec2(this.x - that.x, this.y - that.y)
  @JSExport("times") def *(a:Double) = Vec2(this.x * a, this.y * a)
  @JSExport("div") def /(a:Double) = Vec2(this.x / a, this.y / a)
  @JSExport def cross(that:Vec2) = this.x * that.y - this.y * that.x

  @JSExport def angle = Math.atan2(y, x);
}

@JSExport
case class Line(
  @(JSExport @field) start:Vec2,
  @(JSExport @field) end:Vec2) {
  @JSExport def x1 = start.x
  @JSExport def y1 = start.y
  @JSExport def x2 = end.x
  @JSExport def y2 = end.y

  @JSExport def vector = end - start
  @JSExport def center = (start + end) / 2

  @JSExport def leftOf(p:Vec2) = (vector cross (p - start)) > 0
  @JSExport def rightOf(p:Vec2) = !leftOf(p)

  def intersect(that:Line):Option[Algorithms.LineIntersection] = Algorithms.intersect(this, that)
  def intersect(r:ConvexPolygon):Either[Boolean,Seq[Vec2]] = Algorithms.intersect(r, this)
  def cutBy(r:ConvexPolygon):Option[Line] = Algorithms.cutLineByPolyAtStartOrEnd(this, r)
  @JSExport("cutBy") def cutBy_js(r:ConvexPolygon):js.UndefOr[Line] = cutBy(r).orUndefined
  def clampBy(r:ConvexPolygon):Option[Line] = Algorithms.clampLineByPoly(this, r)
  @JSExport("clampBy") def clampBy_js(r:ConvexPolygon):js.UndefOr[Line] = clampBy(r).orUndefined

  @JSExport def length = {
    val dx = start.x - end.x
    val dy = start.y - end.y
    Math.sqrt(dx * dx + dy * dy)
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[Line]

  override def equals(other: Any): Boolean = other match {
    case that: Line => (that canEqual this) &&
      (this.start == that.start && this.end == that.end) ||
      (this.start == that.end && this.end == that.start)
    case _             => false
  }

  override def hashCode = start.hashCode * end.hashCode // multiply to be commutative
}

trait ConvexPolygon {
  def corners:IndexedSeq[Vec2] // in counter clockwise order
  lazy val edges:IndexedSeq[Line] = Algorithms.slidingRotate(corners).map( e => Line(e.head,e.last))

  def intersect(line:Line) = Algorithms.intersect(this, line)

  @JSExport def includes(v:Vec2):Boolean = edges.forall(_ rightOf v)
  @JSExport def includes(l:Line):Boolean = includes(l.start) && includes(l.end)
  @JSExport def isOverlapping(that:ConvexPolygon):Boolean
}

trait Rect extends ConvexPolygon {
  // center
  @JSExport def pos:Vec2
  @JSExport def x = pos.x
  @JSExport def y = pos.y

  @JSExport def size:Vec2
  @JSExport def width = size.x
  @JSExport def height = size.y

  @JSExport def angle:Double

  @JSExport def minCorner:Vec2
  @JSExport def maxCorner:Vec2
}

object Rect {
  def apply(pos:Vec2, size:Vec2, angle:Double = 0):Rect = if(angle == 0) AARect(pos, size) else RotatedRect(pos, size, angle)
}

@JSExport
case class RotatedRect(pos:Vec2, size:Vec2, angle:Double) extends Rect {
  import Math.{sin,cos}

  lazy val toRight = Vec2(cos(angle),sin(angle))*(width/2)
  lazy val toBottom = Vec2(-sin(angle),cos(angle))*(height/2)

  lazy val minCorner = pos - toRight - toBottom
  lazy val maxCorner = pos + toRight + toBottom

  lazy val corners = Vector(
    minCorner,
    pos - toRight + toBottom,
    maxCorner,
    pos + toRight - toBottom
  )

  def isOverlapping(that:ConvexPolygon):Boolean = ???
}

@JSExport
case class AARect(pos:Vec2, size:Vec2) extends Rect {
  override def angle = 0

  lazy val minCorner = pos - size / 2
  lazy val maxCorner = pos + size / 2

  override def includes(v:Vec2):Boolean = v.x > minCorner.x && v.y > minCorner.y && v.x < maxCorner.x && v.y < maxCorner.y

  lazy val corners = Vector(
    minCorner,
    minCorner + Vec2(size.x, 0),
    maxCorner,
    minCorner + Vec2(0, size.y)
  )

  def isOverlapping(that:ConvexPolygon):Boolean = that match {
    case that:AARect =>
      ((this.x < that.x + that.width) && (this.x + this.width > that.x)) &&
      ((this.y < that.y + that.width) && (this.y + this.width > that.y))
    case poly => ???
  }
}

object Algorithms {
  def slidingRotate[T](l:Seq[T]):IndexedSeq[Seq[T]] = (l :+ l.head).sliding(2).toIndexedSeq

  case class LineIntersection(pos:Vec2, onLine1:Boolean, onLine2:Boolean)
  def intersect(line1: Line, line2: Line):Option[LineIntersection] = {
    // if the lines intersect, the result contains the x and y of the intersection
    // (treating the lines as infinite) and booleans for
    // whether line segment 1 or line segment 2 contain the point
    // from: http://jsfiddle.net/justin_c_rounds/Gd2S2
    // ported to scala

    val line1Dx = line1.end.x - line1.start.x
    val line1Dy = line1.end.y - line1.start.y
    val line2Dx = line2.end.x - line2.start.x
    val line2Dy = line2.end.y - line2.start.y

    val denominator = (line2Dy * line1Dx) - (line2Dx * line1Dy)

    if (denominator == 0) return None

    val startDx = line1.start.x - line2.start.x
    val startDy = line1.start.y - line2.start.y

    val numerator1 = (line2Dx * startDy) - (line2Dy * startDx)
    val numerator2 = (line1Dx * startDy) - (line1Dy * startDx)
    val a = numerator1 / denominator
    val b = numerator2 / denominator

    // if we cast these lines infinitely in both directions, they intersect here:
    val resultX = line1.start.x + (a * (line1Dx))
    val resultY = line1.start.y + (a * (line1Dy))
    /*
    // it is worth noting that this should be the same as:
    x = line2StartX + (b * (line2EndX - line2StartX))
    y = line2StartX + (b * (line2EndY - line2StartY))
    */
   // if line1 is a segment and line2 is infinite, they intersect if:
   val resultOnLine1 = a > 0 && a < 1
   // if line2 is a segment and line1 is infinite, they intersect if:
   val resultOnLine2 = b > 0 && b < 1
   // if line1 and line2 are segments, they intersect if both of the above are true
   return Some(LineIntersection(Vec2(resultX, resultY), resultOnLine1, resultOnLine2))
  }

  def intersect(poly:ConvexPolygon, line:Line):Either[Boolean,Seq[Vec2]] = {
    // Left(true) => line is completely inside
    // Left(fals) => line is completely outside
    // Right(pos) => one intersection point
    val intersections = poly.edges.flatMap { edge =>
      (line intersect edge).filter( i => i.onLine1 && i.onLine2 ).map(_.pos)
    }

    if( intersections.nonEmpty )
      Right(intersections)
    else Left(poly includes line)
  }

  def cutLineByPolyAtStartOrEnd(line:Line, poly:ConvexPolygon):Option[Line] = {
    // Assuming there is only one intersection.
    // Which means that one line end is inside the poly,
    // the other one outside.
    // If there are two intersections the resulting line
    // can be wrong
    intersect(poly, line) match {
      case Left(true) => None // line inside
      case Left(false) => Some(line) // line outside
      case Right(intersections) =>
        // with the assumption that the poly covers one line end,
        // we have exactly one intersection
        if(poly includes line.end)
          Some(Line(line.start, intersections.head))
        else
          Some(Line(intersections.head, line.end))
    }
  }

  def clampLineByPoly(line:Line, poly:ConvexPolygon):Option[Line] = {
    ( poly includes line.start, poly includes line.end) match {
      case (true, true) => Some(line)
      case (true, false) => Some(Line(line.start, intersect(poly, line).right.get.head))
      case (false, true) => Some(Line(intersect(poly, line).right.get.head, line.end))
      case (false, false) =>
        intersect(poly, line) match {
          case Left(_) => None
          case Right(intersections) =>
            // polygon is convex, line endpoints lie outside,
            // so we have exactly two intersections
            Some(Line(intersections(0), intersections(1)))
        }
    }

  }
}
