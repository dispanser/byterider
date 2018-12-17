package org.kulturguerilla.byterider

import org.kulturguerilla.byterider.DeltaCompression._
import org.scalacheck.Gen
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Inside, Matchers}


abstract class TestBase

class DeltaCompressionSuite extends FlatSpec with PropertyChecks with Matchers with Inside {

  def randomSeq(): Gen[Array[Int]] =
    for {
      n <- Gen.choose(0, 50)
      xs <- Gen.listOfN(n, for { e <- Gen.choose(-100000000, 100000000) } yield e)
    } yield xs.toArray

  "variable length integer coding" should "yield input as result" in {
    forAll { i: Int =>
      whenever(inRange(i)) { // we need one bit to store the int internally
        val encoded = encodeInt(i)
        decodeInt(encoded) should be (i) } } }

  it should "only take one byte for values x where |x| < 64" in {
    forAll(Gen.choose(-63,63)) { i =>
      encodeInt(i) should have length 1 } }

  "delta encoding" should "yield input as output" ignore {
    forAll { is: Array[Int] =>
      decode(encode(is)) should be (is) } }

  "variable length delta encoding" should "yield input as output" in {
    forAll(randomSeq()) { is =>
      whenever(is.forall(inRange)) {
        decode(encode(is)) should be (is) } } }

  def inRange(i: Int): Boolean = i < 1000000000 && i > -1000000000

  def printBytes(in: Array[Byte]): Unit = println(in.map(_.toHexString).mkString("[", "|", "]"))
}
