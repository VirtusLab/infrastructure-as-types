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

case class V1ServerAddressByClientCIDR (
  /* The CIDR with which clients can match their IP to figure out the server address that they should use. */
  clientCIDR: String,
  /* Address of this server, suitable for a client that matches the above CIDR. This can be a hostname, hostname:port, IP or IP:port. */
  serverAddress: String
) extends ApiModel


