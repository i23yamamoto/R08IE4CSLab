import chisel3._

// 基本問題1: 4-to-1 マルチプレクサ
class Mux4to1 extends Module {
  val io = IO(new Bundle {
    val in0 = Input(UInt(2.W))
    val in1 = Input(UInt(2.W))
    val in2 = Input(UInt(2.W))
    val in3 = Input(UInt(2.W))
    val sel = Input(UInt(2.W))
    val out = Output(UInt(2.W))
  })

  // TODO: sel (0〜3) の値に応じて in0〜in3 を選択出力してください
  io.out := 0.U
  val muxLow = Mux(io.sel(0), io.in1, io.in0)
  val muxHigh = Mux(io.sel(0), io.in3, io.in2)
  val finalOut = Mux(io.sel(1), muxHigh, muxLow)
  io.out := ~finalOut
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new Mux4to1,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}
