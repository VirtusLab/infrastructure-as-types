package com.virtuslab.kubernetes.client.custom

import java.lang.reflect.Type

import com.google.gson.{JsonElement, JsonSerializationContext, _}
import com.virtuslab.kubernetes.client.custom.IntOrString.IntOrStringAdapter
import com.virtuslab.kubernetes.client.custom.Quantity.QuantityAdapter
import com.virtuslab.kubernetes.client.custom.V1Patch.V1PatchAdapter

object JSON {
  lazy val gson: Gson = new GsonBuilder()
    .registerTypeHierarchyAdapter(classOf[Seq[Any]], new ListSerializer)
    .registerTypeHierarchyAdapter(classOf[Map[Any, Any]], new MapSerializer)
    .registerTypeHierarchyAdapter(classOf[Option[Any]], new OptionSerializer)
    .registerTypeAdapter(classOf[IntOrString], new IntOrStringAdapter)
    .registerTypeAdapter(classOf[Quantity], new QuantityAdapter)
    .registerTypeAdapter(classOf[V1Patch], new V1PatchAdapter)
    .create()

  class ListSerializer extends JsonSerializer[Seq[Any]] {
    override def serialize(src: Seq[Any], typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
      import scala.collection.JavaConverters._
      context.serialize(src.toList.asJava)
    }
  }

  class MapSerializer extends JsonSerializer[Map[Any, Any]] {
    override def serialize(src: Map[Any, Any], typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
      import scala.collection.JavaConverters._
      context.serialize(src.asJava)
    }
  }

  class OptionSerializer extends JsonSerializer[Option[_]] with JsonDeserializer[Option[_]] {
    override def serialize(src: Option[_], typeOfSrc: Type, context: JsonSerializationContext): JsonElement = {
      src match {
        case None => JsonNull.INSTANCE
        case Some(value) => context.serialize(value)
      }
    }

    override def deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Option[_] = {
      json match {
        case null => None
        case _ if json.isJsonNull => None
        case _ => Some(context.deserialize(json, innerType(typeOfT)))
      }
    }

    import java.lang.reflect.{ParameterizedType, Type}

    private def innerType(outerType: Type) = outerType match {
      case p: ParameterizedType => p.getActualTypeArguments()(0)
      case _ => throw new UnsupportedOperationException(outerType.toString)
    }

  }

}
