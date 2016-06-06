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

package com.netflix.spinnaker.clouddriver.openstack.client

import com.netflix.spinnaker.clouddriver.openstack.deploy.exception.OpenstackOperationException
import com.netflix.spinnaker.clouddriver.openstack.domain.LoadBalancerPool
import com.netflix.spinnaker.clouddriver.openstack.domain.PoolHealthMonitor
import com.netflix.spinnaker.clouddriver.openstack.domain.Region
import com.netflix.spinnaker.clouddriver.openstack.domain.VirtualIP
import com.netflix.spinnaker.clouddriver.orchestration.AtomicOperations
import org.openstack4j.api.Builders
import org.openstack4j.api.OSClient
import org.openstack4j.model.common.ActionResponse
import org.openstack4j.model.compute.RebootType
import org.openstack4j.model.network.NetFloatingIP
import org.openstack4j.model.network.Port
import org.openstack4j.model.network.ext.HealthMonitor
import org.openstack4j.model.network.ext.HealthMonitorType
import org.openstack4j.model.network.ext.LbMethod
import org.openstack4j.model.network.ext.LbPool
import org.openstack4j.model.network.ext.Protocol
import org.openstack4j.model.network.ext.Vip

/**
 * Provides access to the Openstack API.
 *
 * TODO tokens will need to be regenerated if they are expired.
 */
abstract class OpenstackClientProvider {

  /**
   * Delete an instance.
   * @param instanceId
   * @return
   */
  void deleteInstance(String instanceId) {
    handleRequest(AtomicOperations.TERMINATE_INSTANCES) {
      client.compute().servers().delete(instanceId)
    }
  }

  /**
   * Reboot an instance ... Default to SOFT reboot if not passed.
   * @param instanceId
   * @return
   */
  void rebootInstance(String instanceId, RebootType rebootType = RebootType.SOFT) {
    handleRequest(AtomicOperations.REBOOT_INSTANCES) {
      client.compute().servers().reboot(instanceId, rebootType)
    }
  }

  /**
   * Gets a load balancer pool for a given region and load balancer UUID.
   * @param region
   * @param loadBalancerId
   * @return
   */
  LbPool getLoadBalancerPool(final Region region, final String loadBalancerId) {
    client.useRegion(region.name).networking().loadbalancers().lbPool().get(loadBalancerId)
  }

  /**
   * Validates the subnet is valid in region.
   * @param region
   * @param subnetId
   * @return boolean
   */
  boolean validateSubnetId (final Region region, final String subnetId) {
    client.useRegion(region.name).networking().subnet().get(subnetId) == null
  }

  /**
   * Creates a load balancer pool in provided region.
   * @param region
   * @param loadBalancerPool
   * @return LbPool
   */
  LbPool createLoadBalancerPool(final Region region, final LoadBalancerPool loadBalancerPool) {
    Protocol poolProtocol = Protocol.forValue(loadBalancerPool.protocol.name())
    LbMethod poolMethod = LbMethod.forValue(loadBalancerPool.method.name())

    getRegionClient(region).networking().loadbalancers().lbPool().create(
      Builders.lbPool()
        .name(loadBalancerPool.derivedName)
        .protocol(poolProtocol)
        .lbMethod(poolMethod)
        .subnetId(loadBalancerPool.subnetId)
        .description(loadBalancerPool.description)
        .adminStateUp(Boolean.TRUE)
        .build()
    )
  }

  /**
   * Creates a VIP for given region and pool.
   * @param region
   * @param virtualIP
   * @return
   */
  Vip createVip(final Region region, final VirtualIP virtualIP) {
    Protocol vipProtocol = Protocol.forValue(virtualIP.protocol.name())

    getRegionClient(region).networking().loadbalancers().vip().create(Builders.vip()
      .name(virtualIP.derivedName)
      .subnetId(virtualIP.subnetId)
      .poolId(virtualIP.poolId)
      .protocol(vipProtocol)
      .protocolPort(virtualIP.port)
      .adminStateUp(Boolean.TRUE)
      .build())
  }

  /**
   * Creates a health check for given pool in specified region.
   * @param region
   * @param lbPoolId
   * @param monitor
   * @return
   */
  HealthMonitor createHealthCheckForPool(final Region region, final String lbPoolId, final PoolHealthMonitor monitor) {
      HealthMonitor result = getRegionClient(region).networking().loadbalancers().healthMonitor().create(
        Builders.healthMonitor().type(HealthMonitorType.forValue(monitor.type?.name()))
          .delay(monitor.delay)
          .timeout(monitor.timeout)
          .maxRetries(monitor.maxRetries)
          .httpMethod(monitor.httpMethod)
          .urlPath(monitor.url)
          .expectedCodes(monitor.expectedHttpStatusCodes?.join(","))
          .adminStateUp(Boolean.TRUE)
          .build())
    getRegionClient(region).networking().loadbalancers().lbPool().associateHealthMonitor(lbPoolId, result.id)
      result
  }

  /**
   * Create floating IP and associate to VIP.
   * @param region
   * @param networkId
   * @param vipId
   * @return
   */
  NetFloatingIP createFloatingIPAndAssociateToVip(final Region region, final String networkId, final String vipId) {
    internalAssociateFloatingIPToVip(region, null, networkId, vipId)
  }

  /**
   * Associate already known floating IP address to VIP in specified region.
   * @param region
   * @param floatingIpId
   * @param vipId
   * @return
   */
  NetFloatingIP associateFloatingIpToVip(final Region region, final String floatingIpId, final String vipId) {
    internalAssociateFloatingIPToVip(region, floatingIpId, null, vipId)
  }

  private NetFloatingIP internalAssociateFloatingIPToVip(final Region region, final String floatingIpId, final String networkId, final String vipId) {
    NetFloatingIP result
    Port port = getRegionClient(region).networking().port().list()?.find{ it.name == "vip-${vipId}" }
    String internalFloatingIpId = floatingIpId
    if (port) {
      if (networkId) {
        NetFloatingIP netFloatingIP = getRegionClient(region).networking().floatingip().create(Builders.netFloatingIP()
          .floatingNetworkId(networkId)
          .build())
        internalFloatingIpId = netFloatingIP.id
      }
      result = getRegionClient(region).networking().floatingip().associateToPort(internalFloatingIpId, port.id)
    }
    result
  }

  /**
   * Handler for an openstack4j request.
   * @param closure
   * @return
   */
  ActionResponse handleRequest(String operation, Closure closure) {
    ActionResponse result
    try {
      result = closure()
    } catch (Exception e) {
      throw new OpenstackOperationException(operation, e)
    }
    if (!result.isSuccess()) {
      throw new OpenstackOperationException(result, operation)
    }
    result
  }

  /**
   * Thread-safe way to get client.
   * @return
   */
  abstract OSClient getClient()

  /**
   * Get a new token id.
   * @return
   */
  abstract String getTokenId()

  /**
   * Helper method to get region based thread-safe OS client.
   * @param region
   * @return
     */
  OSClient getRegionClient (Region region) {
    client.useRegion(region.name)
  }
}
