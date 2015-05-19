package io.wallee.protocol

abstract class MqttPacket {

  def lengthInBytes: Int = {
    val remainingLengthBytes: Int = (scala.math.log(remainingLength) / scala.math.log(128)).floor.toInt
    return 1 + remainingLengthBytes + remainingLength
  }

  protected def remainingLength: Int
}
