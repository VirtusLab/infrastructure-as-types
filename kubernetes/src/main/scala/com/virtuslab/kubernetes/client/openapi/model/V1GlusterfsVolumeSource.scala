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

case class V1GlusterfsVolumeSource (
  /* EndpointsName is the endpoint name that details Glusterfs topology. More info: https://releases.k8s.io/HEAD/examples/volumes/glusterfs/README.md#create-a-pod */
  endpoints: String,
  /* Path is the Glusterfs volume path. More info: https://releases.k8s.io/HEAD/examples/volumes/glusterfs/README.md#create-a-pod */
  path: String,
  /* ReadOnly here will force the Glusterfs volume to be mounted with read-only permissions. Defaults to false. More info: https://releases.k8s.io/HEAD/examples/volumes/glusterfs/README.md#create-a-pod */
  readOnly: Option[Boolean] = None
) extends ApiModel


