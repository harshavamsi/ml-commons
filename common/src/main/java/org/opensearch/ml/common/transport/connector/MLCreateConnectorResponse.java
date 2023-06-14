/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.ml.common.transport.connector;

import lombok.Getter;
import org.opensearch.action.ActionResponse;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.ToXContentObject;
import org.opensearch.core.xcontent.XContentBuilder;

import java.io.IOException;

@Getter
public class MLCreateConnectorResponse extends ActionResponse implements ToXContentObject {
    public static final String Connector_ID_FIELD = "connector_id";
    public static final String STATUS_FIELD = "status";

    private String connectorId;
    private String status;

    public MLCreateConnectorResponse(StreamInput in) throws IOException {
        super(in);
        this.connectorId = in.readString();
        this.status = in.readString();
    }

    public MLCreateConnectorResponse(String taskId, String status) {
        this.connectorId = taskId;
        this.status= status;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeString(connectorId);
        out.writeString(status);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field(Connector_ID_FIELD, connectorId);
        builder.field(STATUS_FIELD, status);
        builder.endObject();
        return builder;
    }
}