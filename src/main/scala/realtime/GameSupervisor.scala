package realtime

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.SpawnProtocol

object GameSupervisor {
  val main: Behavior[SpawnProtocol.Command] =
    Behaviors.setup { _ =>
      SpawnProtocol()
    }
}
