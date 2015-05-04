package com.cevaris.pingpong

import akka.actor._
import com.typesafe.scalalogging.LazyLogging

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
    case PongMessage(step: LastStep) =>
      logger.debug("stopping")
      context.stop(self)
    case PongMessage(step: Step)  =>
      logger.debug(s"Pong $step")
      pong ! PingMessage(step.next)
    case StartGame(value: Int)  =>
      logger.debug("starting")
      pong ! PingMessage(Step(value))
  }
}

class Pong() extends Actor with LazyLogging {
  def receive = {
    case PingMessage(step: Step)  =>
      logger.debug(s"Ping $step")
      sender ! PongMessage(step.next)
    case PingMessage(step: LastStep) =>
      logger.debug("stopping")
      context.stop(self)
  }
}

object PingPongApp extends App with LazyLogging {
  val system = ActorSystem("PingPongApp")
  val pong = system.actorOf(Props[Pong], name= "pong")
  val ping = system.actorOf(Props(new Ping(pong)), name= "ping")

  ping ! StartGame(99)
}
