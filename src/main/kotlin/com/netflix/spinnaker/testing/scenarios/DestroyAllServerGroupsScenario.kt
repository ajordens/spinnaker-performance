/*
 * Copyright 2017 Netflix, Inc.
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

class DestroyAllServerGroupsScenario(val objectMapper: ObjectMapper,
                                     val spinnakerClient: SpinnakerClient,
                                     val scenarioConfig: ScenarioConfig) : Scenario {
  private val config = objectMapper.convertValue(scenarioConfig.config, DestroyAllServerGroupsConfig::class.java)
  val scenarioId = UUID.randomUUID().toString()

  override fun plan(): List<ScenarioActivity> {
    val counter = AtomicInteger(0)
    return (spinnakerClient.getServerGroupsForApplication(config.application).execute().body() ?: emptyList()).map {
      counter.incrementAndGet()

      ScenarioActivity(
        counter.get(),
        mutableMapOf<String, Any>(
          Pair("type", "destroyServerGroup"),
          Pair("asgName", it.name),
          Pair("serverGroupName", it.name),
          Pair("credentials", it.account),
          Pair("region", it.region),
          Pair("scenarioId", scenarioId),
          Pair("tickId", counter.get()),
          Pair("cloudProvider", "aws")
        ),
        scenarioConfig.name,
        config.application,
        "Destroy Server Group: ${it.name} (id: ${scenarioId.substring(scenarioId.length - 6)}, tick: ${counter.get()})"
      )
    }
  }
}

private data class DestroyAllServerGroupsConfig(val application: String)
