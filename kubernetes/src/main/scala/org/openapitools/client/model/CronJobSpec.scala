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
   * CronJobSpec describes how the job execution will look like and when it will actually run.
   */
case class CronJobSpec(
  /* Specifies how to treat concurrent executions of a Job. Valid values are: - \"Allow\" (default): allows CronJobs to run concurrently; - \"Forbid\": forbids concurrent runs, skipping next run if previous run hasn't finished yet; - \"Replace\": cancels currently running job and replaces it with a new one */
  concurrencyPolicy: Option[String] = None,
  /* The number of failed finished jobs to retain. This is a pointer to distinguish between explicit zero and not specified. */
  failedJobsHistoryLimit: Option[Int] = None,
  jobTemplate: JobTemplateSpec,
  /* The schedule in Cron format, see https://en.wikipedia.org/wiki/Cron. */
  schedule: String,
  /* Optional deadline in seconds for starting the job if it misses scheduled time for any reason.  Missed jobs executions will be counted as failed ones. */
  startingDeadlineSeconds: Option[Long] = None,
  /* The number of successful finished jobs to retain. This is a pointer to distinguish between explicit zero and not specified. */
  successfulJobsHistoryLimit: Option[Int] = None,
  /* This flag tells the controller to suspend subsequent executions, it does not apply to already started executions.  Defaults to false. */
  suspend: Option[Boolean] = None
) extends ApiModel


