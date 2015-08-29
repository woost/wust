package geometry

case class Vec2(x:Double, y:Double) {
  def +(that:Vec2) = Vec2(this.x + that.x, this.y + that.y)
  def *(a:Double) = Vec2(this.x * a, this.y * a)

  def isInside(r:Rect) = x > r.pos.x && y > r.pos.y && x < r.otherPos.x && y < r.otherPos.y
}

case class Line(start:Vec2, end:Vec2) {
  def isInside(r:Rect) = (start isInside r) && (end isInside r)

  def intersect(that:Line) = Algorithms.intersection(this, that)
  def intersectFirst(r:Rect) = Algorithms.firstIntersection(r, this)
  def clampBy(r:Rect) = Algorithms.clampLineByRect(this, r)
}

case class Rect(pos:Vec2, dim:Vec2) {
  lazy val otherPos = pos + dim

  lazy val corners = Array(
    pos,
    Vec2(pos.x + dim.x, pos.y),
    otherPos,
    Vec2(pos.x, pos.y + dim.y)
  )

  lazy val edges = Array(
    Line(corners(0), corners(1)),
    Line(corners(1), corners(2)),
    Line(corners(2), corners(3)),
    Line(corners(3), corners(0))
  )

  def intersectFirst(that:Line) = Algorithms.firstIntersection(this, that)
}

object Algorithms {
  case class LineIntersection(pos:Vec2, onLine1:Boolean, onLine2:Boolean)
  def intersection(line1: Line, line2: Line):Option[LineIntersection] = {
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

  def firstIntersection(rect:Rect, line:Line):Either[Boolean,Vec2] = {
    // Left(true) => line is completely inside
    // Left(fals) => line is completely outside
    // Right(pos) => one intersection point
    for(edge <- rect.edges) {
      println(edge, intersection(line, edge))
      (line intersect edge).foreach{ i => if(i.onLine1 && i.onLine2) return Right(i.pos) }
    }

    return Left(line isInside rect)
  }

  def clampLineByRect(line:Line, rect:Rect):Option[Line] = {
    // Assuming there is only one intersection.
    // Which means that one line end is inside the rect,
    // the other one outside.
    // If there are two intersections the resulting line
    // can be wrong
    firstIntersection(rect, line) match {
      case Left(true) => None
      case Left(false) => Some(line)
      case Right(intersection) =>
        if(line.end isInside rect) Some(Line(line.start, intersection))
        else Some(Line(intersection, line.end))
    }
  }
}
