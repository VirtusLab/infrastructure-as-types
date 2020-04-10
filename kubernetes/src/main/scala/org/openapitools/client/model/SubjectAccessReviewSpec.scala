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
   * SubjectAccessReviewSpec is a description of the access request.  Exactly one of ResourceAuthorizationAttributes and NonResourceAuthorizationAttributes must be set
   */
case class SubjectAccessReviewSpec(
  /* Extra corresponds to the user.Info.GetExtra() method from the authenticator.  Since that is input to the authorizer it needs a reflection here. */
  extra: Option[Map[String, Seq[String]]] = None,
  /* Groups is the groups you're testing for. */
  groups: Option[Seq[String]] = None,
  nonResourceAttributes: Option[NonResourceAttributes] = None,
  resourceAttributes: Option[ResourceAttributes] = None,
  /* UID information about the requesting user. */
  uid: Option[String] = None,
  /* User is the user you're testing for. If you specify \"User\" but not \"Groups\", then is it interpreted as \"What if User were not a member of any groups */
  user: Option[String] = None
) extends ApiModel

