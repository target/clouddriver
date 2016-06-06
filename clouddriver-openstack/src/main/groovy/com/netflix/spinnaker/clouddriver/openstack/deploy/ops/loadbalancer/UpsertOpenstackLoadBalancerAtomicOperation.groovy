/*
 * Copyright 2016 Target, Inc.
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

package com.netflix.spinnaker.clouddriver.openstack.deploy.ops.loadbalancer

import com.netflix.spinnaker.clouddriver.openstack.client.OpenstackClientProvider
import com.netflix.spinnaker.clouddriver.openstack.deploy.description.loadbalancer.OpenstackLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.openstack.deploy.exception.OpenstackOperationException
import com.netflix.spinnaker.clouddriver.openstack.domain.LoadBalancerPool
import com.netflix.spinnaker.clouddriver.openstack.domain.Region
import com.netflix.spinnaker.clouddriver.openstack.domain.VirtualIP
import com.netflix.spinnaker.clouddriver.openstack.task.TaskAware
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperation
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import org.openstack4j.model.network.NetFloatingIP
import org.openstack4j.model.network.ext.LbPool
import org.openstack4j.model.network.ext.Vip

class UpsertOpenstackLoadBalancerAtomicOperation implements AtomicOperation<Map>, TaskAware {
  OpenstackLoadBalancerDescription description

  UpsertOpenstackLoadBalancerAtomicOperation(OpenstackLoadBalancerDescription description) {
    this.description = description
  }

  /*
   * curl -X POST -H "Content-Type: application/json" -d  '[ {  "upsertLoadBalancer": { "region": "region", "account": "test", "name": "test",  "protocol": "HTTP", "method" : "ROUND_ROBIN", "subnetId": "9e0d71a9-0086-494a-91d8-abad0912ba83", "externalPort": 80, "internalPort": 8100, "networkId": "9e0d71a9-0086-494a-91d8-abad0912ba83", "healthMonitor": { "type": "PING", "delay": 10, "timeout": 10, "maxRetries": 10 } } } ]' localhost:7002/openstack/ops
   */
  @Override
  Map operate(List priorOutputs) {
    task.updateStatus UPSERT_LOADBALANCER_PHASE, "Initializing upsert of load balancer ${description.id ?: description.name} in ${description.region}..."
    String subnetId = description.subnetId
    Region region = new Region(name: description.region)
    LoadBalancerPool newLoadBalancerPool = new LoadBalancerPool(
      id: description.id,
      name: description.name,
      protocol: description.protocol,
      method: description.method,
      subnetId: description.subnetId,
      internalPort: description.internalPort)

    OpenstackClientProvider openstackClientProvider = this.description.credentials.provider

    if (openstackClientProvider.validateSubnetId(region, subnetId)) {
      task.updateStatus UPSERT_LOADBALANCER_PHASE, "Unable to retrieve referenced subnet ${subnetId} in ${region.name}."
      throw new OpenstackOperationException(AtomicOperations.UPSERT_LOAD_BALANCER)
    }

    LbPool resultPool
    if (newLoadBalancerPool.id) {
      resultPool = openstackClientProvider.getLoadBalancerPool(region, newLoadBalancerPool.id)
    }

    if (resultPool) {
      task.updateStatus UPSERT_LOADBALANCER_PHASE, "Found load balancer pool ${newLoadBalancerPool.id} in ${region.name}."
    } else {
      task.updateStatus UPSERT_LOADBALANCER_PHASE, "Creating lbPool ${newLoadBalancerPool.derivedName} in ${region.name}..."
      resultPool = openstackClientProvider.createLoadBalancerPool(region, newLoadBalancerPool)
      task.updateStatus UPSERT_LOADBALANCER_PHASE, "Created lbPool ${newLoadBalancerPool.derivedName} in ${region.name}"

      task.updateStatus UPSERT_LOADBALANCER_PHASE, "Creating vip for lbPool ${resultPool.name} in ${region.name}..."
      Vip vip = openstackClientProvider.createVip(region, new VirtualIP(
        name: description.name,
        subnetId: subnetId,
        poolId: resultPool.id,
        protocol: description.protocol,
        port: description.externalPort))
      task.updateStatus UPSERT_LOADBALANCER_PHASE, "Created vip for lbPool ${resultPool.name} in ${region} with name ${vip.name}."

      if (description.healthMonitor) {
        task.updateStatus UPSERT_LOADBALANCER_PHASE, "Creating health checks for lbPool ${resultPool.name} in ${region.name}..."
        openstackClientProvider.createHealthCheckForPool(region, resultPool.id, description.healthMonitor)
        task.updateStatus UPSERT_LOADBALANCER_PHASE, "Created health checks for lbPool ${resultPool.name} in ${region.name}."
      }

      if (description.floatingIpId) {
        task.updateStatus UPSERT_LOADBALANCER_PHASE, "Associating floating IP ${description.floatingIpId} with ${vip.id}..."
        NetFloatingIP floatingIP = openstackClientProvider.associateFloatingIpToVip(region, description.floatingIpId, vip.id)
        task.updateStatus UPSERT_LOADBALANCER_PHASE, "Associated floating IP ${floatingIP.floatingIpAddress} with ${vip.id}."
      } else {
        if (description.networkId) {
          task.updateStatus UPSERT_LOADBALANCER_PHASE, "Creating floating IP in network ${description.networkId} with ${vip.id}..."
          NetFloatingIP floatingIP = openstackClientProvider.createFloatingIPAndAssociateToVip(region, description.networkId, vip.id)
          task.updateStatus UPSERT_LOADBALANCER_PHASE, "Created floating IP in network ${description.networkId} with address ${floatingIP.floatingIpAddress} against vip ${vip.id}."
        }
      }
    }

    task.updateStatus UPSERT_LOADBALANCER_PHASE, "Done upserting load balancer ${resultPool?.name} in ${region.name}"
    return [(region): [id: resultPool?.id]]
  }
}
