import chisel3._
import chisel3.util.log2Ceil

// 1Hzのen信号を出すモジュール (サブモジュール1)
class OneHzGenerator(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en         = Input(Bool())
    val clear      = Input(Bool())
    val ledBlink   = Output(Bool())
    val oneHzPulse = Output(Bool())
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq
  val cntWidth   = log2Ceil(fullPeriod)

  val cntReg = RegInit(0.U(cntWidth.W))
  val ledReg = RegInit(false.B)

  when(io.clear) {
    cntReg := 0.U
    ledReg := false.B
  }.elsewhen(io.en) {
    when(cntReg === (fullPeriod - 1).U) {
      cntReg := 0.U
      ledReg := !ledReg
    }.elsewhen(cntReg === (halfPeriod - 1).U) {
      cntReg := cntReg + 1.U
      ledReg := !ledReg
    }.otherwise {
      cntReg := cntReg + 1.U
    }
  }

  io.ledBlink   := ledReg
  io.oneHzPulse := io.en && (cntReg === (fullPeriod - 1).U)
}

// 任意進数カウンタ (サブモジュール2・3: 10進カウンタおよび6進カウンタとして使用)
class ModuloNCounter(val n: Int, val width: Int) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())
    val clear    = Input(Bool())
    val count    = Output(UInt(width.W))
    val rollover = Output(Bool())
  })
  val cntReg = RegInit(0.U(width.W))
  when(io.clear) {
    cntReg := 0.U
  }.elsewhen(io.en) {
    when(cntReg === (n - 1).U) { cntReg := 0.U }.otherwise { cntReg := cntReg + 1.U }
  }
  io.count    := cntReg
  io.rollover := io.en && (cntReg === (n - 1).U)
}

// 基本問題: 1Hz生成モジュール + 10進カウンタ + 6進カウンタによる階層化 60進カウンタ
// (FPGAボードにプログラムして動作を確認します)
class Counter60(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())
    val clear    = Input(Bool())
    val secUnits = Output(UInt(4.W)) // 1の位 (0〜9)
    val secTens  = Output(UInt(3.W)) // 10の位 (0〜5)
    val runLED   = Output(Bool())    // 0.5秒反転LED出力
    val cout     = Output(Bool())    // 59秒の最後の1クロックでアサート
  })

  // TODO: OneHzGenerator(clkFreq)、10進カウンタ ModuloNCounter(10, 4)、6進カウンタ ModuloNCounter(6, 3) の
  //       3つのサブモジュールを Module(new ...) でインスタンス化し、
  val oneHz = Module(new OneHzGenerator(clkFreq))
  val u10 = Module(new ModuloNCounter(10,4))
  val u6 = Module(new ModuloNCounter(6,3))
  //       同期イネーブル接続 (oneHzPulse -> 10進en, 10進rollover -> 6進en) で階層化設計してください。
  oneHz.io.clear := io.clear
  oneHz.io.en := io.en
  // ※ リップルクロック（非同期クロック）の使用は厳禁です
  u10.io.clear := io.clear
  u10.io.en := oneHz.io.oneHzPulse
  u6.io.clear := io.clear
  u6.io.en := u10.io.rollover

  io.secUnits := ~u10.io.count
  io.secTens  := ~u6.io.count
  io.cout     := ~u6.io.rollover
  io.runLED   := ~oneHz.io.ledBlink
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new Counter60,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}