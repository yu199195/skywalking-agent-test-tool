/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.plugin.test.agent.tool.validator.assertor;

import org.apache.skywalking.plugin.test.agent.tool.validator.assertor.exception.HistogramSizeNotEqualsException;
import org.apache.skywalking.plugin.test.agent.tool.validator.assertor.exception.MeterAssertFailedException;
import org.apache.skywalking.plugin.test.agent.tool.validator.assertor.exception.MeterHistogramValueInvalidException;
import org.apache.skywalking.plugin.test.agent.tool.validator.assertor.exception.MeterNotFoundException;
import org.apache.skywalking.plugin.test.agent.tool.validator.assertor.exception.ValueAssertFailedException;
import org.apache.skywalking.plugin.test.agent.tool.validator.entity.Meter;
import org.apache.skywalking.plugin.test.agent.tool.validator.entity.MeterItem;
import org.apache.skywalking.plugin.test.agent.tool.validator.exception.AssertFailedException;

import java.util.Objects;

public class MeterAssert {

    public static void assertEquals(MeterItem expected, MeterItem actual) {
        if (expected.getMeters() == null) {
            return;
        }
        for (Meter meter : expected.getMeters()) {
            // find same meter id
            Meter actualMeter = findMeter(actual, meter);
            if (actualMeter == null) {
                throw new MeterNotFoundException(meter);
            }

            try {
                // check data
                meterDataEquals(meter, actualMeter);
            } catch (AssertFailedException e) {
                throw new MeterAssertFailedException(e, meter.getMeterId());
            }
        }
    }

    private static Meter findMeter(MeterItem actual, Meter expectedMeter) {
        return actual.getMeters().stream().filter(s -> Objects.equals(s.getMeterId(), expectedMeter.getMeterId())).findFirst().orElse(null);
    }

    private static void meterDataEquals(Meter excepted, Meter actual) {
        // check single value
        if (excepted.getSingleValue() != null) {
            ExpressParser.parse(excepted.getSingleValue()).assertValue("single value", actual.getSingleValue());
        } else {
            // histogram
            // check size
            if (excepted.getHistogramBuckets().size() != actual.getHistogramBuckets().size()) {
                throw new HistogramSizeNotEqualsException(excepted, actual.getHistogramBuckets().size());
            }

            // check buckets
            for (int bucketIndex = 0; bucketIndex < excepted.getHistogramBuckets().size(); bucketIndex++) {
                final String exceptedBucket = excepted.getHistogramBuckets().get(bucketIndex);
                final String actualBucket = actual.getHistogramBuckets().get(bucketIndex);

                if (!Objects.equals(exceptedBucket, actualBucket)) {
                    throw new ValueAssertFailedException("histogram bucket", exceptedBucket, actualBucket);
                }
            }

            // check values
            boolean valueContainValid = false;
            for (String histogramValue : actual.getHistogramValues()) {
                if (Long.parseLong(histogramValue) > 0) {
                    valueContainValid = true;
                    break;
                }
            }
            if (!valueContainValid) {
                throw new MeterHistogramValueInvalidException(actual.getMeterId());
            }
        }
    }
}
