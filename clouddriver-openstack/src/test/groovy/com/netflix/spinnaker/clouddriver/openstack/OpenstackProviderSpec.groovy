package com.netflix.spinnaker.clouddriver.openstack

import spock.lang.Specification

class OpenstackProviderSpec extends Specification {

  def "test provider settings" () {
    expect:
    OpenstackProvider provider = new OpenstackProvider()
    provider.displayName == 'Openstack'
    provider.id == 'ops'
    provider.operationAnnotationType == OpenstackOperation
  }
}
