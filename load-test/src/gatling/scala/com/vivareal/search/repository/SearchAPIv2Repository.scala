package com.vivareal.search.repository

import com.typesafe.config.ConfigFactory
import org.json4s.native.JsonMethods._
import org.json4s.{DefaultFormats, JValue}

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.Try
import scalaj.http.{Http, HttpResponse}

object SearchAPIv2Repository {
  implicit val formats = DefaultFormats

  val config = ConfigFactory.load()

  val gatling = config.getConfig("gatling")

  val http = config.getConfig("api.http")

  val facets = config.getConfig("api.facets")

  def getFacets: List[(String, String)] = {
    facets.getStringList("list").asScala.foldLeft(List[(String, String)]()) { (list, facet) => {
      val limit: Int = Try { facets.getInt(s"limits.$facet") }.getOrElse(10)
      val response: HttpResponse[String] = Http(s"http://${http.getString("base")}${http.getString("listings")}?size=0&facets=$facet&facetSize=$limit").asString
      val json: JValue = parse(response.body)
      val values = (json \ "result" \ "facets").extract[Map[String, Map[String, Long]]]
      list ++ values.head._2.keySet.map(v => (values.head._1, v))
    }}
  }

  def getIds: Iterator[String] = {
    val size = gatling.getInt("ids.users") * gatling.getInt("repeat")
    Source.fromURL(s"http://${http.getString("base")}${http.getString("listings")}/stream?includeFields=id&size=$size")
      .getLines
      .take(size)
      .map(parse(_))
      .map(json => (json \ "id").extract[String])
  }
}
