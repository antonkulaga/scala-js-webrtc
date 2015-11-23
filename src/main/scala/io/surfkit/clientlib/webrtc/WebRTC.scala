package io.surfkit.clientlib.webrtc

import java.util.UUID

import org.scalajs.dom.experimental.webrtc._


import scala.scalajs.js

/**
 * Created by corey auger on 13/11/15.
 */
class WebRTC[M, T <: Peer.ModelTransformPeerSignaler[M]](signaler: T) extends LocalMedia with Peer.PeerSignaler{
  println("WebRTC")

  var peers = js.Array[Peer]()

  def send(s:Peer.Signaling):Unit = {
    println(s"SEND => ${s}")
    signaler.send(s)
  }

  def peerStreamAdded(peer:Peer) = {

  }


  val rtcConfiguration = RTCConfiguration(
    iceServers = js.Array[RTCIceServer](
      RTCIceServer(url = "stun:stun.l.google.com:19302")
    )
  )
  val receiveMedia = MediaConstraints(
    mandatory = js.Dynamic.literal(OfferToReceiveAudio = true, OfferToReceiveVideo = true)
  )
  val peerConnectionConstraints = MediaConstraints(optional = js.Array[js.Dynamic](
    js.Dynamic.literal(DtlsSrtpKeyAgreement = true)
  ))

  override def localStream(stream:MediaStream):Unit = {
    println("localStream")
    /*localPeer = Some(new Peer(Peer.Props(
      id = UUID.randomUUID().toString,
      signaler = this,
      rtcConfiguration = rtcConfiguration,
      receiveMedia = receiveMedia,
      peerConnectionConstraints = peerConnectionConstraints
    )))
    peer.start()
    signaler.receivers.push({ signal:Peer.Signaling =>
      localPeer.foreach(_.handleMessage(signal))
    })*/
  }
  override def localStreamStopped(stream:MediaStream):Unit = {
    println("localStreamStopped")
  }
  override def localScreenStopped(stream:MediaStream):Unit = {
    println("localScreenStopped")
  }

  override def audioOff():Unit = {
    println("Audio Off")
  }

  override def audioOn():Unit = {
    println("Audio On")
  }

  override def videoOff():Unit = {
    println("video Off")
  }

  override def videoOn():Unit = {
    println("video On")
  }

  def createPeer(props:Peer.Props):Peer = {
    val peer = new Peer(props)
    localStreams.foreach(peer.addStream(_))
    peer.onAddStream = { s:MediaStream =>
      peerStreamAdded(peer)
    }
    peers.push(peer)
    peer
  }

  def removePeers(id:String) = {
    val (rem, rest) = peers.partition(_.id == id)
    rem.foreach(_.end)
    peers = rest
  }

}
