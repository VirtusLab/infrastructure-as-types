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
package org.openapitools.client.model

import com.virtuslab.kubernetes.client.openapi.core.ApiModel

  /**
   * FSGroupStrategyOptions defines the strategy type and options used to create the strategy.
   */
case class FSGroupStrategyOptions(
  /* ranges are the allowed ranges of fs groups.  If you would like to force a single fs group then supply a single range with the same start and end. Required for MustRunAs. */
  ranges: Option[Seq[IDRange]] = None,
  /* rule is the strategy that will dictate what FSGroup is used in the SecurityContext. */
  rule: Option[String] = None
) extends ApiModel


