package com.netflix.spinnaker.clouddriver.openstack

import com.netflix.spinnaker.clouddriver.core.CloudProvider
import org.springframework.stereotype.Component

import java.lang.annotation.Annotation

@Component
class OpenstackProvider implements CloudProvider {
  final String id = "ops"
  final String displayName = "Openstack"
  final Class<? extends Annotation> operationAnnotationType = OpenstackOperation
}
