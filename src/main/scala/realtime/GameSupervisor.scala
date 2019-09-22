package realtime

import java.util.UUID

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object GameSupervisor {

  sealed trait Command

  case class PlayerConnected(playerId: String,
                             gameId: String,
                             replyTo: ActorRef[ActorRef[Game.Command]]) extends Command

  case class GameTerminated(gameId: String) extends Command

  val main: Behavior[Command] =
    Behaviors.setup { context =>
      def spawnGame(gameId: String): ActorRef[Game.Command] = {
        context.log.info("GameSupervisor spawning a new game")
        context.spawn(Game.main(gameId), UUID.randomUUID().toString)
      }

      var runningGames = Map[String, ActorRef[Game.Command]]().withDefault(spawnGame)

      Behaviors.receiveMessage {
        case PlayerConnected(playerId, gameId, replyTo) =>
          context.log.info(s"GameSupervisor received PlayerConnected($playerId, $gameId)")

          // get-or-create the Game and notify of new player
          val game = runningGames(gameId)
          game ! Game.PlayerJoined(playerId)
          runningGames = runningGames + (gameId -> game)

          // send back the ActorRef so the websocket flow can be created
          replyTo ! game

          context.watchWith(game, GameTerminated(gameId))
          Behaviors.same
        case GameTerminated(gameId) =>
          context.log.info(s"GameSupervisor witnessed termination of $gameId")
          runningGames -= gameId
          Behaviors.same
      }
    }
}
