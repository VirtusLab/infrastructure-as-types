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

case class V1RoleRef (
  /* APIGroup is the group for the resource being referenced */
  apiGroup: String,
  /* Kind is the type of resource being referenced */
  kind: String,
  /* Name is the name of resource being referenced */
  name: String
) extends ApiModel


