/*
 * Copyright 2017 Armory
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

package com.netflix.spinnaker.testing.scenarios.kubernetes

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.testing.ScenarioConfig
import com.netflix.spinnaker.testing.scenarios.Scenario
import com.netflix.spinnaker.testing.scenarios.ScenarioActivity
import com.netflix.spinnaker.testing.api.SpinnakerClient
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DestroyAllServerGroupsScenario(val objectMapper: ObjectMapper,
                                     val spinnakerClient: SpinnakerClient,
                                     val scenarioConfig: ScenarioConfig) : Scenario {
  private val config = objectMapper.convertValue(scenarioConfig.config, DestroyAllServerGroupsScenarioConfig::class.java)
  val scenarioId = UUID.randomUUID().toString()

  override fun plan(): List<ScenarioActivity> {
    val counter = AtomicInteger(0)

    var serverGroupsForApplication = spinnakerClient.getServerGroupsForApplication(config.application).execute().body() ?: emptyList()
    var serverGroupManagersForApplication = spinnakerClient.getServerGroupManagersForApplication(config.application).execute().body() ?: emptyList()


    var serverGroupActivity = serverGroupsForApplication.map {
      counter.incrementAndGet()
      ScenarioActivity(
        counter.get(),
        mutableMapOf<String, Any>(
          Pair("type", "deleteManifest"),
          Pair("cloudProvider", "kubernetes"),
          Pair("account", it.account),
          Pair("credentials", it.account),
          Pair("location", it.region),
          Pair("namespace", it.region),
          Pair("manifestName", it.name),
          Pair("scenarioId", scenarioId),
          Pair("tickId", counter.get()),
          Pair("options", mutableMapOf(Pair("orphanDependents", false)))
        ),
        scenarioConfig.name,
        config.application,
        "Delete Manifest: ${it.name} (id: ${scenarioId.substring(scenarioId.length - 6)}, tick: ${counter.get()})"
      )
    }

    var serverGroupManagerActivity = serverGroupManagersForApplication.map {
      counter.incrementAndGet()
      ScenarioActivity(
        counter.get(),
        mutableMapOf<String, Any>(
          Pair("type", "deleteManifest"),
          Pair("cloudProvider", "kubernetes"),
          Pair("account", it.account),
          Pair("credentials", it.account),
          Pair("location", it.region),
          Pair("manifestName", it.name),
          Pair("scenarioId", scenarioId),
          Pair("tickId", counter.get()),
          Pair("options", mutableMapOf(Pair("orphanDependents", false)))
        ),
        scenarioConfig.name,
        config.application,
        "Delete Manifest: ${it.name} (id: ${scenarioId.substring(scenarioId.length - 6)}, tick: ${counter.get()})"
      )
    }

    return serverGroupActivity + serverGroupManagerActivity
  }
}

private data class DestroyAllServerGroupsScenarioConfig(val application: String)
