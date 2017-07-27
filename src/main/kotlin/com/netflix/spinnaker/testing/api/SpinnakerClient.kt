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


package com.netflix.spinnaker.testing.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SpinnakerClient {
  @GET("/health")
  fun health(): Call<Map<String, Any>>

  @POST("/tasks")
  fun submitTask(@Body task: Task): Call<SubmittedTask>

  @GET("/applications/{application}/serverGroups")
  fun getServerGroupsForApplication(@Path("application") application: String): Call<List<ServerGroup>>

  @GET("/applications/{application}/tasks")
  fun getTasksForApplication(@Path("application") application: String): Call<List<TaskResult>>

  @GET("/tasks/{taskId}")
  fun getTask(@Path("taskId") taskId: String): Call<TaskResult>
}

data class ServerGroup(val name: String, val account: String, val region: String, val cloudProvider: String)

data class Task(val job: List<Map<String, Any>>, val application: String, val description: String)

data class SubmittedTask(val ref: String)

data class TaskResult(val id: String,
                      val status: String,
                      val variables: List<TaskVariable>,
                      val buildTime: Long,
                      val startTime: Long,
                      val endTime: Long) {
  fun getVariable(key: String): TaskVariable? = variables.find { it.key == key }
  fun getDuration() = (endTime - startTime) / 1000
  fun getLag() = (startTime - buildTime) / 1000
}

data class TaskVariable(val key: String, val value: Any?)
