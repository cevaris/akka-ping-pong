package com.cevaris.pingpong

import akka.actor._
import akka.routing.{ Broadcast, RoundRobinPool }
import com.typesafe.scalalogging.LazyLogging

trait StepMessage
object StepMessage {
  def next(step: StepMessage) = step match {
    case FinalStep()   => step
    case Step(v: Int) if v >  0 => Step(v-1)
    case Step(v: Int) if v <= 0 => FinalStep()
  }
}
case class FinalStep() extends StepMessage
case class Step(value: Int) extends StepMessage


case class PingMessage(step: StepMessage)
case class PongMessage(step: StepMessage)

case class  StartupMessage(value: Int)
case object ShutdownMessage

class Ping(pong: ActorRef) extends Actor with LazyLogging {
  def receive = {
    case ShutdownMessage =>
      context.system.shutdown
    case StartupMessage(value: Int) =>
      logger.debug("starting")
      pong ! PingMessage(Step(value))
    case PongMessage(step: FinalStep) =>
      logger.debug("stopping")
    case PongMessage(step: Step)  =>
      logger.debug(s"Pong $step")
      pong ! PingMessage(StepMessage.next(step))
  }
}

class Pong() extends Actor with LazyLogging {
  def receive = {
    case ShutdownMessage =>
      context.system.shutdown
    case PingMessage(step: FinalStep) =>
      logger.debug("stopping")
    case PingMessage(step: Step)  =>
      logger.debug(s"Ping $step")
      sender ! PongMessage(StepMessage.next(step))
  }
}

object PingPongApp extends App with LazyLogging {

  val ActorCount = 5
  val router = RoundRobinPool(ActorCount)
  val system = ActorSystem("PingPongApp")
  val pong = system.actorOf(Props[Pong].withRouter(router), name="pong")
  val ping = system.actorOf(Props(new Ping(pong)), name="ping")

  args.toList match {
    case steps :: Nil =>
      for (n <- 0 until ActorCount-1)
        ping ! StartupMessage(steps.toInt)
    case _ =>
      ping ! StartupMessage(15)
  }

  Thread.sleep(2000)
  system.shutdown
}
