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

case class V1ISCSIPersistentVolumeSource (
  /* whether support iSCSI Discovery CHAP authentication */
  chapAuthDiscovery: Option[Boolean] = None,
  /* whether support iSCSI Session CHAP authentication */
  chapAuthSession: Option[Boolean] = None,
  /* Filesystem type of the volume that you want to mount. Tip: Ensure that the filesystem type is supported by the host operating system. Examples: \"ext4\", \"xfs\", \"ntfs\". Implicitly inferred to be \"ext4\" if unspecified. More info: https://kubernetes.io/docs/concepts/storage/volumes#iscsi */
  fsType: Option[String] = None,
  /* Custom iSCSI Initiator Name. If initiatorName is specified with iscsiInterface simultaneously, new iSCSI interface <target portal>:<volume name> will be created for the connection. */
  initiatorName: Option[String] = None,
  /* Target iSCSI Qualified Name. */
  iqn: String,
  /* iSCSI Interface Name that uses an iSCSI transport. Defaults to 'default' (tcp). */
  iscsiInterface: Option[String] = None,
  /* iSCSI Target Lun number. */
  lun: Int,
  /* iSCSI Target Portal List. The Portal is either an IP or ip_addr:port if the port is other than default (typically TCP ports 860 and 3260). */
  portals: Option[Seq[String]] = None,
  /* ReadOnly here will force the ReadOnly setting in VolumeMounts. Defaults to false. */
  readOnly: Option[Boolean] = None,
  secretRef: Option[V1SecretReference] = None,
  /* iSCSI Target Portal. The Portal is either an IP or ip_addr:port if the port is other than default (typically TCP ports 860 and 3260). */
  targetPortal: String
) extends ApiModel


