package com.vivareal.search.simulation

import com.typesafe.config.ConfigFactory.load
import com.typesafe.config.ConfigValueFactory.fromAnyRef
import com.vivareal.search.config.ScenariosLoader
import com.vivareal.search.config.SearchAPIv2Feeder.feeder
import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class SearchAPIv2Simulation extends Simulation {

  private val globalConfig = load()

  private val users = globalConfig.getInt("gatling.users")
  private val repeat = globalConfig.getInt("gatling.repeat")

  private val runIncludeScenarios = globalConfig.getString("gatling.includeScenarios")
  private val runIncludeScenariosSpl = runIncludeScenarios.split(",").toList
  private val runExcludeScenariosSpl = globalConfig.getString("gatling.excludeScenarios").split(",").toList

  private val httpConf = http.baseURL(s"http://${globalConfig.getString("api.http.base")}")

  private val path = globalConfig.getString("api.http.path")

  private val index = globalConfig.getString("api.index")

  private val scenariosConf = ScenariosLoader.load()

  private var scenarios = scenariosConf.getObjectList("scenarios").asScala
    .map(configValue => configValue.toConfig)
    .filter(config => !runExcludeScenariosSpl.contains(config.getString("scenario.id")))
    .filter(config => "_all".equals(runIncludeScenarios) || runIncludeScenariosSpl.contains(config.getString("scenario.id")))
    .map(config => {

      println(s"Preparing current scenario: ${config.getString("scenario.title")}")

      def updatedConfig = config.withValue("scenario.users", fromAnyRef(if (users > 0) users else config.getInt("scenario.users")))
        .withValue("scenario.repeat", fromAnyRef(if (repeat > 0) repeat else config.getInt("scenario.repeat")))

      scenario(updatedConfig.getString("scenario.decription"))
        .repeat(updatedConfig.getInt("scenario.repeat")) {
          feed(feeder(updatedConfig).random)
            .exec(http(updatedConfig.getString("scenario.title")).get(path + index + updatedConfig.getString("scenario.query")))
        }.inject(rampUsers(updatedConfig.getInt("scenario.users")) over (globalConfig.getInt("gatling.rampUp") seconds))
    }).toList

  setUp(scenarios)
    .protocols(httpConf)
    .maxDuration(globalConfig.getInt("gatling.maxDuration") seconds)

}
