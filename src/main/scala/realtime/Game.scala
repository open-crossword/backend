package realtime

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.ws.TextMessage

object Game {

  sealed trait Command

  case class WSConnected(actorRef: akka.actor.ActorRef) extends Command

  case object WSHandleDropped extends Command

  case class WSMessage(str: String) extends Command

  case class HandleError(throwable: Throwable) extends Command

  val main: Behavior[Command] =
    Behaviors.setup { context =>
      val log = context.system.log

      log.info("game actor setup")
      Behaviors.receiveMessage {
        case WSConnected(outboundRef) =>
          outboundRef ! TextMessage("yo dawg")
          Behaviors.same
        case WSHandleDropped =>
          log.info("handle dropped, shutting down")
          Behaviors.stopped
        case HandleError(err) =>
          log.error(s"error, shutting down! $err")
          Behaviors.stopped
        case WSMessage(message) =>
          log.info(s"game actor received message! $message")
          Behaviors.same
        case _ => Behaviors.same
      }
    }

}
