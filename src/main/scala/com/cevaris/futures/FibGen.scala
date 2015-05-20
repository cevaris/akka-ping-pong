package com.cevaris.futures

import akka.actor._
import akka.routing.{ Broadcast, RoundRobinPool }
import com.typesafe.scalalogging.LazyLogging
import scala.math._


case class  StartupMessage(value: Int)
case object ShutdownMessage

case class FibPair(n: Int, value: Long)

trait Message
case class FibRequest(n :Int) extends Message
case class FibResponse(fibPair :FibPair) extends Message

class Fibber() extends Actor with LazyLogging {
  def sqrt5 = sqrt(5)
  def goldenRatio = (1 + sqrt5) / 2

  def fibN(n: Int): FibPair = {
    val r = floor(
      pow(goldenRatio, n)/sqrt5 + 0.5
    ).toLong
    FibPair(n, r)
  }

  def receive = {
    case ShutdownMessage =>
      context.system.shutdown
    case FibRequest(n :Int) =>
      sender ! FibResponse(fibN(n))
  }
}

class FibGen(fibber: ActorRef) extends Actor with LazyLogging {
  def stream(n: Int) = Stream.range(1, n)

  def receive = {
    case ShutdownMessage =>
      context.system.shutdown
    case StartupMessage(value: Int) =>
      logger.debug(s"starting $value")
      stream(value) foreach (fibber ! FibRequest(_))
      fibber ! ShutdownMessage
    case FibResponse(fibPair :FibPair) =>
      logger.debug(s"$fibPair")
  }
}


object FibGenApp extends App with LazyLogging {
  val ActorCount = 5
  val router = RoundRobinPool(ActorCount)
  val system = ActorSystem("PingPongApp")
  val fibbers = system.actorOf(Props[Fibber].withRouter(router), name="fibber")
  val fibGen = system.actorOf(Props(new FibGen(fibbers)), name="fibgen")

  args.toList match {
    case n :: Nil =>
      fibGen ! StartupMessage(n.toInt)
    case _ =>
      fibGen ! StartupMessage(15)
  }
}
