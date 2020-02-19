package com.virtuslab.internal

import cats.data.NonEmptyList
import cats.syntax.option._
import com.virtuslab.internal.EnvVar.ValueFromRef
import com.virtuslab.internal.ValueFrom.ConfigMapKeyRefValue
import com.virtuslab.internal.ValueRef.ConfigMapKeyRef

case class ConfigMap(name: String, values: Map[String, String]) {
  def foo(key: String): EnvVar = {
    if (!values.contains(key)) {
      throw new IllegalArgumentException(s"Key $key does not exist in configMap $name")
    }
    val name1 = ConfigMapKeyRefValue(key, name, optional = true)
    val ref = ConfigMapKeyRef(name1)
    ValueFromRef(name, ref)
  }
}

case class Container protected (
    image: Image,
    imagePullPolicy: Option[ImagePullPolicy] = None,
    env: List[EnvVar] = Nil,
    ports: List[Port] = Nil)
object Container {
  def apply(
      image: Image,
      imagePullPolicy: ImagePullPolicy,
      env: List[EnvVar] = Nil
    ): Container = {
    new Container(image = image, imagePullPolicy = imagePullPolicy.some)
  }

  def apply(image: Image, env: List[EnvVar] = Nil): Container = {
    new Container(image)
  }
}
case class PodSpec(containers: NonEmptyList[Container])
case class Pod(spec: PodSpec)

object Foo {
  val pod1 = {
    val configMap: EnvVar = ConfigMap("asd", Map("vodka" -> "bols")).foo("vodka")
    val env: EnvVar = EnvVar("sda", "dasda")
    val fooImage: Image = Image.byTag("foo", "bar")
    val value: Container = Container(fooImage, ImagePullPolicy.Always, env :: configMap :: Nil)
    val podSpec: PodSpec = PodSpec(NonEmptyList.of(value))

    Pod(podSpec)
  }

  val pod2 = {
    val configMap: EnvVar = ConfigMap("xd", Map("meme" -> "doggo")).foo("meme")
    val env: EnvVar = EnvVar("abcd", "efgh")
    val fooImage: Image = Image.byTag("boo", "bak")
    val value: Container = Container(fooImage, ImagePullPolicy.IfNotPresent, env :: configMap :: Nil)
    val podSpec: PodSpec = PodSpec(NonEmptyList.of(value))

    Pod(podSpec)
  }

  //pod1.viaHttp(pod2)

}
