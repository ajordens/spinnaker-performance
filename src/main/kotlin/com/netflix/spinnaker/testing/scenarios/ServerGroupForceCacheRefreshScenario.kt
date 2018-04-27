/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.testing.scenarios

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.testing.ScenarioConfig
import com.netflix.spinnaker.testing.api.SpinnakerClient
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class ServerGroupForceCacheRefreshScenario(val objectMapper: ObjectMapper,
                                           val spinnakerClient: SpinnakerClient,
                                           val scenarioConfig: ScenarioConfig) : Scenario {
  private val config = objectMapper.convertValue(scenarioConfig.config, ServerGroupForceCacheRefreshConfig::class.java)
  val scenarioId = UUID.randomUUID().toString()

  override fun plan(): List<ScenarioActivity> {
    val counter = AtomicInteger(0)
    val serverGroups = spinnakerClient.getServerGroupsForApplication(config.sourceApplication).execute().body()
      ?: emptyList()

    val selectedServerGroups = serverGroups.subList(0, Math.min(serverGroups.size, scenarioConfig.executionConfig.total))
    val batchSize = selectedServerGroups.size / scenarioConfig.executionConfig.perSecondRate.toInt()

    return selectedServerGroups.map {
      counter.incrementAndGet()

      val offset = (counter.get() % batchSize) + 1
      ScenarioActivity(
        offset,
        mutableMapOf<String, Any>(
          Pair("type", "serverGroupForceCacheRefresh"),
          Pair("deploy.server.groups", mapOf(it.region to listOf(it.name))),
          Pair("scenarioId", scenarioId),
          Pair("tickId", counter.get()),
          Pair("account", it.account),
          Pair("cloudProvider", it.cloudProvider)
        ),
        scenarioConfig.name,
        config.application,
        "Force Cache Refresh: ${it.name} (id: ${scenarioId.substring(scenarioId.length - 6)}, tick: ${offset})"
      )
    }
  }
}

private data class ServerGroupForceCacheRefreshConfig(val application: String,
                                                      val sourceApplication: String)
