package geometry

import utest._

import scala.scalajs.js

object GeometrySpec extends TestSuite {
  // console.warn polyfill
  if(js.Dynamic.global.console.warn == js.undefined)
    js.Dynamic.global.console.updateDynamic("warn")((m: String) => println(m))

  val tests = TestSuite {
    'Primitives {
      'Vec2 {
        'constructor {
          val v = Vec2(5,7)
          assert(v.x == 5)
          assert(v.y == 7)
        }
        'addition {
          val a = Vec2(5,7)
          val b = Vec2(2,3)
          val c = a + b
          assert(c.x == 7)
          assert(c.y == 10)
        }
        'multiplication {
          val a = Vec2(5,7)
          val c = a * 3
          assert(c.x == 15)
          assert(c.y == 21)
        }
      }

      'Line {
        'consstructor {
          val l = Line(Vec2(7,3), Vec2(8,4))
          assert(l.start == Vec2(7,3))
          assert(l.end == Vec2(8,4))
        }
      }

      'Rect {
        'constructor {
          val r = Rect(Vec2(7,3), Vec2(8,4))
          assert(r.pos == Vec2(7,3))
          assert(r.dim == Vec2(8,4))
        }

        'corners {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          val c = r.corners
          assert(c.toList == Array(Vec2(2,3), Vec2(4,3), Vec2(2,4), Vec2(4,4)).toList)
        }

        //TODO: edges

        'PointInside {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          assert(r.inside(Vec2(3, 3.5)) == true)
          assert(r.inside(Vec2(3, 4.5)) == false)
        }

        //TODO: line inside
      }
    }
    'Algorithms {
      import Algorithms._
      'LineIntersection {
        'Segments {
          val a = Line(Vec2(1,3), Vec2(4,1))
          val b = Line(Vec2(2,1), Vec2(3,3))
          val i = intersection(a, b).get
          assert( i.pos == Vec2(2.5, 2) )
          assert( i.onLine1 == true )
          assert( i.onLine2 == true )
        }
        'SegmentAndLine {
          val a = Line(Vec2(1,3), Vec2(4,1))
          val b = Line(Vec2(0,0), Vec2(2,2))
          val i = intersection(a, b).get
          assert( i.onLine1 == true )
          assert( i.onLine2 == false )
        }
        'LineAndSegment {
          val a = Line(Vec2(1,3), Vec2(4,1))
          val b = Line(Vec2(0,3), Vec2(1,4))
          val i = intersection(a, b).get
          assert( i.onLine1 == false )
          assert( i.onLine2 == true )
        }
        'Parallel {
          val a = Line(Vec2(1,3), Vec2(4,1))
          val b = Line(Vec2(2,3), Vec2(5,1))
          val i = intersection(a,b)
          assert( i == None )
        }
      }
      'FirstLineRectIntersection {
        'Intersect {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          val l = Line(Vec2(3,2), Vec2(3,3.5))
          val i = intersection(r, l).right.get
          assert( i == Vec2(3, 3) )
        }
        'NoIntersectInside {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          val l = Line(Vec2(2.5,3.5), Vec2(2.5,3.5))
          val i = intersection(r, l).left.get
          assert( i == true )
        }
        'NoIntersectOutside {
          val r = Rect(Vec2(2,3), Vec2(2,1))
          val l = Line(Vec2(2.5,2.5), Vec2(2.5,2.5))
          val i = intersection(r, l).left.get
          assert( i == false )
        }
      }
    }
  }
}

