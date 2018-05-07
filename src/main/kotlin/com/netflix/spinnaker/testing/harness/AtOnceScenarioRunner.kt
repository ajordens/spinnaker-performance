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
package com.netflix.spinnaker.testing.harness

import com.netflix.spinnaker.testing.api.SpinnakerClient
import com.netflix.spinnaker.testing.api.Task
import com.netflix.spinnaker.testing.scenarios.Scenario
import com.netflix.spinnaker.testing.scenarios.ScenarioActivity

open class AtOnceScenarioRunner(
  private val spinnakerClient: SpinnakerClient,
  protected val scenarios: List<Scenario>
) : ScenarioRunner {
  protected val allActivities = mutableMapOf<Int, List<ScenarioActivity>>().withDefault { mutableListOf() }

  override fun plan() {
    allActivities.putAll(
      scenarios.flatMap { it.plan() }.groupBy { it.secondsOffset }
    )
    println(allActivities)
  }

  override fun tick(secondsOffset: Int): Boolean {
    for (activity in allActivities.getValue(secondsOffset)) {
      val response = spinnakerClient.submitTask(
        Task(
          arrayListOf(activity.job),
          activity.application,
          activity.description
        )
      ).execute()

      activity.taskId = response.body()?.ref?.replace("/tasks/", "")
    }

    val maxSecondsOffset = allActivities.keys.max() ?: 0
    return secondsOffset > maxSecondsOffset
  }

  override fun fetchResults(): List<ScenarioActivity> {
    val activitiesMissingResults = allActivities.values.flatten().filter { it.taskResult == null }

    activitiesMissingResults
      .filter { it.taskId != null }
      .forEach {
        val taskResult = spinnakerClient.getTask(it.taskId!!).execute().body()
        if (taskResult != null && taskResult.status != "RUNNING") {
          it.taskResult = taskResult
        }
      }

    return allActivities.values.flatten().filter { it.taskResult != null }
  }

  override fun isComplete(): Boolean {
    fetchResults()
    return allActivities.values.flatten().none { it.taskResult == null }
  }
}
