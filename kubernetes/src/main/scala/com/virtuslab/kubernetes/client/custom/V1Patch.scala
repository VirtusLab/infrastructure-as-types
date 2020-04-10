package com.virtuslab.kubernetes.client.custom

import java.io.IOException
import com.google.gson.TypeAdapter
import com.google.gson.stream.{JsonReader, JsonWriter}

object V1Patch {
  val PATCH_FORMAT_JSON_PATCH = "application/json-patch+json"
  val PATCH_FORMAT_JSON_MERGE_PATCH = "application/merge-patch+json"
  val PATCH_FORMAT_STRATEGIC_MERGE_PATCH = "application/strategic-merge-patch+json"
  val PATCH_FORMAT_APPLY_YAML = "application/apply-patch+yaml"

  class V1PatchAdapter extends TypeAdapter[V1Patch] {
    @throws[IOException]
    override def write(jsonWriter: JsonWriter, patch: V1Patch): Unit = {
      jsonWriter.jsonValue(patch.getValue)
    }

    @throws[IOException]
    override def read(jsonReader: JsonReader) =
      throw new UnsupportedOperationException("deserializing patch data is not supported")
  }

}

class V1Patch(val value: String) {
  def getValue: String = value
}
