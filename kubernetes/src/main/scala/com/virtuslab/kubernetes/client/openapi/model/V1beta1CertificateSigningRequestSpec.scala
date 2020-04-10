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

import com.virtuslab.kubernetes.client.custom.ArrayByte
import com.virtuslab.kubernetes.client.openapi.core.ApiModel

case class V1beta1CertificateSigningRequestSpec (
  /* Extra information about the requesting user. See user.Info interface for details. */
  extra: Option[Map[String, Seq[String]]] = None,
  /* Group information about the requesting user. See user.Info interface for details. */
  groups: Option[Seq[String]] = None,
  /* Base64-encoded PKCS#10 CSR data */
  request: ArrayByte,
  /* UID information about the requesting user. See user.Info interface for details. */
  uid: Option[String] = None,
  /* allowedUsages specifies a set of usage contexts the key will be valid for. See: https://tools.ietf.org/html/rfc5280#section-4.2.1.3      https://tools.ietf.org/html/rfc5280#section-4.2.1.12 */
  usages: Option[Seq[String]] = None,
  /* Information about the requesting user. See user.Info interface for details. */
  username: Option[String] = None
) extends ApiModel

