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

import com.virtuslab.kubernetes.client.openapi.model.V1APIResourceList
import com.virtuslab.kubernetes.client.openapi.model.V1LocalSubjectAccessReview
import com.virtuslab.kubernetes.client.openapi.model.V1SelfSubjectAccessReview
import com.virtuslab.kubernetes.client.openapi.model.V1SelfSubjectRulesReview
import com.virtuslab.kubernetes.client.openapi.model.V1SubjectAccessReview
import com.virtuslab.kubernetes.client.openapi.core._
import com.virtuslab.kubernetes.client.openapi.core.CollectionFormats._
import com.virtuslab.kubernetes.client.openapi.core.ApiKeyLocations._

object AuthorizationV1Api {

  def apply(baseUrl: String = "http://localhost") = new AuthorizationV1Api(baseUrl)
}

class AuthorizationV1Api(baseUrl: String) {
  
  /**
   * create a LocalSubjectAccessReview
   * 
   * Expected answers:
   *   code 200 : V1LocalSubjectAccessReview (OK)
   *   code 201 : V1LocalSubjectAccessReview (Created)
   *   code 202 : V1LocalSubjectAccessReview (Accepted)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param dryRun When present, indicates that modifications should not be persisted. An invalid or unrecognized dryRun directive will result in an error response and no further processing of the request. Valid values are: - All: all dry run stages will be processed
   * @param fieldManager fieldManager is a name associated with the actor or entity that is making these changes. The value must be less than or 128 characters long, and only contain printable characters, as defined by https://golang.org/pkg/unicode/#IsPrint.
   * @param namespace object name and auth scope, such as for teams and projects
   * @param pretty If 'true', then the output is pretty printed.
   * @param body 
   */
  def createNamespacedLocalSubjectAccessReview(dryRun: Option[String] = None, fieldManager: Option[String] = None, namespace: String, pretty: Option[String] = None, body: V1LocalSubjectAccessReview)(implicit apiKey: ApiKeyValue
): ApiRequest[V1LocalSubjectAccessReview] =
    ApiRequest[V1LocalSubjectAccessReview](ApiMethods.POST, baseUrl, "/apis/authorization.k8s.io/v1/namespaces/{namespace}/localsubjectaccessreviews", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withBody(body)
      .withQueryParam("dryRun", dryRun)
      .withQueryParam("fieldManager", fieldManager)
      .withQueryParam("pretty", pretty)
      .withPathParam("namespace", namespace)
      .withSuccessResponse[V1LocalSubjectAccessReview](200)
      .withErrorResponse[V1LocalSubjectAccessReview](201)
      .withErrorResponse[V1LocalSubjectAccessReview](202)
      .withErrorResponse[Unit](401)
      

  /**
   * create a SelfSubjectAccessReview
   * 
   * Expected answers:
   *   code 200 : V1SelfSubjectAccessReview (OK)
   *   code 201 : V1SelfSubjectAccessReview (Created)
   *   code 202 : V1SelfSubjectAccessReview (Accepted)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param dryRun When present, indicates that modifications should not be persisted. An invalid or unrecognized dryRun directive will result in an error response and no further processing of the request. Valid values are: - All: all dry run stages will be processed
   * @param fieldManager fieldManager is a name associated with the actor or entity that is making these changes. The value must be less than or 128 characters long, and only contain printable characters, as defined by https://golang.org/pkg/unicode/#IsPrint.
   * @param pretty If 'true', then the output is pretty printed.
   * @param body 
   */
  def createSelfSubjectAccessReview(dryRun: Option[String] = None, fieldManager: Option[String] = None, pretty: Option[String] = None, body: V1SelfSubjectAccessReview)(implicit apiKey: ApiKeyValue
): ApiRequest[V1SelfSubjectAccessReview] =
    ApiRequest[V1SelfSubjectAccessReview](ApiMethods.POST, baseUrl, "/apis/authorization.k8s.io/v1/selfsubjectaccessreviews", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withBody(body)
      .withQueryParam("dryRun", dryRun)
      .withQueryParam("fieldManager", fieldManager)
      .withQueryParam("pretty", pretty)
      .withSuccessResponse[V1SelfSubjectAccessReview](200)
      .withErrorResponse[V1SelfSubjectAccessReview](201)
      .withErrorResponse[V1SelfSubjectAccessReview](202)
      .withErrorResponse[Unit](401)
      

  /**
   * create a SelfSubjectRulesReview
   * 
   * Expected answers:
   *   code 200 : V1SelfSubjectRulesReview (OK)
   *   code 201 : V1SelfSubjectRulesReview (Created)
   *   code 202 : V1SelfSubjectRulesReview (Accepted)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param dryRun When present, indicates that modifications should not be persisted. An invalid or unrecognized dryRun directive will result in an error response and no further processing of the request. Valid values are: - All: all dry run stages will be processed
   * @param fieldManager fieldManager is a name associated with the actor or entity that is making these changes. The value must be less than or 128 characters long, and only contain printable characters, as defined by https://golang.org/pkg/unicode/#IsPrint.
   * @param pretty If 'true', then the output is pretty printed.
   * @param body 
   */
  def createSelfSubjectRulesReview(dryRun: Option[String] = None, fieldManager: Option[String] = None, pretty: Option[String] = None, body: V1SelfSubjectRulesReview)(implicit apiKey: ApiKeyValue
): ApiRequest[V1SelfSubjectRulesReview] =
    ApiRequest[V1SelfSubjectRulesReview](ApiMethods.POST, baseUrl, "/apis/authorization.k8s.io/v1/selfsubjectrulesreviews", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withBody(body)
      .withQueryParam("dryRun", dryRun)
      .withQueryParam("fieldManager", fieldManager)
      .withQueryParam("pretty", pretty)
      .withSuccessResponse[V1SelfSubjectRulesReview](200)
      .withErrorResponse[V1SelfSubjectRulesReview](201)
      .withErrorResponse[V1SelfSubjectRulesReview](202)
      .withErrorResponse[Unit](401)
      

  /**
   * create a SubjectAccessReview
   * 
   * Expected answers:
   *   code 200 : V1SubjectAccessReview (OK)
   *   code 201 : V1SubjectAccessReview (Created)
   *   code 202 : V1SubjectAccessReview (Accepted)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param dryRun When present, indicates that modifications should not be persisted. An invalid or unrecognized dryRun directive will result in an error response and no further processing of the request. Valid values are: - All: all dry run stages will be processed
   * @param fieldManager fieldManager is a name associated with the actor or entity that is making these changes. The value must be less than or 128 characters long, and only contain printable characters, as defined by https://golang.org/pkg/unicode/#IsPrint.
   * @param pretty If 'true', then the output is pretty printed.
   * @param body 
   */
  def createSubjectAccessReview(dryRun: Option[String] = None, fieldManager: Option[String] = None, pretty: Option[String] = None, body: V1SubjectAccessReview)(implicit apiKey: ApiKeyValue
): ApiRequest[V1SubjectAccessReview] =
    ApiRequest[V1SubjectAccessReview](ApiMethods.POST, baseUrl, "/apis/authorization.k8s.io/v1/subjectaccessreviews", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withBody(body)
      .withQueryParam("dryRun", dryRun)
      .withQueryParam("fieldManager", fieldManager)
      .withQueryParam("pretty", pretty)
      .withSuccessResponse[V1SubjectAccessReview](200)
      .withErrorResponse[V1SubjectAccessReview](201)
      .withErrorResponse[V1SubjectAccessReview](202)
      .withErrorResponse[Unit](401)
      

  /**
   * get available resources
   * 
   * Expected answers:
   *   code 200 : V1APIResourceList (OK)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   */
  def getAPIResources()(implicit apiKey: ApiKeyValue
): ApiRequest[V1APIResourceList] =
    ApiRequest[V1APIResourceList](ApiMethods.GET, baseUrl, "/apis/authorization.k8s.io/v1/", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withSuccessResponse[V1APIResourceList](200)
      .withErrorResponse[Unit](401)
      



}
