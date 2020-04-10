/**
 * Kubernetes
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1.15.10
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
package com.virtuslab.kubernetes.client.openapi.model

import com.virtuslab.kubernetes.client.openapi.core.ApiModel

case class V2beta1MetricStatus (
  external: Option[V2beta1ExternalMetricStatus] = None,
  `object`: Option[V2beta1ObjectMetricStatus] = None,
  pods: Option[V2beta1PodsMetricStatus] = None,
  resource: Option[V2beta1ResourceMetricStatus] = None,
  /* type is the type of metric source.  It will be one of \"Object\", \"Pods\" or \"Resource\", each corresponds to a matching field in the object. */
  `type`: String
) extends ApiModel

