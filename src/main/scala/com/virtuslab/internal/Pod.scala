package com.virtuslab.internal

import cats.data.NonEmptyList
import cats.syntax.option._
import com.virtuslab.dsl.{ Namespace, Namespaced, Resource }
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
    name: String,
    image: Image,
    imagePullPolicy: Option[ImagePullPolicy] = None,
    env: List[EnvVar] = Nil,
    ports: List[Port] = Nil) {

  // TODO factor this out
  def toSkuber: skuber.Container = {
    skuber.Container(
      name = name,
      image = image.toString
    )
  }
}
object Container {
  def apply(
      name: String,
      image: Image,
      imagePullPolicy: ImagePullPolicy
    ): Container = {
    new Container(
      name = name,
      image = image,
      imagePullPolicy = imagePullPolicy.some
    )
  }

  def apply(name: String, image: Image): Container = {
    new Container(name, image)
  }
}
case class PodSpec(containers: NonEmptyList[Container])
case class Pod(meta: ObjectMeta, spec: PodSpec) extends Resource {
  override def name: String = meta.name

  // TODO factor this out
  def toSkuber: skuber.Pod = {
    skuber.Pod(
      metadata = skuber.ObjectMeta(name = meta.name),
      spec = Some(
        skuber.Pod.Spec(
          containers = spec.containers.map(_.toSkuber).toList
        )
      )
    )
  }
}

object Foo {
  val pod1 = {
    val configMap: EnvVar = ConfigMap("asd", Map("vodka" -> "bols")).foo("vodka")
    val env: EnvVar = EnvVar("sda", "dasda")
    val fooImage: Image = Image.byTag("foo", "bar")
    val value: Container = Container("foo", fooImage, ImagePullPolicy.Always)
    val podSpec: PodSpec = PodSpec(NonEmptyList.of(value))
    val meta: ObjectMeta = ObjectMeta("test")

    Pod(meta, podSpec)
  }

  val pod2 = {
    val configMap: EnvVar = ConfigMap("xd", Map("meme" -> "doggo")).foo("meme")
    val env: EnvVar = EnvVar("abcd", "efgh")
    val fooImage: Image = Image.byTag("boo", "bak")
    val value: Container = Container("foo", fooImage, ImagePullPolicy.IfNotPresent)
    val podSpec: PodSpec = PodSpec(NonEmptyList.of(value))
    val meta: ObjectMeta = ObjectMeta("test")

    Pod(meta, podSpec)
  }

  //pod1.viaHttp(pod2)

}
