package com.virtuslab.kubernetes.client.custom

import java.io.IOException
import com.google.gson.TypeAdapter
import com.google.gson.stream.{JsonReader, JsonToken, JsonWriter}

case class IntOrString(value: Either[Int, String]) {
  def isInt: Boolean = value.isLeft
  def int: Int = if (!isInt) throw new IllegalStateException("Not an integer") else value.left.get
  def string: String = if (isInt) throw new IllegalStateException("Not a string") else value.right.get
}

// Adapted from Skuber and Kubernetes Java client
object IntOrString {
  def apply(int: Int): IntOrString = new IntOrString(Left(int))
  def apply(string: String): IntOrString = new IntOrString(Right(string))

  class IntOrStringAdapter extends TypeAdapter[IntOrString] {
    @throws[IOException]
    def write(jsonWriter: JsonWriter, intOrString: IntOrString): Unit = {
      if (intOrString.isInt) jsonWriter.value(intOrString.int)
      else jsonWriter.value(intOrString.string)
    }

    @throws[IOException]
    def read(jsonReader: JsonReader): IntOrString = {
      val nextToken = jsonReader.peek
      if (nextToken eq JsonToken.NUMBER) IntOrString(jsonReader.nextInt)
      else if (nextToken eq JsonToken.STRING) IntOrString(jsonReader.nextString)
      else throw new IllegalStateException("Could not deserialize to IntOrString. Was: " + nextToken)
    }
  }
}
