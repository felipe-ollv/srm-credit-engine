package com.credit.engine.srm.pricing.internal.adapter.in.web;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

final class StrictStringDeserializer extends StdDeserializer<String> {

    StrictStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        if (parser.hasToken(JsonToken.VALUE_STRING)) {
            return parser.getString();
        }

        return context.reportInputMismatch(
                String.class,
                "faceValue must be represented as a JSON string"
        );
    }
}
