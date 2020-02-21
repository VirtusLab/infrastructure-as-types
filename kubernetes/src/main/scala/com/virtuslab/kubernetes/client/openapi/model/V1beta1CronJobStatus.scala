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

case class V1beta1CronJobStatus (
  /* A list of pointers to currently running jobs. */
  active: Option[Seq[V1ObjectReference]] = None,
  /* Information when was the last time the job was successfully scheduled. */
  lastScheduleTime: Option[DateTime] = None
) extends ApiModel


