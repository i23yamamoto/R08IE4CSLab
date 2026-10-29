import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Mux4to1tb extends AnyFlatSpec with ChiselScalatestTester {
  it should "correctly select input according to sel" in {
    test(new Mux4to1).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.in0.poke(0.U)
      dut.io.in1.poke(1.U)
      dut.io.in2.poke(2.U)
      dut.io.in3.poke(3.U)

      // Test sel = 0 -> in0
      dut.io.sel.poke(0.U)
      dut.clock.step(1)
      dut.io.out.expect(0.U)
      println(s"sel=0 => out=${dut.io.out.peekInt()} (expected: 0)")

      // Test sel = 1 -> in1
      dut.io.sel.poke(1.U)
      dut.clock.step(1)
      dut.io.out.expect(1.U)
      println(s"sel=1 => out=${dut.io.out.peekInt()} (expected: 1)")

      // Test sel = 2 -> in2
      dut.io.sel.poke(2.U)
      dut.clock.step(1)
      dut.io.out.expect(2.U)
      println(s"sel=2 => out=${dut.io.out.peekInt()} (expected: 2)")

      // Test sel = 3 -> in3
      dut.io.sel.poke(3.U)
      dut.clock.step(1)
      dut.io.out.expect(3.U)
      println(s"sel=3 => out=${dut.io.out.peekInt()} (expected: 3)")
    }
  }
}
