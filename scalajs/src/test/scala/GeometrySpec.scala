package geometry

import utest._

import scala.scalajs.js

object GeometrySpec extends TestSuite {
  // console.warn polyfill
  if(js.isUndefined(js.Dynamic.global.console.warn))
    js.Dynamic.global.console.updateDynamic("warn")((m: String) => println(m))

  val tests = TestSuite {
    'Primitives {
      'Vec2 {
        'constructor {
          val v = Vec2(5,7)
          assert(v.x == 5)
          assert(v.y == 7)
          assert(v.width == 5)
          assert(v.height == 7)
        }
        'addition {
          val a = Vec2(5,7)
          val b = Vec2(2,3)
          val c = a + b
          assert(c.x == 7)
          assert(c.y == 10)
        }
        'substraction {
          val a = Vec2(5,7)
          val b = Vec2(2,3)
          val c = a - b
          assert(c.x == 3)
          assert(c.y == 4)
        }
        'multiplication {
          val a = Vec2(5,7)
          val c = a * 3
          assert(c.x == 15)
          assert(c.y == 21)
        }
        'division {
          val a = Vec2(6,8)
          val c = a / 2
          assert(c.x == 3)
          assert(c.y == 4)
        }
        'crossProduct {
          val a = Vec2(6,8)
          val b = Vec2(2,3)
          assert((a cross b) == 2)
        }
      }

      'Line {
        'constructor {
          val l = Line(Vec2(7,3), Vec2(8,4))
          assert(l.start == Vec2(7,3))
          assert(l.end == Vec2(8,4))
          assert(l.x1 == 7)
          assert(l.y1 == 3)
          assert(l.x2 == 8)
          assert(l.y2 == 4)
        }

        'vector {
          val l = Line(Vec2(7,3), Vec2(8,5))
          assert(l.vector == Vec2(1, 2))
        }

        'center {
          val l = Line(Vec2(7,3), Vec2(8,5))
          assert(l.center == Vec2(7.5, 4))
        }

        'angle {
          val l = Line(Vec2(7,3), Vec2(12,3))
          assert(l.vector.angle == 0)
          val m = Line(Vec2(7,3), Vec2(6,3))
          assert(m.vector.angle == Math.PI)
          val n = Line(Vec2(7,3), Vec2(7,5))
          assert(n.vector.angle == Math.PI/2)
          val o = Line(Vec2(7,3), Vec2(7,1))
          assert(o.vector.angle == -Math.PI/2)
        }

        'rightOf {
          val l = Line(Vec2(4,12), Vec2(8,0))
          val p = Vec2(12,16)
          assert( !(l rightOf p) )
          assert( l leftOf p )
        }

        'equals {
          val l = Line(Vec2(7,3), Vec2(8,4))
          val m = Line(Vec2(8,4), Vec2(7,3))
          assert(l == l)
          assert(l == m)
        }

        'hashCode {
          val l = Line(Vec2(7,3), Vec2(8,4))
          val m = Line(Vec2(8,4), Vec2(7,3))
          assert(l.hashCode == m.hashCode)
        }

        'length {
          val l = Line(Vec2(5, 5), Vec2(8, 9))
          assert(l.length == 5)
        }
      }

      'AARect {
        'constructor {
          val r = AARect(Vec2(11,5), Vec2(8,4))
          assert(r.pos == Vec2(11,5))
          assert(r.size == Vec2(8,4))
          assert(r.x == 11)
          assert(r.y == 5)
          assert(r.width == 8)
          assert(r.height == 4)
        }

        'minMaxCorner {
          val r = AARect(Vec2(11,5), Vec2(8,4))
          assert(r.minCorner == Vec2(7,3))
          assert(r.maxCorner == Vec2(15,7))
        }

        'corners {
          val r = AARect(Vec2(3,3.5), Vec2(2,1))
          val c = r.corners.toList
          assert(c.toList == List(Vec2(2,3), Vec2(4,3), Vec2(4,4), Vec2(2,4)))
        }

        'edges {
          val r = AARect(Vec2(3,3.5), Vec2(2,1))
          val e = r.edges.toList
          assert(e == List(
            Line(Vec2(2,3), Vec2(4,3)),
            Line(Vec2(4,3), Vec2(4,4)),
            Line(Vec2(4,4), Vec2(2,4)),
            Line(Vec2(2,4), Vec2(2,3)))
          )
        }

        'PointInside {
          val r = AARect(Vec2(3,3.5), Vec2(2,1))
          assert((r includes Vec2(3, 3.5)) == true)
          assert((r includes Vec2(3, 4.5)) == false)
        }

        'LineInside {
          val r = AARect(Vec2(3,3.5), Vec2(2,1))
          assert(( r includes Line(Vec2(2.5,3.5), Vec2(3.5,3.5))) == true)
          assert(( r includes Line(Vec2(2.5,3.5), Vec2(5.5,3.5))) == false)
          assert(( r includes Line(Vec2(4.5,3.5), Vec2(5.5,3.5))) == false)
        }
        'OverlappingRect {
          val r1 = AARect(Vec2(2,3), Vec2(4,4))
          val r2 = AARect(Vec2(1,4), Vec2(3,1))
          val r3 = AARect(Vec2(10,10), Vec2(1,1))
          assert(r1 isOverlapping r2)
          assert(r2 isOverlapping r1)
          assert(!(r1 isOverlapping r3))
          assert(!(r3 isOverlapping r1))
        }
      }

      'RotatedRect {
        'constructor {
          val r = RotatedRect(Vec2(11,5), Vec2(8,4), Math.PI/4)
          assert(r.pos == Vec2(11,5))
          assert(r.size == Vec2(8,4))
          assert(r.x == 11)
          assert(r.y == 5)
          assert(r.width == 8)
          assert(r.height == 4)
        }

        'minMaxCorner {
          val r = RotatedRect(Vec2(8,9.5), Vec2(20,5), Math.atan(4.0/3.0))
          val roundedMin = Vec2(Math.round(r.minCorner.x), Math.round(r.minCorner.y))
          val roundedMax = Vec2(Math.round(r.maxCorner.x), Math.round(r.maxCorner.y))
          assert(roundedMin == Vec2(4,0))
          assert(roundedMax == Vec2(12,19))
        }

        'corners {
          val r = RotatedRect(Vec2(8,9.5), Vec2(20,5), Math.atan(4.0/3.0))
          val c = r.corners.toList
          val rounded = c.toList.map(v => Vec2(Math.round(v.x), Math.round(v.y)))
          assert(rounded == List(Vec2(4,0), Vec2(0,3), Vec2(12,19), Vec2(16,16)))
        }

        'edges {
          val r = RotatedRect(Vec2(8,9.5), Vec2(20,5), Math.atan(4.0/3.0))
          val e = r.edges.toList
          val rounded = e.toList.map(l => Line(
            Vec2(Math.round(l.start.x), Math.round(l.start.y)),
            Vec2(Math.round(l.end.x), Math.round(l.end.y))))
          assert(rounded == List(
            Line(Vec2(4,0), Vec2(0,3)),
            Line(Vec2(0,3), Vec2(12,19)),
            Line(Vec2(12,19), Vec2(16,16)),
            Line(Vec2(16,16), Vec2(4,0))
          ))
        }
        'PointInside {
          val r = RotatedRect(Vec2(8,9.5), Vec2(20,5), Math.atan(4.0/3.0))
          assert((r includes Vec2(4, 4)) == true)
          assert((r includes Vec2(8, 12)) == true)
          assert((r includes Vec2(12, 8)) == false)
          assert((r includes Vec2(12, 10)) == false)
        }

        'LineInside {
          val r = RotatedRect(Vec2(8,9.5), Vec2(20,5), Math.atan(4.0/3.0))
          assert(( r includes Line(Vec2(12,16), Vec2(8,8))) == true)
          assert(( r includes Line(Vec2(8,16), Vec2(12,16))) == false)
          assert(( r includes Line(Vec2(12,10), Vec2(13,10))) == false)
        }
      }
    }
    'Algorithms {
      'LineIntersection {
        'Segments {
          val a = Line(Vec2(1,3), Vec2(4,1))
          val b = Line(Vec2(2,1), Vec2(3,3))
          val i = (a intersect b).get
          assert( i.pos == Vec2(2.5, 2) )
          assert( i.onLine1 == true )
          assert( i.onLine2 == true )
        }
        'SegmentAndLine {
          val a = Line(Vec2(1,3), Vec2(4,1))
          val b = Line(Vec2(0,0), Vec2(2,2))
          val i = (a intersect b).get
          assert( i.onLine1 == true )
          assert( i.onLine2 == false )
        }
        'LineAndSegment {
          val a = Line(Vec2(1,3), Vec2(4,1))
          val b = Line(Vec2(0,3), Vec2(1,4))
          val i = (a intersect b).get
          assert( i.onLine1 == false )
          assert( i.onLine2 == true )
        }
        'Parallel {
          val a = Line(Vec2(1,3), Vec2(4,1))
          val b = Line(Vec2(2,3), Vec2(5,1))
          val i = (a intersect b)
          assert( i == None )
        }
      }
      'FirstLineRectIntersection {
        'Intersect {
          val r = Rect(Vec2(3,3.5), Vec2(2,1))
          val l = Line(Vec2(3,2), Vec2(3,3.5))
          val i = (r intersect l).right.get
          val i2 = (l intersect r).right.get
          assert( i == Seq(Vec2(3, 3)) )
          assert( i2 == Seq(Vec2(3, 3)) )
        }
        'NoIntersectInside {
          val r = Rect(Vec2(3,3.5), Vec2(2,1))
          val l = Line(Vec2(2.5,3.5), Vec2(3.5,3.5))
          val i = (r intersect l).left.get
          assert( i == true )
        }
        'NoIntersectOutside {
          val r = Rect(Vec2(3,3.5), Vec2(2,1))
          val l = Line(Vec2(2.5,2.5), Vec2(2.5,2.5))
          val i = (r intersect l).left.get
          assert( i == false )
        }
      }

      'CutLineByRect {
        // assuming there is only one intersection
        // which means that one line end is inside the rect
        // if there are two intersections the resulting line
        // can be wrong
        'Intersect {
          val r = Rect(Vec2(3,3.5), Vec2(2,1))
          val l = Line(Vec2(3,2), Vec2(3,3.5))
          val c = (l cutBy r).get
          assert( c == Line(Vec2(3,2), Vec2(3,3)) )
        }
        'NoCut {
          val r = Rect(Vec2(3,3.5), Vec2(2,1))
          val l = Line(Vec2(3,2), Vec2(4,2))
          val c = (l cutBy r).get
          assert( c == l )
        }
        'FullCut {
          val r = Rect(Vec2(3,3.5), Vec2(2,1))
          val l = Line(Vec2(2.5,3.5), Vec2(3.5,3.5))
          val c = (l cutBy r)
          assert( c == None )
        }
      }

      'ClampLineByRect {
        'Inside {
          val r = Rect(Vec2(4,5), Vec2(4,4))
          val l = Line(Vec2(3,4), Vec2(4,5))
          val c = (l clampBy r).get
          assert( c == l )
        }

        'StartOutside {
          val r = Rect(Vec2(4,5), Vec2(4,4))
          val l = Line(Vec2(1,4), Vec2(4,4))
          val c = (l clampBy r).get
          assert( c == Line(Vec2(2,4), Vec2(4,4)) )
        }

        'StartInside {
          val r = Rect(Vec2(4,5), Vec2(4,4))
          val l = Line(Vec2(4,4), Vec2(1,4))
          val c = (l clampBy r).get
          assert( c == Line(Vec2(4,4), Vec2(2,4)) )
        }

        'Outside {
          val r = Rect(Vec2(4,5), Vec2(4,4))
          val l = Line(Vec2(1,2), Vec2(-1,4))
          val c = (l clampBy r)
          assert( c == None )
        }

        'GoingThrough {
          val r = Rect(Vec2(4,5), Vec2(4,4))
          val l = Line(Vec2(1,4), Vec2(7,4))
          val c = (l clampBy r).get
          assert( c == Line(Vec2(2,4), Vec2(6,4)) )
        }
      }
    }
  }
}

