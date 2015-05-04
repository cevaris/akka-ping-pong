package com.cevaris.pingpong

import akka.actor._
import com.typesafe.scalalogging.LazyLogging

// val PROJECT_NAME    = "akkka-ping-pong"
// val PROJECT_VERSION = "0.1"
// val parser = new scopt.OptionParser[Config](PROJECT_NAME) {
//   head(PROJECT_NAME, PROJECT_VERSION)
//   opt[Int]('s', "steps") action { (x, c) =>
//     c.copy(steps = x) } text("Number of Ping/Pong steps")
// }

trait StepMessage {
  def next: StepMessage
}
case class LastStep() extends StepMessage {
  def next: StepMessage = LastStep()
}
case class Step(value: Int) extends StepMessage {
  def next: StepMessage = {
    if (value > 0) {
      Step(value-1)
    } else {
      LastStep()
    }
  }
}

case class PingMessage(step: StepMessage)
case class PongMessage(step: StepMessage)

case class StartGame(value: Int)

class Ping(pong: ActorRef) extends Actor with LazyLogging {
  def receive = {
    case StartGame(value: Int)  =>
      logger.debug("starting")
      pong ! PingMessage(Step(value))
    case PongMessage(step: LastStep) =>
      logger.debug("stopping")
      context.stop(self)
    case PongMessage(step: Step)  =>
      logger.debug(s"Pong $step")
      pong ! PingMessage(step.next)
  }
}

class Pong() extends Actor with LazyLogging {
  def receive = {
    case PingMessage(step: LastStep) =>
      logger.debug("stopping")
      context.stop(self)
    case PingMessage(step: Step)  =>
      logger.debug(s"Ping $step")
      sender ! PongMessage(step.next)
  }
}

object PingPongApp extends App with LazyLogging {

  val system = ActorSystem("PingPongApp")
  val pong = system.actorOf(Props[Pong], name= "pong")
  val ping = system.actorOf(Props(new Ping(pong)), name= "ping")

  args.toList match {
    case steps :: Nil =>
      ping ! StartGame(steps.toInt)
    case _ =>
      ping ! StartGame(15)
  }

}
