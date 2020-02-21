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
package com.virtuslab.kubernetes.client.openapi.api

import com.virtuslab.kubernetes.client.openapi.model.V1APIGroup
import com.virtuslab.kubernetes.client.openapi.core._
import com.virtuslab.kubernetes.client.openapi.core.CollectionFormats._
import com.virtuslab.kubernetes.client.openapi.core.ApiKeyLocations._

object AdmissionregistrationApi {

  def apply(baseUrl: String = "http://localhost") = new AdmissionregistrationApi(baseUrl)
}

class AdmissionregistrationApi(baseUrl: String) {
  
  /**
   * get information of a group
   * 
   * Expected answers:
   *   code 200 : V1APIGroup (OK)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   */
  def getAPIGroup()(implicit apiKey: ApiKeyValue
): ApiRequest[V1APIGroup] =
    ApiRequest[V1APIGroup](ApiMethods.GET, baseUrl, "/apis/admissionregistration.k8s.io/", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withSuccessResponse[V1APIGroup](200)
      .withErrorResponse[Unit](401)
      



}

