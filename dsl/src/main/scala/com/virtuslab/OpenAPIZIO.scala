package com.virtuslab

import java.net.Socket
import java.security.cert.X509Certificate

import com.virtuslab.kubernetes.client.openapi.api.CoreV1Api
import com.virtuslab.kubernetes.client.openapi.core._
import com.virtuslab.kubernetes.client.openapi.model
import com.virtuslab.kubernetes.client.openapi.model.Namespace
import io.netty.handler.ssl.SslContextBuilder
import javax.net.ssl._
import skuber.api
import skuber.api.client._
import skuber.api.security.SecurityHelper
import skuber.api.{ client, Configuration }
import sttp.client.asynchttpclient.zio._
import zio.console.Console
import zio.{ ZIO, console, _ }

object OpenAPIZIO extends App {
  private val kubeconfig: Configuration = api.Configuration.parseKubeconfigFile().get
  private val ourContext: Context = kubeconfig.contexts("gke_infrastructure-as-types_us-central1-a_standard-cluster-1")
  private val ourUser: client.AuthInfo = kubeconfig.users("gke_infrastructure-as-types_us-central1-a_standard-cluster-1")

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val server = ourContext.cluster.server
    val auth: Credentials = ourUser match {
      case NoAuth | _: CertAuth      => NoCredentials // No auth header or TLS certificate
      case BasicAuth(user, password) => BasicCredentials(user, password)
      case auth: AccessTokenAuth     => BearerToken(auth.accessToken)
    }
    // https://kubernetes.io/docs/reference/access-authn-authz/authentication/#authentication-strategies
    implicit val a: ApiKeyValue = auth match {
      case BearerToken(token) => ApiKeyValue(s"Bearer $token")
      case _                  => ???
    }

    val skipTLSVerify = ourContext.cluster.insecureSkipTLSVerify
    val clusterCA = ourContext.cluster.certificateAuthority

    val ns = Namespace(
      apiVersion = Some("v1"),
      kind = Some("Namespace"),
      metadata = Some(
        model.ObjectMeta(
          name = Some("test")
        )
      )
    )

    implicit val s: SttpSerializer = new SttpSerializer

    val core: CoreV1Api = CoreV1Api(server)
    val request = core.createNamespace(
      dryRun = Some("All"),
      body = ns
    )

    val sendAndPrint: ZIO[Console with SttpClient, Throwable, Unit] = for {
      response <- SttpClient.send(request)
      _ <- console.putStrLn(s"Got response code: ${response.code}")
      _ <- console.putStrLn(response.body.toString)
    } yield ()

    sendAndPrint
      .provideCustomLayer(AsyncHttpClientZioBackend.layerUsingConfigBuilder {
        val sslContext = buildSSLContext(skipTLSVerify, clusterCA, ourUser)
        builder => builder.setSslContext(sslContext.build())
      })
      .fold(_ => 1, _ => 0)
  }

  def buildSSLContext(
      skipTLSVerify: Boolean,
      clusterCertConfig: Option[PathOrData],
      authInfo: AuthInfo
    ): SslContextBuilder = {
    val (trustManagers, keyManagers) = getManagers(skipTLSVerify, clusterCertConfig, authInfo)
    val sslContextBuilder = SslContextBuilder.forClient
//      .ciphers(util.Arrays.asList(
//        sslFactory.getSslContext.getDefaultSSLParameters.getCipherSuites),
//        SupportedCipherSuiteFilter.INSTANCE)
//      .protocols(sslFactory.getSslContext.getDefaultSSLParameters.getProtocols)
    if (trustManagers.isDefined) trustManagers.get.foreach(sslContextBuilder.trustManager)
    if (keyManagers.isDefined) keyManagers.get.map(sslContextBuilder.keyManager)
    sslContextBuilder
  }

  private def getManagers(
      skipTLSVerify: Boolean,
      clusterCertConfig: Option[PathOrData],
      authInfo: AuthInfo
    ): (Option[Array[TrustManager]], Option[Array[KeyManager]]) = {
    val trustManagers = getTrustManagers(skipTLSVerify, clusterCertConfig)
    val keyManagers = getKeyManagers(authInfo)
    (trustManagers, keyManagers)
  }

  private def getTrustManagers(skipTLSVerify: Boolean, serverCertConfig: Option[PathOrData]): Option[Array[TrustManager]] =
    if (skipTLSVerify)
      Some(skipTLSTrustManagers)
    else
      serverCertConfig map { certPathOrData =>
        val clusterServerCerts = SecurityHelper.getCertificates(certPathOrData)
        val trustStore = SecurityHelper.createTrustStore(clusterServerCerts)
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm);
        tmf.init(trustStore)
        tmf.getTrustManagers
      }

  private def getKeyManagers(authInfo: AuthInfo): Option[Array[KeyManager]] =
    authInfo match {
      case CertAuth(clientCert, clientKey, userName) =>
        val certs = SecurityHelper.getCertificates(clientCert)
        val key = SecurityHelper.getPrivateKey(clientKey)
        val keyStore = SecurityHelper.createKeyStore(userName.getOrElse("skuber"), certs, key) // ???
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
        kmf.init(keyStore, "changeit".toCharArray) // ???
        Some(kmf.getKeyManagers)
      case _ => None
    }

  // This trust manager supports the InsecureSkipTLSVerify flag in kubeconfig files -
  // it always trusts the server i.e. skips verifying the server cert for a TLS connection
  object InsecureSkipTLSVerifyTrustManager extends X509ExtendedTrustManager {
    def getAcceptedIssuers = Array.empty[X509Certificate]
    def checkClientTrusted(certs: Array[X509Certificate], authType: String): Unit = {}
    def checkServerTrusted(certs: Array[X509Certificate], authType: String): Unit = {}
    def checkClientTrusted(
        certs: Array[X509Certificate],
        s: String,
        socket: Socket
      ): Unit = {}
    def checkClientTrusted(
        certs: Array[X509Certificate],
        s: String,
        sslEngine: SSLEngine
      ): Unit = {}
    def checkServerTrusted(
        certs: Array[X509Certificate],
        s: String,
        socket: Socket
      ): Unit = {}
    def checkServerTrusted(
        certs: Array[X509Certificate],
        s: String,
        sslEngine: SSLEngine
      ): Unit = {}
  }

  val skipTLSTrustManagers: Array[TrustManager] = Array[TrustManager](InsecureSkipTLSVerifyTrustManager)
}
