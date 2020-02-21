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

case class V2beta2CrossVersionObjectReference (
  /* API version of the referent */
  apiVersion: Option[String] = None,
  /* Kind of the referent; More info: https://git.k8s.io/community/contributors/devel/api-conventions.md#types-kinds\" */
  kind: String,
  /* Name of the referent; More info: http://kubernetes.io/docs/user-guide/identifiers#names */
  name: String
) extends ApiModel


