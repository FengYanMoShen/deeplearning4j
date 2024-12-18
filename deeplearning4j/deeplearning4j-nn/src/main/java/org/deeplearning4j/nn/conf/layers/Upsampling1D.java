/*
 *  ******************************************************************************
 *  *
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Apache License, Version 2.0 which is available at
 *  * https://www.apache.org/licenses/LICENSE-2.0.
 *  *
 *  *  See the NOTICE file distributed with this work for additional
 *  *  information regarding copyright ownership.
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  * License for the specific language governing permissions and limitations
 *  * under the License.
 *  *
 *  * SPDX-License-Identifier: Apache-2.0
 *  *****************************************************************************
 */

package org.deeplearning4j.nn.conf.layers;

import java.util.Arrays;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.deeplearning4j.nn.conf.InputPreProcessor;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.memory.LayerMemoryReport;
import org.deeplearning4j.nn.conf.memory.MemoryReport;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.deeplearning4j.util.ValidationUtils;
import org.nd4j.common.base.Preconditions;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Collection;
import java.util.Map;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Upsampling1D extends BaseUpsamplingLayer {

    protected long[] size;

    protected Upsampling1D(UpsamplingBuilder builder) {
        super(builder);
        this.size = builder.size;
    }

    @Override
    public org.deeplearning4j.nn.api.Layer instantiate(NeuralNetConfiguration conf,
                                                       Collection<TrainingListener> trainingListeners, int layerIndex, INDArray layerParamsView,
                                                       boolean initializeParams, DataType networkDataType) {
        org.deeplearning4j.nn.layers.convolution.upsampling.Upsampling1D ret =
                        new org.deeplearning4j.nn.layers.convolution.upsampling.Upsampling1D(conf, networkDataType);
        ret.setListeners(trainingListeners);
        ret.setIndex(layerIndex);
        ret.setParamsViewArray(layerParamsView);
        Map<String, INDArray> paramTable = initializer().init(conf, layerParamsView, initializeParams);
        ret.setParamTable(paramTable);
        ret.setConf(conf);
        return ret;
    }

    @Override
    public Upsampling1D clone() {
        Upsampling1D clone = (Upsampling1D) super.clone();
        return clone;
    }

    @Override
    public InputType getOutputType(int layerIndex, InputType inputType) {
        if (inputType == null || inputType.getType() != InputType.Type.RNN) {
            throw new IllegalStateException("Invalid input for 1D Upsampling layer (layer index = " + layerIndex
                            + ", layer name = \"" + getLayerName() + "\"): expect RNN input type with size > 0. Got: "
                            + inputType);
        }
        InputType.InputTypeRecurrent recurrent = (InputType.InputTypeRecurrent) inputType;
        long outLength = recurrent.getTimeSeriesLength();
        if (outLength > 0) {
            outLength *= size[0];
        }
        return InputType.recurrent(recurrent.getSize(), outLength);
    }

    @Override
    public InputPreProcessor getPreProcessorForInputType(InputType inputType) {
        if (inputType == null) {
            throw new IllegalStateException("Invalid input for Upsampling layer (layer name=\"" + getLayerName()
                            + "\"): input is null");
        }
        return InputTypeUtil.getPreProcessorForInputTypeCnnLayers(inputType, getLayerName());
    }

    @Override
    public LayerMemoryReport getMemoryReport(InputType inputType) {
        InputType.InputTypeRecurrent recurrent = (InputType.InputTypeRecurrent) inputType;
        InputType.InputTypeRecurrent outputType = (InputType.InputTypeRecurrent) getOutputType(-1, inputType);

        long im2colSizePerEx = recurrent.getSize() * outputType.getTimeSeriesLength() * size[0];
        long trainingWorkingSizePerEx = im2colSizePerEx;
        if (getIDropout() != null) {
            trainingWorkingSizePerEx += inputType.arrayElementsPerExample();
        }

        return new LayerMemoryReport.Builder(layerName, Upsampling1D.class, inputType, outputType).standardMemory(0, 0) //No params
                        .workingMemory(0, im2colSizePerEx, 0, trainingWorkingSizePerEx)
                        .cacheMemory(MemoryReport.CACHE_MODE_ALL_ZEROS, MemoryReport.CACHE_MODE_ALL_ZEROS) //No caching
                        .build();
    }

    @NoArgsConstructor
    public static class Builder extends UpsamplingBuilder<Builder> {

        public Builder(int size) {
            super(new int[] {size, size});
        }

        /**
         * Upsampling size
         *
         * @param size upsampling size in single spatial dimension of this 1D layer
         */
        public Builder size(long size) {

            this.setSize(new long[] {size});
            return this;
        }

        /**
         * Upsampling size int array with a single element. Array must be length 1
         *
         * @param size upsampling size in single spatial dimension of this 1D layer
         */
        public Builder size(long[] size) {
            this.setSize(size);
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Upsampling1D build() {
            return new Upsampling1D(this);
        }

        @Override
        public void setSize(long... size) {

            if(size.length == 2) {
                if(size[0] == size[1]) {
                    setSize(size[0]);
                    return;
                } else {
                    Preconditions.checkArgument(false,
                            "When given a length 2 array for size, "
                                    + "the values must be equal.  Got: " + Arrays.toString(size));
                }
            }

            long[] temp = ValidationUtils.validate1NonNegativeLong(size, "size");
            this.size = new long[]{temp[0], temp[0]};
        }
    }

}
