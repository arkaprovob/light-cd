package org.prototype.type;

import java.util.Map;

public class ExtendedPayload {
    Payload payload;
    Map<String, String> templateParameters;

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public Map<String, String> getTemplateParameters() {
        return templateParameters;
    }

    public void setTemplateParameters(Map<String, String> templateParameters) {
        this.templateParameters = templateParameters;
    }

    @Override
    public String toString() {
        return "{"
                + "\"payload\":" + payload
                + ", \"templateParameters\":" + templateParameters
                + "}";
    }
}
