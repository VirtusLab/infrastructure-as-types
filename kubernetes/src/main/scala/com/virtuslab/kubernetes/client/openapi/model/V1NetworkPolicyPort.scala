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

import com.virtuslab.kubernetes.client.custom.IntOrString
import com.virtuslab.kubernetes.client.openapi.core.ApiModel

case class V1NetworkPolicyPort (
  /* IntOrString is a type that can hold an int32 or a string.  When used in JSON or YAML marshalling and unmarshalling, it produces or consumes the inner type.  This allows you to have, for example, a JSON field that can accept a name or number. */
  port: Option[IntOrString] = None,
  /* The protocol (TCP, UDP, or SCTP) which traffic must match. If not specified, this field defaults to TCP. */
  protocol: Option[String] = None
) extends ApiModel


