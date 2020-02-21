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

case class V1EnvFromSource (
  configMapRef: Option[V1ConfigMapEnvSource] = None,
  /* An optional identifier to prepend to each key in the ConfigMap. Must be a C_IDENTIFIER. */
  prefix: Option[String] = None,
  secretRef: Option[V1SecretEnvSource] = None
) extends ApiModel


