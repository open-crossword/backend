package realtime

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.ws.TextMessage

import scala.collection.mutable.ListBuffer

object Game {

  sealed trait Command

  case class PlayerJoined(playerId: String) extends Command

  case class WSConnected(playerId: String,
                         // fixme: using an untyped ActorRef here
                         //  because the websocket handler is using Source
                         //  rather than ActorSource
                         actorRef: akka.actor.ActorRef
                        ) extends Command

  case class WSHandleDropped(playerId: String) extends Command

  case class WSMessage(playerId: String, str: String) extends Command

  case class HandleError(throwable: Throwable) extends Command

  case class PlayerInfo(id: String, out: akka.actor.ActorRef)

  def main(gameName: String): Behavior[Command] =
    Behaviors.setup { context =>
      val log = context.system.log
      var players = Map[String, PlayerInfo]()

      log.info("game actor setup")
      Behaviors.receiveMessage {
        case WSConnected(playerId, out) =>
          players += (playerId -> PlayerInfo(playerId, out))
          out ! TextMessage(
            s"""Hi there! Welcome to $gameName.
               |there are ${players.size} player(s) online.
               |ids: [${players.keys.mkString(",")}]
               |""".stripMargin)

          players.values.filterNot(_.id == playerId).foreach(otherPlayer =>
            otherPlayer.out ! TextMessage(s"\n$playerId has joined\n")
          )

          Behaviors.same
        case WSHandleDropped(playerId) =>
          players -= playerId
          players.values.foreach(otherPlayer =>
            otherPlayer.out ! TextMessage(s"\n$playerId has left the building...\n")
          )
          log.info(s"${context.self} handle dropped: $playerId")
          if (players.isEmpty) {
            log.info(s"${context.self} shutting down")
            Behaviors.stopped
          } else {
            Behaviors.same
          }
        case HandleError(err) =>
          log.error(s"error, shutting down! $err")
          Behaviors.stopped
        case WSMessage(playerId, message) =>
          players.values.filterNot(_.id == playerId).foreach(otherPlayer =>
            otherPlayer.out ! TextMessage(s"\n$playerId sez $message\n")
          )
          Behaviors.same
        case _ => Behaviors.same
      }
    }

}
