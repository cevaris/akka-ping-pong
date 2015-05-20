package com.cevaris.bowser

import akka.actor._
import akka.routing.{ Broadcast, RoundRobinPool }
import com.typesafe.scalalogging.LazyLogging
import com.redis._
import java.net.URL
import scala.io.Source
import scalaj.http._


case object StartupMessage
case object ShutdownMessage

case class ToDownload(url: URL)
case class ToExtract(url: URL, document: String)


class RootActor(crawler: ActorRef) extends Actor with LazyLogging {
  def urls = List(
    "http://marvel.com/",
    "https://news.ycombinator.com",
    "http://www.foxnews.com",
    "http://www.reuters.com"
  )

  def receive = {
    case ShutdownMessage =>
      context.system.shutdown
    case StartupMessage =>
      logger.debug("starting")
      for(url <- urls)
        crawler ! ToDownload(new URL(url))
  }
}

class UrlCrawlerActor(extractor: ActorRef) extends Actor with LazyLogging {

  lazy val redis = new RedisClient("localhost", 6379)

  def receive = {
    case ShutdownMessage =>
      context.system.shutdown
    case ToDownload(url: URL) =>
      logger.debug(s"Downloading: $url")
      val response: HttpResponse[String] = Http(url.toString).asString
      logger.debug(s"Downloaded $url ${response.code} ${response.body.length}")
      extractor ! ToExtract(url, response.body)
  }
}

class UrlExtractorActor() extends Actor with LazyLogging {

  lazy val redis = new RedisClient("localhost", 6379)
  lazy val URL_REGEX = """href=['"]([^'" >]+)""".r

  def isWhiteList(s: String) =
    s.startsWith("http") || s.startsWith("www")
  def isBlackList(s: String) =
    s.isEmpty || s.startsWith("mailto:") || s.contains("ad.doubleclick.net")

  def handle(sourceUrl: URL, extractedUrl: String ) = {
    extractedUrl.trim match {
      case s if isWhiteList(s) =>
        logger.debug(s"Found Valid url $s")
        //redis.sadd("raw-urls", s)
        // self ToExtract(url: URL)
      case s if isBlackList(s) =>
        logger.debug(s"Dropping '$s'")
      case s =>
        logger.debug(s"Found possible partial url ${sourceUrl.getHost+s}")
    }
  }

  def receive = {
    case ShutdownMessage =>
      context.system.shutdown
    case ToExtract(url: URL, document: String) =>
      logger.debug(s"Extracting links from $url")
      // DONOT FORGOT: url.getHost
      URL_REGEX.findAllIn(document).matchData foreach { m =>
        handle(url, m.group(1))
      }
      //redis.hset("pages", url, Source.fromURL(url).mkString)
  }
}

object BowserApp extends App with LazyLogging {
  val system = ActorSystem("Bowser")
  val urlExtractor = system.actorOf(
    Props[UrlExtractorActor].withRouter(RoundRobinPool(5)),
    name="url-extractor"
  )
  val urlCrawler = system.actorOf(
    Props(new UrlCrawlerActor(urlExtractor)).withRouter(RoundRobinPool(5)),
    name="url-crawler"
  )
  val root = system.actorOf(
    Props(new RootActor(urlCrawler)), name="root"
  )

  root ! StartupMessage
}
