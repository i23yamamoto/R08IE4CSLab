import chisel3._

// 任意進数カウンタ (ヘルパーモジュール: load信号を排除した同期クリア付きモジュロNカウンタ)
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

// 発展問題: 24時間デジタル時計コア
class DigitalClockCore extends Module {
  val io = IO(new Bundle {
    val enSec = Input(Bool()) // 1秒パルス
    val clear = Input(Bool())
    val sec   = Output(UInt(6.W)) // 0〜59
    val min   = Output(UInt(6.W)) // 0〜59
    val hour  = Output(UInt(5.W)) // 0〜23
  })

  val secCounter = Module(new ModuloNCounter(60,6))
  val minCounter = Module(new ModuloNCounter(60,6))
  val hrsCounter = Module(new ModuloNCounter(24,5))

  secCounter.io.clear := io.clear
  secCounter.io.en := io.enSec
  minCounter.io.clear := io.clear
  minCounter.io.en := io.enSec && secCounter.io.rollover
  hrsCounter.io.clear := io.clear
  hrsCounter.io.en := io.enSec && secCounter.io.rollover && minCounter.io.rollover
  // TODO: 秒(60進) -> 分(60進) -> 時(24進) を同期カスケードで結合してください
  io.sec  := secCounter.io.count
  io.min  := minCounter.io.count
  io.hour := hrsCounter.io.count
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new DigitalClockCore,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}