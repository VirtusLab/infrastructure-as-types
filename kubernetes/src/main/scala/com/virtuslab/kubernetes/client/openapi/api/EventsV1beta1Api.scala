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
import com.virtuslab.kubernetes.client.openapi.model.V1DeleteOptions
import com.virtuslab.kubernetes.client.custom.V1Patch
import com.virtuslab.kubernetes.client.openapi.model.V1Status
import com.virtuslab.kubernetes.client.openapi.model.V1beta1Event
import com.virtuslab.kubernetes.client.openapi.model.V1beta1EventList
import com.virtuslab.kubernetes.client.openapi.core._
import com.virtuslab.kubernetes.client.openapi.core.CollectionFormats._
import com.virtuslab.kubernetes.client.openapi.core.ApiKeyLocations._

object EventsV1beta1Api {

  def apply(baseUrl: String = "http://localhost") = new EventsV1beta1Api(baseUrl)
}

class EventsV1beta1Api(baseUrl: String) {
  
  /**
   * create an Event
   * 
   * Expected answers:
   *   code 200 : V1beta1Event (OK)
   *   code 201 : V1beta1Event (Created)
   *   code 202 : V1beta1Event (Accepted)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param namespace object name and auth scope, such as for teams and projects
   * @param pretty If 'true', then the output is pretty printed.
   * @param dryRun When present, indicates that modifications should not be persisted. An invalid or unrecognized dryRun directive will result in an error response and no further processing of the request. Valid values are: - All: all dry run stages will be processed
   * @param fieldManager fieldManager is a name associated with the actor or entity that is making these changes. The value must be less than or 128 characters long, and only contain printable characters, as defined by https://golang.org/pkg/unicode/#IsPrint.
   * @param body 
   */
  def createNamespacedEvent(namespace: String, pretty: Option[String] = None, dryRun: Option[String] = None, fieldManager: Option[String] = None, body: V1beta1Event)(implicit apiKey: ApiKeyValue
): ApiRequest[V1beta1Event] =
    ApiRequest[V1beta1Event](ApiMethods.POST, baseUrl, "/apis/events.k8s.io/v1beta1/namespaces/{namespace}/events", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withBody(body)
      .withQueryParam("pretty", pretty)
      .withQueryParam("dryRun", dryRun)
      .withQueryParam("fieldManager", fieldManager)
      .withPathParam("namespace", namespace)
      .withSuccessResponse[V1beta1Event](200)
      .withErrorResponse[V1beta1Event](201)
      .withErrorResponse[V1beta1Event](202)
      .withErrorResponse[Unit](401)
      

  /**
   * delete collection of Event
   * 
   * Expected answers:
   *   code 200 : V1Status (OK)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param namespace object name and auth scope, such as for teams and projects
   * @param pretty If 'true', then the output is pretty printed.
   * @param allowWatchBookmarks allowWatchBookmarks requests watch events with type \"BOOKMARK\". Servers that do not implement bookmarks may ignore this flag and bookmarks are sent at the server's discretion. Clients should not assume bookmarks are returned at any specific interval, nor may they assume the server will send any BOOKMARK event during a session. If this is not a watch, this field is ignored. If the feature gate WatchBookmarks is not enabled in apiserver, this field is ignored.  This field is alpha and can be changed or removed without notice.
   * @param continue The continue option should be set when retrieving more results from the server. Since this value is server defined, clients may only use the continue value from a previous query result with identical query parameters (except for the value of continue) and the server may reject a continue value it does not recognize. If the specified continue value is no longer valid whether due to expiration (generally five to fifteen minutes) or a configuration change on the server, the server will respond with a 410 ResourceExpired error together with a continue token. If the client needs a consistent list, it must restart their list without the continue field. Otherwise, the client may send another list request with the token received with the 410 error, the server will respond with a list starting from the next key, but from the latest snapshot, which is inconsistent from the previous list results - objects that are created, modified, or deleted after the first list request will be included in the response, as long as their keys are after the \"next key\".  This field is not supported when watch is true. Clients may start a watch from the last resourceVersion value returned by the server and not miss any modifications.
   * @param dryRun When present, indicates that modifications should not be persisted. An invalid or unrecognized dryRun directive will result in an error response and no further processing of the request. Valid values are: - All: all dry run stages will be processed
   * @param fieldSelector A selector to restrict the list of returned objects by their fields. Defaults to everything.
   * @param gracePeriodSeconds The duration in seconds before the object should be deleted. Value must be non-negative integer. The value zero indicates delete immediately. If this value is nil, the default grace period for the specified type will be used. Defaults to a per object value if not specified. zero means delete immediately.
   * @param labelSelector A selector to restrict the list of returned objects by their labels. Defaults to everything.
   * @param limit limit is a maximum number of responses to return for a list call. If more items exist, the server will set the `continue` field on the list metadata to a value that can be used with the same initial query to retrieve the next set of results. Setting a limit may return fewer than the requested amount of items (up to zero items) in the event all requested objects are filtered out and clients should only use the presence of the continue field to determine whether more results are available. Servers may choose not to support the limit argument and will return all of the available results. If limit is specified and the continue field is empty, clients may assume that no more results are available. This field is not supported if watch is true.  The server guarantees that the objects returned when using continue will be identical to issuing a single list call without a limit - that is, no objects created, modified, or deleted after the first request is issued will be included in any subsequent continued requests. This is sometimes referred to as a consistent snapshot, and ensures that a client that is using limit to receive smaller chunks of a very large result can ensure they see all possible objects. If objects are updated during a chunked list the version of the object that was present at the time the first list result was calculated is returned.
   * @param orphanDependents Deprecated: please use the PropagationPolicy, this field will be deprecated in 1.7. Should the dependent objects be orphaned. If true/false, the \"orphan\" finalizer will be added to/removed from the object's finalizers list. Either this field or PropagationPolicy may be set, but not both.
   * @param propagationPolicy Whether and how garbage collection will be performed. Either this field or OrphanDependents may be set, but not both. The default policy is decided by the existing finalizer set in the metadata.finalizers and the resource-specific default policy. Acceptable values are: 'Orphan' - orphan the dependents; 'Background' - allow the garbage collector to delete the dependents in the background; 'Foreground' - a cascading policy that deletes all dependents in the foreground.
   * @param resourceVersion When specified with a watch call, shows changes that occur after that particular version of a resource. Defaults to changes from the beginning of history. When specified for list: - if unset, then the result is returned from remote storage based on quorum-read flag; - if it's 0, then we simply return what we currently have in cache, no guarantee; - if set to non zero, then the result is at least as fresh as given rv.
   * @param timeoutSeconds Timeout for the list/watch call. This limits the duration of the call, regardless of any activity or inactivity.
   * @param watch Watch for changes to the described resources and return them as a stream of add, update, and remove notifications. Specify resourceVersion.
   * @param body 
   */
  def deleteCollectionNamespacedEvent(namespace: String, pretty: Option[String] = None, allowWatchBookmarks: Option[Boolean] = None, continue: Option[String] = None, dryRun: Option[String] = None, fieldSelector: Option[String] = None, gracePeriodSeconds: Option[Int] = None, labelSelector: Option[String] = None, limit: Option[Int] = None, orphanDependents: Option[Boolean] = None, propagationPolicy: Option[String] = None, resourceVersion: Option[String] = None, timeoutSeconds: Option[Int] = None, watch: Option[Boolean] = None, body: Option[V1DeleteOptions] = None)(implicit apiKey: ApiKeyValue
): ApiRequest[V1Status] =
    ApiRequest[V1Status](ApiMethods.DELETE, baseUrl, "/apis/events.k8s.io/v1beta1/namespaces/{namespace}/events", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withBody(body)
      .withQueryParam("pretty", pretty)
      .withQueryParam("allowWatchBookmarks", allowWatchBookmarks)
      .withQueryParam("continue", continue)
      .withQueryParam("dryRun", dryRun)
      .withQueryParam("fieldSelector", fieldSelector)
      .withQueryParam("gracePeriodSeconds", gracePeriodSeconds)
      .withQueryParam("labelSelector", labelSelector)
      .withQueryParam("limit", limit)
      .withQueryParam("orphanDependents", orphanDependents)
      .withQueryParam("propagationPolicy", propagationPolicy)
      .withQueryParam("resourceVersion", resourceVersion)
      .withQueryParam("timeoutSeconds", timeoutSeconds)
      .withQueryParam("watch", watch)
      .withPathParam("namespace", namespace)
      .withSuccessResponse[V1Status](200)
      .withErrorResponse[Unit](401)
      

  /**
   * delete an Event
   * 
   * Expected answers:
   *   code 200 : V1Status (OK)
   *   code 202 : V1Status (Accepted)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param name name of the Event
   * @param namespace object name and auth scope, such as for teams and projects
   * @param pretty If 'true', then the output is pretty printed.
   * @param dryRun When present, indicates that modifications should not be persisted. An invalid or unrecognized dryRun directive will result in an error response and no further processing of the request. Valid values are: - All: all dry run stages will be processed
   * @param gracePeriodSeconds The duration in seconds before the object should be deleted. Value must be non-negative integer. The value zero indicates delete immediately. If this value is nil, the default grace period for the specified type will be used. Defaults to a per object value if not specified. zero means delete immediately.
   * @param orphanDependents Deprecated: please use the PropagationPolicy, this field will be deprecated in 1.7. Should the dependent objects be orphaned. If true/false, the \"orphan\" finalizer will be added to/removed from the object's finalizers list. Either this field or PropagationPolicy may be set, but not both.
   * @param propagationPolicy Whether and how garbage collection will be performed. Either this field or OrphanDependents may be set, but not both. The default policy is decided by the existing finalizer set in the metadata.finalizers and the resource-specific default policy. Acceptable values are: 'Orphan' - orphan the dependents; 'Background' - allow the garbage collector to delete the dependents in the background; 'Foreground' - a cascading policy that deletes all dependents in the foreground.
   * @param body 
   */
  def deleteNamespacedEvent(name: String, namespace: String, pretty: Option[String] = None, dryRun: Option[String] = None, gracePeriodSeconds: Option[Int] = None, orphanDependents: Option[Boolean] = None, propagationPolicy: Option[String] = None, body: Option[V1DeleteOptions] = None)(implicit apiKey: ApiKeyValue
): ApiRequest[V1Status] =
    ApiRequest[V1Status](ApiMethods.DELETE, baseUrl, "/apis/events.k8s.io/v1beta1/namespaces/{namespace}/events/{name}", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withBody(body)
      .withQueryParam("pretty", pretty)
      .withQueryParam("dryRun", dryRun)
      .withQueryParam("gracePeriodSeconds", gracePeriodSeconds)
      .withQueryParam("orphanDependents", orphanDependents)
      .withQueryParam("propagationPolicy", propagationPolicy)
      .withPathParam("name", name)
      .withPathParam("namespace", namespace)
      .withSuccessResponse[V1Status](200)
      .withErrorResponse[V1Status](202)
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
    ApiRequest[V1APIResourceList](ApiMethods.GET, baseUrl, "/apis/events.k8s.io/v1beta1/", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withSuccessResponse[V1APIResourceList](200)
      .withErrorResponse[Unit](401)
      

  /**
   * list or watch objects of kind Event
   * 
   * Expected answers:
   *   code 200 : V1beta1EventList (OK)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param allowWatchBookmarks allowWatchBookmarks requests watch events with type \"BOOKMARK\". Servers that do not implement bookmarks may ignore this flag and bookmarks are sent at the server's discretion. Clients should not assume bookmarks are returned at any specific interval, nor may they assume the server will send any BOOKMARK event during a session. If this is not a watch, this field is ignored. If the feature gate WatchBookmarks is not enabled in apiserver, this field is ignored.  This field is alpha and can be changed or removed without notice.
   * @param continue The continue option should be set when retrieving more results from the server. Since this value is server defined, clients may only use the continue value from a previous query result with identical query parameters (except for the value of continue) and the server may reject a continue value it does not recognize. If the specified continue value is no longer valid whether due to expiration (generally five to fifteen minutes) or a configuration change on the server, the server will respond with a 410 ResourceExpired error together with a continue token. If the client needs a consistent list, it must restart their list without the continue field. Otherwise, the client may send another list request with the token received with the 410 error, the server will respond with a list starting from the next key, but from the latest snapshot, which is inconsistent from the previous list results - objects that are created, modified, or deleted after the first list request will be included in the response, as long as their keys are after the \"next key\".  This field is not supported when watch is true. Clients may start a watch from the last resourceVersion value returned by the server and not miss any modifications.
   * @param fieldSelector A selector to restrict the list of returned objects by their fields. Defaults to everything.
   * @param labelSelector A selector to restrict the list of returned objects by their labels. Defaults to everything.
   * @param limit limit is a maximum number of responses to return for a list call. If more items exist, the server will set the `continue` field on the list metadata to a value that can be used with the same initial query to retrieve the next set of results. Setting a limit may return fewer than the requested amount of items (up to zero items) in the event all requested objects are filtered out and clients should only use the presence of the continue field to determine whether more results are available. Servers may choose not to support the limit argument and will return all of the available results. If limit is specified and the continue field is empty, clients may assume that no more results are available. This field is not supported if watch is true.  The server guarantees that the objects returned when using continue will be identical to issuing a single list call without a limit - that is, no objects created, modified, or deleted after the first request is issued will be included in any subsequent continued requests. This is sometimes referred to as a consistent snapshot, and ensures that a client that is using limit to receive smaller chunks of a very large result can ensure they see all possible objects. If objects are updated during a chunked list the version of the object that was present at the time the first list result was calculated is returned.
   * @param pretty If 'true', then the output is pretty printed.
   * @param resourceVersion When specified with a watch call, shows changes that occur after that particular version of a resource. Defaults to changes from the beginning of history. When specified for list: - if unset, then the result is returned from remote storage based on quorum-read flag; - if it's 0, then we simply return what we currently have in cache, no guarantee; - if set to non zero, then the result is at least as fresh as given rv.
   * @param timeoutSeconds Timeout for the list/watch call. This limits the duration of the call, regardless of any activity or inactivity.
   * @param watch Watch for changes to the described resources and return them as a stream of add, update, and remove notifications. Specify resourceVersion.
   */
  def listEventForAllNamespaces(allowWatchBookmarks: Option[Boolean] = None, continue: Option[String] = None, fieldSelector: Option[String] = None, labelSelector: Option[String] = None, limit: Option[Int] = None, pretty: Option[String] = None, resourceVersion: Option[String] = None, timeoutSeconds: Option[Int] = None, watch: Option[Boolean] = None)(implicit apiKey: ApiKeyValue
): ApiRequest[V1beta1EventList] =
    ApiRequest[V1beta1EventList](ApiMethods.GET, baseUrl, "/apis/events.k8s.io/v1beta1/events", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withQueryParam("allowWatchBookmarks", allowWatchBookmarks)
      .withQueryParam("continue", continue)
      .withQueryParam("fieldSelector", fieldSelector)
      .withQueryParam("labelSelector", labelSelector)
      .withQueryParam("limit", limit)
      .withQueryParam("pretty", pretty)
      .withQueryParam("resourceVersion", resourceVersion)
      .withQueryParam("timeoutSeconds", timeoutSeconds)
      .withQueryParam("watch", watch)
      .withSuccessResponse[V1beta1EventList](200)
      .withErrorResponse[Unit](401)
      

  /**
   * list or watch objects of kind Event
   * 
   * Expected answers:
   *   code 200 : V1beta1EventList (OK)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param namespace object name and auth scope, such as for teams and projects
   * @param pretty If 'true', then the output is pretty printed.
   * @param allowWatchBookmarks allowWatchBookmarks requests watch events with type \"BOOKMARK\". Servers that do not implement bookmarks may ignore this flag and bookmarks are sent at the server's discretion. Clients should not assume bookmarks are returned at any specific interval, nor may they assume the server will send any BOOKMARK event during a session. If this is not a watch, this field is ignored. If the feature gate WatchBookmarks is not enabled in apiserver, this field is ignored.  This field is alpha and can be changed or removed without notice.
   * @param continue The continue option should be set when retrieving more results from the server. Since this value is server defined, clients may only use the continue value from a previous query result with identical query parameters (except for the value of continue) and the server may reject a continue value it does not recognize. If the specified continue value is no longer valid whether due to expiration (generally five to fifteen minutes) or a configuration change on the server, the server will respond with a 410 ResourceExpired error together with a continue token. If the client needs a consistent list, it must restart their list without the continue field. Otherwise, the client may send another list request with the token received with the 410 error, the server will respond with a list starting from the next key, but from the latest snapshot, which is inconsistent from the previous list results - objects that are created, modified, or deleted after the first list request will be included in the response, as long as their keys are after the \"next key\".  This field is not supported when watch is true. Clients may start a watch from the last resourceVersion value returned by the server and not miss any modifications.
   * @param fieldSelector A selector to restrict the list of returned objects by their fields. Defaults to everything.
   * @param labelSelector A selector to restrict the list of returned objects by their labels. Defaults to everything.
   * @param limit limit is a maximum number of responses to return for a list call. If more items exist, the server will set the `continue` field on the list metadata to a value that can be used with the same initial query to retrieve the next set of results. Setting a limit may return fewer than the requested amount of items (up to zero items) in the event all requested objects are filtered out and clients should only use the presence of the continue field to determine whether more results are available. Servers may choose not to support the limit argument and will return all of the available results. If limit is specified and the continue field is empty, clients may assume that no more results are available. This field is not supported if watch is true.  The server guarantees that the objects returned when using continue will be identical to issuing a single list call without a limit - that is, no objects created, modified, or deleted after the first request is issued will be included in any subsequent continued requests. This is sometimes referred to as a consistent snapshot, and ensures that a client that is using limit to receive smaller chunks of a very large result can ensure they see all possible objects. If objects are updated during a chunked list the version of the object that was present at the time the first list result was calculated is returned.
   * @param resourceVersion When specified with a watch call, shows changes that occur after that particular version of a resource. Defaults to changes from the beginning of history. When specified for list: - if unset, then the result is returned from remote storage based on quorum-read flag; - if it's 0, then we simply return what we currently have in cache, no guarantee; - if set to non zero, then the result is at least as fresh as given rv.
   * @param timeoutSeconds Timeout for the list/watch call. This limits the duration of the call, regardless of any activity or inactivity.
   * @param watch Watch for changes to the described resources and return them as a stream of add, update, and remove notifications. Specify resourceVersion.
   */
  def listNamespacedEvent(namespace: String, pretty: Option[String] = None, allowWatchBookmarks: Option[Boolean] = None, continue: Option[String] = None, fieldSelector: Option[String] = None, labelSelector: Option[String] = None, limit: Option[Int] = None, resourceVersion: Option[String] = None, timeoutSeconds: Option[Int] = None, watch: Option[Boolean] = None)(implicit apiKey: ApiKeyValue
): ApiRequest[V1beta1EventList] =
    ApiRequest[V1beta1EventList](ApiMethods.GET, baseUrl, "/apis/events.k8s.io/v1beta1/namespaces/{namespace}/events", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withQueryParam("pretty", pretty)
      .withQueryParam("allowWatchBookmarks", allowWatchBookmarks)
      .withQueryParam("continue", continue)
      .withQueryParam("fieldSelector", fieldSelector)
      .withQueryParam("labelSelector", labelSelector)
      .withQueryParam("limit", limit)
      .withQueryParam("resourceVersion", resourceVersion)
      .withQueryParam("timeoutSeconds", timeoutSeconds)
      .withQueryParam("watch", watch)
      .withPathParam("namespace", namespace)
      .withSuccessResponse[V1beta1EventList](200)
      .withErrorResponse[Unit](401)
      

  /**
   * partially update the specified Event
   * 
   * Expected answers:
   *   code 200 : V1beta1Event (OK)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param name name of the Event
   * @param namespace object name and auth scope, such as for teams and projects
   * @param pretty If 'true', then the output is pretty printed.
   * @param dryRun When present, indicates that modifications should not be persisted. An invalid or unrecognized dryRun directive will result in an error response and no further processing of the request. Valid values are: - All: all dry run stages will be processed
   * @param fieldManager fieldManager is a name associated with the actor or entity that is making these changes. The value must be less than or 128 characters long, and only contain printable characters, as defined by https://golang.org/pkg/unicode/#IsPrint. This field is required for apply requests (application/apply-patch) but optional for non-apply patch types (JsonPatch, MergePatch, StrategicMergePatch).
   * @param force Force is going to \"force\" Apply requests. It means user will re-acquire conflicting fields owned by other people. Force flag must be unset for non-apply patch requests.
   * @param body 
   */
  def patchNamespacedEvent(name: String, namespace: String, pretty: Option[String] = None, dryRun: Option[String] = None, fieldManager: Option[String] = None, force: Option[Boolean] = None, body: V1Patch)(implicit apiKey: ApiKeyValue
): ApiRequest[V1beta1Event] =
    ApiRequest[V1beta1Event](ApiMethods.PATCH, baseUrl, "/apis/events.k8s.io/v1beta1/namespaces/{namespace}/events/{name}", "application/json-patch+json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withBody(body)
      .withQueryParam("pretty", pretty)
      .withQueryParam("dryRun", dryRun)
      .withQueryParam("fieldManager", fieldManager)
      .withQueryParam("force", force)
      .withPathParam("name", name)
      .withPathParam("namespace", namespace)
      .withSuccessResponse[V1beta1Event](200)
      .withErrorResponse[Unit](401)
      

  /**
   * read the specified Event
   * 
   * Expected answers:
   *   code 200 : V1beta1Event (OK)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param name name of the Event
   * @param namespace object name and auth scope, such as for teams and projects
   * @param pretty If 'true', then the output is pretty printed.
   * @param exact Should the export be exact.  Exact export maintains cluster-specific fields like 'Namespace'. Deprecated. Planned for removal in 1.18.
   * @param export Should this value be exported.  Export strips fields that a user can not specify. Deprecated. Planned for removal in 1.18.
   */
  def readNamespacedEvent(name: String, namespace: String, pretty: Option[String] = None, exact: Option[Boolean] = None, export: Option[Boolean] = None)(implicit apiKey: ApiKeyValue
): ApiRequest[V1beta1Event] =
    ApiRequest[V1beta1Event](ApiMethods.GET, baseUrl, "/apis/events.k8s.io/v1beta1/namespaces/{namespace}/events/{name}", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withQueryParam("pretty", pretty)
      .withQueryParam("exact", exact)
      .withQueryParam("export", export)
      .withPathParam("name", name)
      .withPathParam("namespace", namespace)
      .withSuccessResponse[V1beta1Event](200)
      .withErrorResponse[Unit](401)
      

  /**
   * replace the specified Event
   * 
   * Expected answers:
   *   code 200 : V1beta1Event (OK)
   *   code 201 : V1beta1Event (Created)
   *   code 401 :  (Unauthorized)
   * 
   * Available security schemes:
   *   BearerToken (apiKey)
   * 
   * @param name name of the Event
   * @param namespace object name and auth scope, such as for teams and projects
   * @param pretty If 'true', then the output is pretty printed.
   * @param dryRun When present, indicates that modifications should not be persisted. An invalid or unrecognized dryRun directive will result in an error response and no further processing of the request. Valid values are: - All: all dry run stages will be processed
   * @param fieldManager fieldManager is a name associated with the actor or entity that is making these changes. The value must be less than or 128 characters long, and only contain printable characters, as defined by https://golang.org/pkg/unicode/#IsPrint.
   * @param body 
   */
  def replaceNamespacedEvent(name: String, namespace: String, pretty: Option[String] = None, dryRun: Option[String] = None, fieldManager: Option[String] = None, body: V1beta1Event)(implicit apiKey: ApiKeyValue
): ApiRequest[V1beta1Event] =
    ApiRequest[V1beta1Event](ApiMethods.PUT, baseUrl, "/apis/events.k8s.io/v1beta1/namespaces/{namespace}/events/{name}", "application/json")
      .withApiKey(apiKey, "authorization", HEADER)
      .withBody(body)
      .withQueryParam("pretty", pretty)
      .withQueryParam("dryRun", dryRun)
      .withQueryParam("fieldManager", fieldManager)
      .withPathParam("name", name)
      .withPathParam("namespace", namespace)
      .withSuccessResponse[V1beta1Event](200)
      .withErrorResponse[V1beta1Event](201)
      .withErrorResponse[Unit](401)
      



}

