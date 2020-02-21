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

case class V1alpha1Policy (
  /* The Level that all requests are recorded at. available options: None, Metadata, Request, RequestResponse required */
  level: String,
  /* Stages is a list of stages for which events are created. */
  stages: Option[Seq[String]] = None
) extends ApiModel


