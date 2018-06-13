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
package com.netflix.spinnaker.testing

import com.netflix.spinnaker.testing.harness.ScenarioRunner
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class ContinuousDriver : Driver {
  override fun drive(scenarioRunner: ScenarioRunner) {
    val timer = Timer()
    timer.scheduleAtFixedRate(ContinuousTickTask(scenarioRunner, timer), 1000, 1000)
  }
}

private class ContinuousTickTask(val scenarioRunner: ScenarioRunner, val timer: Timer) : TimerTask() {
  val count = AtomicInteger(1)

  override fun run() {
    if (scenarioRunner.tick(count.get())) {
      println("Tasks kicked off, waiting for results ...")
      cancel()
      timer.scheduleAtFixedRate(ContinuousFetchResultsTask(scenarioRunner, timer), 5000, 30000)
    }
    count.addAndGet(1)
  }
}

private class ContinuousFetchResultsTask(val scenarioRunner: ScenarioRunner, val timer: Timer) : TimerTask() {
  override fun run() {
    if (scenarioRunner.isComplete()) {
      println("All results fetched, re-driving scenarios")
      cancel()
      timer.scheduleAtFixedRate(ContinuousTickTask(scenarioRunner, timer), 1000, 1000)
    }
  }
}
