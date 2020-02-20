package com.virutslab

object Test2Main extends App {
  val client = ApiClient(KubeConfig(context = "gke_infrastructure-as-types_us-central1-a_standard-cluster-1"))

  import io.kubernetes.client.apis.CoreV1Api

  val api = new CoreV1Api()

  val list = api.listPodForAllNamespaces(null, null, null, null, null, null, null, null)

  import scala.collection.JavaConverters._

  for (item <- list.getItems.asScala) {
    println(item.getMetadata.getName)
  }
}

object ApiClient {

  import io.kubernetes.client.util.{ClientBuilder, KubeConfig => JavaKubeConfig}
  import io.kubernetes.client.{Configuration, ApiClient => JavaApiClient}

  def apply(kubeConfig: JavaKubeConfig): JavaApiClient = {
    val client: JavaApiClient =
      ClientBuilder
        .kubeconfig(KubeConfig(context = "gke_infrastructure-as-types_us-central1-a_standard-cluster-1"))
        .build()

    // set the global default api-client to the one from above
    Configuration.setDefaultApiClient(client)

    client
  }
}

import io.kubernetes.client.util.{KubeConfig => JavaKubeConfig}

object KubeConfig {
  {
    JavaKubeConfig.registerAuthenticator(new GCPAuthenticator())
  }

  import java.io.{File, FileReader}

  import io.kubernetes.client.util.KubeConfig.{ENV_HOME, KUBECONFIG, KUBEDIR}

  def apply(context: String): JavaKubeConfig = {
    val config: File = findConfigInHomeDir.get // FIXME
    val kubeConfig = JavaKubeConfig.loadKubeConfig(new FileReader(config))
    kubeConfig.setContext(context)
    kubeConfig
  }

  // copied form the Java library
  private def findConfigInHomeDir: Option[File] = {
    val homeDir = findHomeDir
    if (homeDir.isEmpty) {
      return None
    }
    if (homeDir != null) {
      val config = new File(new File(homeDir.get, KUBEDIR), KUBECONFIG)
      if (config.exists) return Some(config)
    }
    None
  }

  // copied form the Java library
  private def findHomeDir: Option[File] = {
    val envHome = System.getenv(ENV_HOME)
    if (envHome != null && envHome.length > 0) {
      val config = new File(envHome)
      if (config.exists) return Some(config)
    }
    if (System.getProperty("os.name").toLowerCase.startsWith("windows")) {
      val homeDrive = System.getenv("HOMEDRIVE")
      val homePath = System.getenv("HOMEPATH")
      if (homeDrive != null && homeDrive.length > 0 && homePath != null && homePath.length > 0) {
        val homeDir = new File(new File(homeDrive), homePath)
        if (homeDir.exists) return Some(homeDir)
      }
      val userProfile = System.getenv("USERPROFILE")
      if (userProfile != null && userProfile.length > 0) {
        val profileDir = new File(userProfile)
        if (profileDir.exists) return Some(profileDir)
      }
    }
    None
  }

  import io.kubernetes.client.util.authenticators.{GCPAuthenticator => JaveGCPAuthenticator}

  class GCPAuthenticator() extends JaveGCPAuthenticator {
    private val ACCESS_TOKEN = "access-token"
    private val EXPIRY = "expiry"
    private val CMD_PATH = "cmd-path"
    private val CMD_ARGS = "cmd-args"

    import java.util

    override def refresh(config: util.Map[String, AnyRef]): util.Map[String, AnyRef] = {
      // https://github.com/kubernetes/kubernetes/blob/master/staging/src/k8s.io/client-go/plugin/pkg/client/auth/gcp/gcp.go#L296

      val jsonStream = callCliHelper(config)

      import java.nio.charset.Charset
      import com.google.api.client.json.jackson2.JacksonFactory
      import com.google.api.client.json.{GenericJson, JsonObjectParser}

      val UTF_8: Charset = Charset.forName("UTF-8")
      val jsonFactory = JacksonFactory.getDefaultInstance
      val parser = new JsonObjectParser(jsonFactory)
      val contents = parser.parseAndClose(jsonStream, UTF_8, classOf[GenericJson])
      val token: util.Map[String, AnyRef] = contents.get("credential") match {
        case el: util.Map[String, AnyRef] => el
        case o => throw new RuntimeException("Unexpected object type: " + o.getClass)
      }

      config.put(ACCESS_TOKEN, token.get("access_token"))
      config.put(EXPIRY, token.get("token_expiry"))
      config
    }

    import java.time.Instant
    import java.util.Date

    def getExpiry(config: util.Map[String, AnyRef]): Instant = {
      config.get(EXPIRY) match {
        case date: Date => date.toInstant
        case instant: Instant => instant
        case str: String => Instant.parse(str)
        case o => throw new RuntimeException("Unexpected object type: " + o.getClass)
      }
    }

    def getCmdPath(config: util.Map[String, AnyRef]): String = {
      config.get(CMD_PATH) match {
        case path: String => path
        case o => throw new RuntimeException("Unexpected object type: " + o.getClass)
      }
    }

    def getCmdArgs(config: util.Map[String, AnyRef]): String = {
      config.get(CMD_ARGS) match {
        case args: String => args
        case o => throw new RuntimeException("Unexpected object type: " + o.getClass)
      }
    }

    import java.io.InputStream

    private def callCliHelper(config: util.Map[String, AnyRef]): InputStream = {
      import scala.collection.JavaConverters._
      val cmd = List(getCmdPath(config)) ++ getCmdArgs(config).split(' ')
      val process = new ProcessBuilder(cmd.asJava).start()
      process.getInputStream
    }
  }

}