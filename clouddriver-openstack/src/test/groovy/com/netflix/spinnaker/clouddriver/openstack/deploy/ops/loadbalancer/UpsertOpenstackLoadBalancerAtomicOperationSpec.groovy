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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.clouddriver.openstack.deploy.ops.loadbalancer

import com.netflix.spinnaker.clouddriver.data.task.Task
import com.netflix.spinnaker.clouddriver.data.task.TaskRepository
import com.netflix.spinnaker.clouddriver.openstack.client.OpenstackClientProvider
import com.netflix.spinnaker.clouddriver.openstack.client.OpenstackProviderFactory
import com.netflix.spinnaker.clouddriver.openstack.deploy.description.loadbalancer.OpenstackLoadBalancerDescription
import com.netflix.spinnaker.clouddriver.openstack.deploy.exception.OpenstackOperationException
import com.netflix.spinnaker.clouddriver.openstack.domain.PoolHealthMonitor
import com.netflix.spinnaker.clouddriver.openstack.security.OpenstackCredentials
import com.netflix.spinnaker.clouddriver.openstack.security.OpenstackNamedAccountCredentials
import org.openstack4j.model.network.NetFloatingIP
import org.openstack4j.model.network.ext.LbPool
import org.openstack4j.model.network.ext.Vip
import spock.lang.Specification
import spock.lang.Subject

class UpsertOpenstackLoadBalancerAtomicOperationSpec extends Specification {
  def provider
  def credentials
  def description

  def setupSpec() {
    TaskRepository.threadLocalTask.set(Mock(Task))
  }

  def setup() {
    provider = Mock(OpenstackClientProvider)
    GroovyMock(OpenstackProviderFactory, global: true)
    OpenstackNamedAccountCredentials credz = Mock(OpenstackNamedAccountCredentials)
    OpenstackProviderFactory.createProvider(credz) >> { provider }
    credentials = new OpenstackCredentials(credz)
    description = new OpenstackLoadBalancerDescription(credentials: credentials)
  }

  def "should create load balancer with floating IP - no health monitor"() {
    given:
    String region = 'west'
    String subnetId = 'subnetId'
    String floatingIpId = 'floatingIp'

    description.region = region
    description.subnetId = subnetId
    description.floatingIpId = floatingIpId

    @Subject def operation = new UpsertOpenstackLoadBalancerAtomicOperation(description)
    def newLoadBalancerPool = Mock(LbPool)
    def newVip = Mock(Vip)
    def newFloatingIp = Mock(NetFloatingIP)

    when:
    operation.operate([])

    then:
    1 * provider.validateSubnetId(_, subnetId)
    0 * provider.getLoadBalancerPool(_, description.id)
    1 * provider.createLoadBalancerPool(_, _) >> newLoadBalancerPool
    1 * provider.createVip(_, _) >> newVip
    0 * provider.createHealthCheckForPool(_, newLoadBalancerPool.id, description.healthMonitor)
    1 * provider.associateFloatingIpToVip(_, floatingIpId, _) >> newFloatingIp
    0 * provider.createFloatingIPAndAssociateToVip(_, description.networkId, _)
    noExceptionThrown()
  }

  def "should create load balancer and floating IP address - no health monitor"() {
    given:
    description.region = 'west'
    description.subnetId = 'subnetId'
    description.networkId = 'networkId'

    @Subject def operation = new UpsertOpenstackLoadBalancerAtomicOperation(description)
    def newLoadBalancerPool = Mock(LbPool)
    def newVip = Mock(Vip)
    def newFloatingIp = Mock(NetFloatingIP)

    when:
    operation.operate([])

    then:
    1 * provider.validateSubnetId(_, description.subnetId)
    0 * provider.getLoadBalancerPool(_, description.id)
    1 * provider.createLoadBalancerPool(_, _) >> newLoadBalancerPool
    1 * provider.createVip(_, _) >> newVip
    0 * provider.createHealthCheckForPool(_, newLoadBalancerPool.id, description.healthMonitor)
    0 * provider.associateFloatingIpToVip(_, description.floatingIpId, _)
    1 * provider.createFloatingIPAndAssociateToVip(_, description.networkId, _) >> newFloatingIp
    noExceptionThrown()
  }

  def "should create load balancer, floating IP address and health monitor"() {
    given:
    description.region = 'west'
    description.subnetId = 'subnetId'
    description.networkId = 'networkId'
    description.healthMonitor = Mock(PoolHealthMonitor)

    @Subject def operation = new UpsertOpenstackLoadBalancerAtomicOperation(description)
    def newLoadBalancerPool = Mock(LbPool)
    def newVip = Mock(Vip)
    def newFloatingIp = Mock(NetFloatingIP)

    when:
    operation.operate([])

    then:
    1 * provider.validateSubnetId(_, description.subnetId)
    0 * provider.getLoadBalancerPool(_, description.id)
    1 * provider.createLoadBalancerPool(_, _) >> newLoadBalancerPool
    1 * provider.createVip(_, _) >> newVip
    1 * provider.createHealthCheckForPool(_, newLoadBalancerPool.id, description.healthMonitor)
    0 * provider.associateFloatingIpToVip(_, description.floatingIpId, _)
    1 * provider.createFloatingIPAndAssociateToVip(_, description.networkId, _) >> newFloatingIp
    noExceptionThrown()
  }

  def "should throw invalid subnet id"() {
    given:
    description.region = 'west'
    description.subnetId = 'subnetId'
    @Subject def operation = new UpsertOpenstackLoadBalancerAtomicOperation(description)

    when:
    operation.operate([])

    then:
    1 * provider.validateSubnetId(_, description.subnetId) >> {
      throw new OpenstackOperationException("foobar")
    }
    OpenstackOperationException ex = thrown(OpenstackOperationException)
    ex.message == "foobar"
  }
}
