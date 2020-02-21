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

import org.joda.time.DateTime
import com.virtuslab.kubernetes.client.openapi.core.ApiModel

case class V1ReplicaSetCondition (
  /* The last time the condition transitioned from one status to another. */
  lastTransitionTime: Option[DateTime] = None,
  /* A human readable message indicating details about the transition. */
  message: Option[String] = None,
  /* The reason for the condition's last transition. */
  reason: Option[String] = None,
  /* Status of the condition, one of True, False, Unknown. */
  status: String,
  /* Type of replica set condition. */
  `type`: String
) extends ApiModel


