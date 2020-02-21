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

case class V1DeploymentStatus (
  /* Total number of available pods (ready for at least minReadySeconds) targeted by this deployment. */
  availableReplicas: Option[Int] = None,
  /* Count of hash collisions for the Deployment. The Deployment controller uses this field as a collision avoidance mechanism when it needs to create the name for the newest ReplicaSet. */
  collisionCount: Option[Int] = None,
  /* Represents the latest available observations of a deployment's current state. */
  conditions: Option[Seq[V1DeploymentCondition]] = None,
  /* The generation observed by the deployment controller. */
  observedGeneration: Option[Long] = None,
  /* Total number of ready pods targeted by this deployment. */
  readyReplicas: Option[Int] = None,
  /* Total number of non-terminated pods targeted by this deployment (their labels match the selector). */
  replicas: Option[Int] = None,
  /* Total number of unavailable pods targeted by this deployment. This is the total number of pods that are still required for the deployment to have 100% available capacity. They may either be pods that are running but not yet available or pods that still have not been created. */
  unavailableReplicas: Option[Int] = None,
  /* Total number of non-terminated pods targeted by this deployment that have the desired template spec. */
  updatedReplicas: Option[Int] = None
) extends ApiModel


