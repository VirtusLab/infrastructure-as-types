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

case class V1EndpointPort (
  /* The name of this port (corresponds to ServicePort.Name). Must be a DNS_LABEL. Optional only if one port is defined. */
  name: Option[String] = None,
  /* The port number of the endpoint. */
  port: Int,
  /* The IP protocol for this port. Must be UDP, TCP, or SCTP. Default is TCP. */
  protocol: Option[String] = None
) extends ApiModel


