/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.ml.common.input.parameter.rcf;

import org.junit.Before;
import org.junit.Test;
import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.core.xcontent.XContentParser;
import org.opensearch.ml.common.TestHelper;

import java.io.IOException;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

public class FitRCFParamsTest {

    FitRCFParams params;
    private Function<XContentParser, FitRCFParams> function = parser -> {
        try {
            return (FitRCFParams) FitRCFParams.parse(parser);
        } catch (IOException e) {
            throw new RuntimeException("failed to parse FitRCFParams", e);
        }
    };

    @Before
    public void setUp() {
        params = FitRCFParams.builder()
                .numberOfTrees(10)
                .shingleSize(8)
                .sampleSize(256)
                .outputAfter(32)
                .timeDecay(0.001)
                .anomalyRate(0.005)
                .timeField("timestamp")
                .dateFormat("yyyy-mm-dd")
                .timeZone("UTC")
                .build();
    }

    @Test
    public void parse_RCFParams() throws IOException {
        TestHelper.testParse(params, function);
    }

    @Test
    public void parse_EmptyRCFParams() throws IOException {
        TestHelper.testParse(FitRCFParams.builder().build(), function);
    }

    @Test
    public void readInputStream_Success() throws IOException {
        readInputStream(params);
    }

    @Test
    public void readInputStream_Success_EmptyParams() throws IOException {
        readInputStream(FitRCFParams.builder().build());
    }

    private void readInputStream(FitRCFParams params) throws IOException {
        BytesStreamOutput bytesStreamOutput = new BytesStreamOutput();
        params.writeTo(bytesStreamOutput);

        StreamInput streamInput = bytesStreamOutput.bytes().streamInput();
        FitRCFParams parsedParams = new FitRCFParams(streamInput);
        assertEquals(params, parsedParams);
    }
}
