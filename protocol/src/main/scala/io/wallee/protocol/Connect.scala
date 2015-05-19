package io.wallee.protocol

/**
 * MQTT CONNECT packet.
 *
 * @see http://docs.oasis-open.org/mqtt/mqtt/v3.1.1/os/mqtt-v3.1.1-os.html#_Toc398718028
 */
case class Connect(protocolName: String) extends MqttPacket {

  override protected def remainingLength: Int = 0
}
