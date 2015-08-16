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

  @JSExport def isInside(r:Rect) = x > r.pos.x && y > r.pos.y && x < r.otherPos.x && y < r.otherPos.y
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

  @JSExport def isInside(r:Rect) = (start isInside r) && (end isInside r)

  def intersect(that:Line):Option[Algorithms.LineIntersection] = Algorithms.intersect(this, that)
  def intersect(r:Rect):Either[Boolean,Seq[Vec2]] = Algorithms.intersect(r, this)
  def cutBy(r:Rect):Option[Line] = Algorithms.cutLineByRectAtStartOrEnd(this, r)
  @JSExport("cutBy") def cutBy_js(r:Rect):js.UndefOr[Line] = cutBy(r).orUndefined
  def clampBy(r:Rect):Option[Line] = Algorithms.clampLineByRect(this, r)
  @JSExport("clampBy") def clampBy_js(r:Rect):js.UndefOr[Line] = clampBy(r).orUndefined

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

@JSExport
object Rect {
}

@JSExport
case class Rect(
  @(JSExport @field) pos:Vec2,
  @(JSExport @field) size:Vec2) {
  @JSExport def x = pos.x
  @JSExport def y = pos.y
  @JSExport def width = size.x
  @JSExport def height = size.y

  @JSExport lazy val otherPos = pos + size
  @JSExport lazy val center = pos + size / 2

  @JSExport def centered = Rect(center - size, size)

  lazy val corners = Array(
    pos,
    Vec2(pos.x + size.x, pos.y),
    otherPos,
    Vec2(pos.x, pos.y + size.y)
  )

  lazy val edges = Array(
    Line(corners(0), corners(1)),
    Line(corners(1), corners(2)),
    Line(corners(2), corners(3)),
    Line(corners(3), corners(0))
  )

  def intersect(that:Line) = Algorithms.intersect(this, that)

  def isOverlapping(that:Rect) = {
    ((this.x < that.x + that.width) && (this.x + this.width > that.x)) &&
    ((this.y < that.y + that.width) && (this.y + this.width > that.y))
  }
}

object Algorithms {
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

  def intersect(rect:Rect, line:Line):Either[Boolean,Seq[Vec2]] = {
    // Left(true) => line is completely inside
    // Left(fals) => line is completely outside
    // Right(pos) => one intersection point
    val intersections = rect.edges.flatMap { edge =>
      (line intersect edge).filter( i => i.onLine1 && i.onLine2 ).map(_.pos)
    }

    if( intersections.nonEmpty )
      Right(intersections)
    else Left(line isInside rect)
  }

  def cutLineByRectAtStartOrEnd(line:Line, rect:Rect):Option[Line] = {
    // Assuming there is only one intersection.
    // Which means that one line end is inside the rect,
    // the other one outside.
    // If there are two intersections the resulting line
    // can be wrong
    intersect(rect, line) match {
      case Left(true) => None // line inside
      case Left(false) => Some(line) // line outside
      case Right(intersections) =>
        // with the assumption that the rect covers one line end,
        // we have exactly one intersection
        if(line.end isInside rect)
          Some(Line(line.start, intersections.head))
        else
          Some(Line(intersections.head, line.end))
    }
  }

  def clampLineByRect(line:Line, rect:Rect):Option[Line] = {
    (line.start isInside rect, line.end isInside rect) match {
      case (true, true) => Some(line)
      case (true, false) => Some(Line(line.start, intersect(rect, line).right.get.head))
      case (false, true) => Some(Line(intersect(rect, line).right.get.head, line.end))
      case (false, false) =>
        intersect(rect, line) match {
          case Left(_) => None
          case Right(intersections) =>
            // rectangle is convex, line endpoints lie outside,
            // so we have exactly two intersections
            Some(Line(intersections(0), intersections(1)))
        }
    }

  }
}
