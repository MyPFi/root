package com.andreidiego.mpfi.stocks.adapter.files.watchers.actors.poc.brokeragenotewatcher

import org.slf4j.LoggerFactory
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind
import databind.{DeserializationContext, JsonDeserializer, JsonNode, SerializerProvider}
import databind.ser.std.StdSerializer
import BrokerageNoteWatcher.{RequestStatus, RequestType}

private val log = LoggerFactory.getLogger("BrokerageNoteWatcher-EnumSerializer")

class RequestTypeSerializer extends StdSerializer[RequestType](classOf[RequestType]):
  override def serialize(value: RequestType, gen: JsonGenerator, provider: SerializerProvider): Unit =
    log.debug("RequestTypeSerializer => About to serialize {}", value)
    gen.writeString(value.toString)

class RequestTypeDeserializer extends JsonDeserializer[RequestType]:
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): RequestType =
    val enumName = p.getValueAsString().capitalize
    log.debug("RequestTypeDeserializer => About to deserialize {}", enumName)
    RequestType.valueOf(enumName)

class RequestStatusSerializer extends StdSerializer[RequestStatus](classOf[RequestStatus]):
  override def serialize(value: RequestStatus, gen: JsonGenerator, provider: SerializerProvider): Unit =
    log.debug("RequestStatusSerializer => About to serialize {}", value)
    gen.writeStartObject()
    gen.writeStringField("status", value.toString)
    value match
      case _: RequestStatus.Failed ⇒
        gen.writeObjectField("error", value.asInstanceOf[RequestStatus.Failed].ex)
      case _ ⇒
    gen.writeEndObject()

class RequestStatusDeserializer extends JsonDeserializer[RequestStatus]:
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): RequestStatus =
    val node: JsonNode = p.getCodec.readTree(p)
    log.debug("RequestStatusDeserializer => About to deserialize {}", node)
    Option(node.get("error")) match
      case Some(value) ⇒
        val ex = p.getCodec.treeToValue(value, classOf[Throwable])
        RequestStatus.Failed(ex)
      case None ⇒ RequestStatus.fromString(node.get("status").asText()).get