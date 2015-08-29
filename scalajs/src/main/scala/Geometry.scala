package geometry

case class Vec2(x:Double, y:Double) {
  def +(that:Vec2) = Vec2(this.x + that.x, this.y + that.y)
  def *(a:Double) = Vec2(this.x * a, this.y * a)
}

case class Line(start:Vec2, end:Vec2) {
  def intersect(that:Line) = Algorithms.intersection(this, that)
  def intersectFirst(that:Rect) = Algorithms.firstIntersection(that, this)
}

case class Rect(pos:Vec2, dim:Vec2) {
  lazy val otherPos = pos + dim

  lazy val corners = Array(
    pos,
    Vec2(pos.x + dim.x, pos.y),
    Vec2(pos.x, pos.y + dim.y),
    otherPos
  )

  lazy val edges = Array(
      Line(corners(0), corners(1)),
      Line(corners(1), corners(2)),
      Line(corners(2), corners(3)),
      Line(corners(3), corners(0))
    )

  def inside(p:Vec2):Boolean = p.x > pos.x && p.y > pos.y && p.x < otherPos.x && p.y < otherPos.y
  def inside(l:Line):Boolean = inside(l.start) && inside(l.end)

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
    for(edge <- rect.edges)
      intersection(line, edge).foreach{ i => if(i.onLine1 && i.onLine2) return Right(i.pos) }
    return Left(rect.inside(line))
  }

// function clampLineByRects(edge, sourceRect, targetRect) {
//     let linkLine = {
//         start: {
//             x: edge.source.x,
//             y: edge.source.y
//         },
//         end: {
//             x: edge.target.x,
//             y: edge.target.y
//         }
//     };

//     let sourceIntersection = lineRectIntersection(linkLine, {
//         width: sourceRect.width,
//         height: sourceRect.height,
//         x: edge.source.x - sourceRect.width / 2,
//         y: edge.source.y - sourceRect.height / 2
//     });

//     let targetIntersection = lineRectIntersection(linkLine, {
//         width: targetRect.width,
//         height: targetRect.height,
//         x: edge.target.x - targetRect.width / 2,
//         y: edge.target.y - targetRect.height / 2
//     });

//     return {
//         x1: sourceIntersection === undefined ? edge.source.x : sourceIntersection
//             .x,
//         y1: sourceIntersection === undefined ? edge.source.y : sourceIntersection
//             .y,
//         x2: targetIntersection === undefined ? edge.target.x : targetIntersection
//             .x,
//         y2: targetIntersection === undefined ? edge.target.y : targetIntersection
//             .y
//     };
// }
}
