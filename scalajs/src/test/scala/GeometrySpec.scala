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

      'Rect {
        'constructor {
          val r = Rect(Vec2(7,3), Vec2(8,4))
          assert(r.pos == Vec2(7,3))
          assert(r.size == Vec2(8,4))
          assert(r.x == 7)
          assert(r.y == 3)
          assert(r.width == 8)
          assert(r.height == 4)
        }

        'centeredRect {
          val r = Rect(Vec2(7,3), Vec2(8,4))
          assert(r.centered == Rect(Vec2(3,1), Vec2(8,4)))
        }

        'center {
          val r = Rect(Vec2(7,3), Vec2(8,4))
          assert(r.center == Vec2(11,5))
        }

        'corners {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          val c = r.corners.toList
          assert(c.toList == List(Vec2(2,3), Vec2(4,3), Vec2(4,4), Vec2(2,4)))
        }

        'edges {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          val e = r.edges.toList
          assert(e.toList == List(
            Line(Vec2(2,3), Vec2(4,3)),
            Line(Vec2(4,3), Vec2(4,4)),
            Line(Vec2(4,4), Vec2(2,4)),
            Line(Vec2(2,4), Vec2(2,3))).toList
          )
        }

        'PointInside {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          assert((Vec2(3, 3.5) isInside r) == true)
          assert((Vec2(3, 4.5) isInside r) == false)
        }

        'LineInside {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          assert((Line(Vec2(2.5,3.5), Vec2(3.5,3.5)) isInside r) == true)
          assert((Line(Vec2(2.5,3.5), Vec2(5.5,3.5)) isInside r) == false)
          assert((Line(Vec2(4.5,3.5), Vec2(5.5,3.5)) isInside r) == false)
        }
        'OverlappingRect {
          val r1 = Rect(Vec2(2,3), Vec2(4,4))
          val r2 = Rect(Vec2(1,4), Vec2(3,1))
          val r3 = Rect(Vec2(10,10), Vec2(1,1))
          assert(r1 isOverlapping r2)
          assert(r2 isOverlapping r1)
          assert(!(r1 isOverlapping r3))
          assert(!(r3 isOverlapping r1))
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
          val r = Rect(Vec2(2,3), Vec2(2,1))
          val l = Line(Vec2(3,2), Vec2(3,3.5))
          val i = (r intersect l).right.get
          val i2 = (l intersect r).right.get
          assert( i == Seq(Vec2(3, 3)) )
          assert( i2 == Seq(Vec2(3, 3)) )
        }
        'NoIntersectInside {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          val l = Line(Vec2(2.5,3.5), Vec2(3.5,3.5))
          val i = (r intersect l).left.get
          assert( i == true )
        }
        'NoIntersectOutside {
          val r = Rect(Vec2(2,3), Vec2(2,1))
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
          val r = Rect(Vec2(2,3), Vec2(2,1))
          val l = Line(Vec2(3,2), Vec2(3,3.5))
          val c = (l cutBy r).get
          assert( c == Line(Vec2(3,2), Vec2(3,3)) )
        }
        'NoCut {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          val l = Line(Vec2(3,2), Vec2(4,2))
          val c = (l cutBy r).get
          assert( c == l )
        }
        'FullCut {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          val l = Line(Vec2(2.5,3.5), Vec2(3.5,3.5))
          val c = (l cutBy r)
          assert( c == None )
        }
      }

      'ClampLineByRect {
        'Inside {
          val r = Rect(Vec2(2,3), Vec2(4,4))
          val l = Line(Vec2(3,4), Vec2(4,5))
          val c = (l clampBy r).get
          assert( c == l )
        }

        'StartOutside {
          val r = Rect(Vec2(2,3), Vec2(4,4))
          val l = Line(Vec2(1,4), Vec2(4,4))
          val c = (l clampBy r).get
          assert( c == Line(Vec2(2,4), Vec2(4,4)) )
        }

        'StartInside {
          val r = Rect(Vec2(2,3), Vec2(4,4))
          val l = Line(Vec2(4,4), Vec2(1,4))
          val c = (l clampBy r).get
          assert( c == Line(Vec2(4,4), Vec2(2,4)) )
        }

        'Outside {
          val r = Rect(Vec2(2,3), Vec2(4,4))
          val l = Line(Vec2(1,2), Vec2(-1,4))
          val c = (l clampBy r)
          assert( c == None )
        }

        'GoingThrough {
          val r = Rect(Vec2(2,3), Vec2(4,4))
          val l = Line(Vec2(1,4), Vec2(7,4))
          val c = (l clampBy r).get
          assert( c == Line(Vec2(2,4), Vec2(6,4)) )
        }
      }
    }
  }
}

