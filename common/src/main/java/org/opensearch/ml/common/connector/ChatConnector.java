/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.ml.common.connector;

import com.jayway.jsonpath.JsonPath;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.StringSubstitutor;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.common.io.stream.StreamInput;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.ml.common.output.model.ModelTensor;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opensearch.ml.common.connector.ConnectorNames.CHAT_V1;
import static org.opensearch.ml.common.utils.StringUtils.gson;

@Log4j2
@NoArgsConstructor
@org.opensearch.ml.common.annotation.Connector(CHAT_V1)
public class ChatConnector extends HttpConnector {
    public static final String CONTENT_INDEX = "content_index";
    public static final String EMBEDDING_MODEL_ID_FIELD = "embedding_model_id";
    public static final String EMBEDDING_FIELD_FIELD = "embedding_field";
    public static final String CONTENT_FIELD_FIELD = "content_fields";
    public static final String CONTENT_DOC_SIZE_FIELD = "content_doc_size";
    private static final Integer DEFAULT_CONTENT_DOC_SIZE = 2;
    public static final String SESSION_INDEX_FIELD = "session_index";
    public static final String SESSION_ID_FIELD_FIELD = "session_id_field";
    public static final String SESSION_SIZE_FIELD = "session_size";
    private static final Integer DEFAULT_SESSION_SIZE = 2;

    private static final String DEFAULT_SEMANTIC_SEARCH_QUERY = "{\n" +
            "  \"query\": {\n" +
            "    \"neural\": {\n" +
            "      \"${parameters.embedding_field}\": {\n" +
            "        \"query_text\": \"${parameters.question}\",\n" +
            "        \"model_id\": \"${parameters.embedding_model_id}\",\n" +
            "        \"k\": 100\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"size\": \"${parameters.content_doc_size}\",\n" +
            "  \"_source\": [\n" +
            "    \"${parameters.content_fields}\"\n" +
            "  ]\n" +
            "}";

    public ChatConnector(String name, XContentParser parser) throws IOException {
        super(name, parser);
        validate();
    }

    public ChatConnector(StreamInput input) throws IOException {
        super(input);
        validate();
    }

    private void validate() {
        if (credential == null) {
            throw new IllegalArgumentException("Missing credential");
        }
    }

    public String createNeuralSearchQuery(Map<String, String> params) {
        Map<String, String> mergedParams = new HashMap<>();
        mergedParams.putAll(this.parameters);
        mergedParams.putAll(params);
        String searchTemplate = mergedParams.containsKey("search_query") ? mergedParams.get("search_query") : DEFAULT_SEMANTIC_SEARCH_QUERY;
        StringSubstitutor substitutor = new StringSubstitutor(mergedParams, "${parameters.", "}");
        String query = substitutor.replace(searchTemplate);
        return query;
    }

    @SuppressWarnings("unchecked")
    public <T> void parseResponse(T response, List<ModelTensor> modelTensors, boolean modelTensorJson) throws IOException {
        if (modelTensorJson) {
            String modelTensorJsonContent = (String) response;
            XContentParser parser = XContentType.JSON.xContent().createParser(NamedXContentRegistry.EMPTY, null, modelTensorJsonContent);
            parser.nextToken();
            if (XContentParser.Token.START_ARRAY == parser.currentToken()) {
                while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                    ModelTensor modelTensor = ModelTensor.parser(parser);
                    modelTensors.add(modelTensor);
                }
            } else {
                ModelTensor modelTensor = ModelTensor.parser(parser);
                modelTensors.add(modelTensor);
            }
            return;
        }
        if (responseFilter == null) {
            Map<String, Object> data = gson.fromJson((String) response, Map.class);
            modelTensors.add(ModelTensor.builder().name("response").dataAsMap(data).build());
            return;
        }
        Object result = JsonPath.parse((String)response).read(responseFilter);

        Map<String, Object> data = new HashMap<>();
        data.put("filtered_response", result);
        modelTensors.add(ModelTensor.builder().name("response").dataAsMap(data).build());
    }

    @Override
    public Connector clone() {
        try (BytesStreamOutput bytesStreamOutput = new BytesStreamOutput()){
            this.writeTo(bytesStreamOutput);
            StreamInput streamInput = bytesStreamOutput.bytes().streamInput();
            return new ChatConnector(streamInput);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getContentIndex() {
        return parameters.get(CONTENT_INDEX);
    }

    public String getEmbeddingModel() {
        return parameters.get(EMBEDDING_MODEL_ID_FIELD);
    }

    public String getEmbeddingField() {
        return parameters.get(EMBEDDING_FIELD_FIELD);
    }

    public String getContentFields() {
        return parameters.get(CONTENT_FIELD_FIELD);
    }

    public Integer getContentDocSize() {
        if (!parameters.containsKey(CONTENT_DOC_SIZE_FIELD)) {
            return DEFAULT_CONTENT_DOC_SIZE;
        }
        return Integer.parseInt(parameters.get(CONTENT_DOC_SIZE_FIELD));
    }

    public String getSessionIndex() {
        return parameters.get(SESSION_INDEX_FIELD);
    }

    public String getSessionIdField() {
        return parameters.get(SESSION_ID_FIELD_FIELD);
    }

    public Integer getSessionSize() {
        if (!parameters.containsKey(SESSION_SIZE_FIELD)) {
            return DEFAULT_SESSION_SIZE;
        }
        return Integer.parseInt(parameters.get(SESSION_SIZE_FIELD));
    }
}