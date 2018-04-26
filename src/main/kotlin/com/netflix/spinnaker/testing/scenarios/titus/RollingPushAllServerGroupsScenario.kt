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


package com.netflix.spinnaker.testing.scenarios.titus

import com.fasterxml.jackson.databind.ObjectMapper
import com.netflix.spinnaker.testing.ExecutionConfig
import com.netflix.spinnaker.testing.ScenarioConfig
import com.netflix.spinnaker.testing.api.ServerGroup
import com.netflix.spinnaker.testing.api.SpinnakerClient
import com.netflix.spinnaker.testing.scenarios.Scenario
import com.netflix.spinnaker.testing.scenarios.ScenarioActivity
import java.util.*
import java.util.concurrent.atomic.AtomicInteger


class RollingPushAllServerGroupsScenario(val objectMapper: ObjectMapper,
                                         val spinnakerClient: SpinnakerClient,
                                         val scenarioConfig: ScenarioConfig) : Scenario {
  private val config = objectMapper.convertValue(scenarioConfig.config, RollingPushAllServerGroupsConfig::class.java)
  private val executionConfig = objectMapper.convertValue(scenarioConfig.executionConfig, ExecutionConfig::class.java)

  val scenarioId = UUID.randomUUID().toString()
  override fun plan(): List<ScenarioActivity> {
    val counter = AtomicInteger(0)
    return (spinnakerClient.getServerGroupsForApplication(config.application).execute().body() ?: emptyList()).map {
      counter.incrementAndGet()

      val job = config.toJob(it, counter.get().toString())
      job.put("scenarioId", scenarioId)
      job.put("tickId", counter.get())

      ScenarioActivity(
        counter.get(),
        job,
        scenarioConfig.name,
        config.application,
        "RollingPushServerGroup (id: ${scenarioId.substring(scenarioId.length - 6)}, tick: ${counter.get()}"
      )
    }
  }
}

private data class RollingPushAllServerGroupsConfig(val application: String,
                                                    val timeoutSeconds: Int
) {
  fun toJob(serverGroup: ServerGroup, stack: String): MutableMap<String, Any> {
    return mutableMapOf(
      Pair("application", application),
      Pair("strategy", "rollingpush"),
      Pair("stack", stack),
      Pair("credentials", serverGroup.account),
      Pair("useSourceCapacity", true),
      Pair("region", serverGroup.region),
      Pair("serverGroupName", serverGroup.name),
      Pair("type", "cloneServerGroup"),
      Pair("cloudProvider", "titus"),
      Pair("stageTimeoutMs", timeoutSeconds * 1000)
    )
  }
}
