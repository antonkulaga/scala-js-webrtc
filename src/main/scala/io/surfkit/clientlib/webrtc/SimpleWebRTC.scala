package io.surfkit.clientlib.webrtc

import java.util.UUID

import io.surfkit.clientlib.webrtc.Peer.{Signaling, PeerInfo}
import org.scalajs.dom._
import scala.concurrent.{Promise, Future}
import scala.scalajs.js
import org.scalajs.dom.experimental.webrtc._
import org.scalajs.dom
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}
import org.scalajs.dom.experimental.mediastream._

/**
 * Created by corey auger on 13/11/15.
 */
class SimpleWebRTC[M, T <: Peer.ModelTransformPeerSignaler[M]](signaler: T, config:RTCConfiguration) extends WebRTC[M, T](signaler,config) {


  def startLocalVideo(constraints:MediaStreamConstraints, videoElm:dom.html.Video):Future[MediaStream] = {
    startLocalMedia(constraints).map{ stream: MediaStream =>
      val videoDyn = (videoElm.asInstanceOf[js.Dynamic])
      videoDyn.muted = true
      videoDyn.srcObject = stream
      videoDyn.style.display = "block"
      stream
    }
  }

  def joinRoom(name:String):Future[Peer.Room] = {
    val p = Promise[Peer.Room]()
    // clear known peers...
    peers.foreach(_.end)
    peers = js.Array[Peer]()
    signaler.receivers =  js.Array[(Signaling) => Unit]()   // setup new receivers
    signaler.send(Peer.Join(Peer.EmptyPeer, signaler.localPeer, name))
    signaler.receivers.push({
      case r:Peer.Room if r.name == name =>
        println("Got room..")
        r.members.filter(_.id != signaler.localPeer.id).foreach{ m:Peer.PeerInfo =>
          val peer = createPeer( Peer.Props(
            remote = Peer.PeerInfo(m.id, m.`type`),
            local = signaler.localPeer,
            signaler = signaler,
            rtcConfiguration = config
          ))
          localStreams.foreach(peer.addStream(_))
          peer.start(name)
        }
        p.complete(Try(r))
      case o:Peer.Offer if o.local.id != signaler.localPeer.id =>
        println(s"GOT AN OFFER ... for ${o.local}")
        peers.find(_.remote.id == o.local.id) match{
          case Some(peer) =>
            println("Offer for found peer")
            peer.handleMessage(o)
          case None =>
            println("Offer for new peer...")
            val peer = createPeer( Peer.Props(
              remote = o.local,
              local = signaler.localPeer,
              signaler = signaler,
              rtcConfiguration = config
            ))
            peer.handleMessage(o)
        }

      case msg if msg.local.id != signaler.localPeer.id =>
        println(s"msg ${msg}")
        peers.foreach(_.handleMessage(msg))

      case _ =>
        println("ignore message...")
        // don't care..
    })
    p.future
  }
}
