package com.virtuslab.internal

import enumeratum._

object Port {
  sealed private[internal] trait Protocol extends EnumEntry
  private[internal] object Protocol extends Enum[Protocol] {
    case object TCP extends Protocol
    case object UDP extends Protocol
    case object SCTP extends Protocol

    override def values = findValues
  }
}

sealed trait Port {
  def containerPort: Int
  def hostIp: Option[String]
  def hostPort: Option[Int]
  def name: Option[String]
  protected def protocol: Port.Protocol
}

final case class TCPPort(
    containerPort: Int,
    hostIp: Option[String] = None,
    hostPort: Option[Int] = None,
    name: Option[String] = None)
  extends Port {
  override protected def protocol: Port.Protocol = Port.Protocol.TCP
}

final case class UDPPort(
    containerPort: Int,
    hostIp: Option[String] = None,
    hostPort: Option[Int] = None,
    name: Option[String] = None)
  extends Port {
  override protected def protocol: Port.Protocol = Port.Protocol.UDP
}

final case class SCTPPort(
    containerPort: Int,
    hostIp: Option[String] = None,
    hostPort: Option[Int] = None,
    name: Option[String] = None)
  extends Port {
  override protected def protocol: Port.Protocol = Port.Protocol.SCTP
}
