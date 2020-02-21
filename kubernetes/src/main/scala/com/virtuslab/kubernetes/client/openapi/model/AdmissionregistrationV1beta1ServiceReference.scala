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

case class AdmissionregistrationV1beta1ServiceReference (
  /* `name` is the name of the service. Required */
  name: String,
  /* `namespace` is the namespace of the service. Required */
  namespace: String,
  /* `path` is an optional URL path which will be sent in any request to this service. */
  path: Option[String] = None,
  /* If specified, the port on the service that hosting webhook. Default to 443 for backward compatibility. `port` should be a valid port number (1-65535, inclusive). */
  port: Option[Int] = None
) extends ApiModel


