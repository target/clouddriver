/*
 * Copyright 2016 Target, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.openstack.deploy.ops.servergroup

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.openstack.client.OpenstackClientProvider
import com.netflix.spinnaker.clouddriver.openstack.deploy.OpenstackServerGroupNameResolver
import com.netflix.spinnaker.clouddriver.openstack.deploy.description.servergroup.DeployOpenstackAtomicOperationDescription
import com.netflix.spinnaker.clouddriver.openstack.deploy.description.servergroup.DestroyOpenstackAtomicOperationDescription
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import groovy.util.logging.Slf4j
import org.openstack4j.model.heat.Stack

@Slf4j
class DestroyOpenstackAtomicOperation implements AtomicOperation<Void> {
  private final String BASE_PHASE = "DEPLOY"
  DestroyOpenstackAtomicOperationDescription description;

  DestroyOpenstackAtomicOperation(DestroyOpenstackAtomicOperationDescription description) {
    this.description = description
  }

  protected static Task getTask() {
    TaskRepository.threadLocalTask.get()
  }

  /*
  * curl -X POST -H "Content-Type: application/json" -d '[ { "createServerGroup": { "stack": "test-stack", "application": "myapp", "region": "RegionOne", "heatTemplate": "{\"heat_template_version\":\"2013-05-23\",\"description\":\"Simple template to test heat commands\",\"parameters\":{\"flavor\":{\"default\":\"m1.nano\",\"type\":\"string\"}},\"resources\":{\"hello_world\":{\"type\":\"OS::Nova::Server\",\"properties\":{\"flavor\":{\"get_param\":\"flavor\"},\"image\":\"cirros-0.3.4-x86_64-uec\",\"user_data\":\"\"}}}}", "timeoutMins": "5", "account": "test" }} ]' localhost:7002/openstack/ops
  * curl -X GET -H "Accept: application/json" localhost:7002/task/1
  */
  @Override
  Void operate(List priorOutputs) {
    OpenstackClientProvider provider = description.credentials.provider

    task.updateStatus BASE_PHASE, "Initializing destruction of server group"

    task.updateStatus BASE_PHASE, "Looking up heat stack ${description.stack}..."
    Stack stack = provider.getStack(description.stack)
    task.updateStatus BASE_PHASE, "Found heat stack ${description.stack}..."

    task.updateStatus BASE_PHASE, "Destroying heat stack ${stack.name} with id ${stack.id}..."
    provider.destroy(stack)
    task.updateStatus BASE_PHASE, "Destroyed heat stack ${stack.name} with id ${stack.id}..."

    task.updateStatus BASE_PHASE, "Successfully destroyed server group"
  }
}
