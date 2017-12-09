/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.giraph.examples.mimuw;

import org.apache.giraph.edge.Edge;
import org.apache.giraph.examples.Algorithm;
import org.apache.giraph.graph.BasicComputation;
import org.apache.giraph.graph.Vertex;
import org.apache.giraph.writable.tuple.PairWritable;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;

import java.io.IOException;

import static org.apache.giraph.examples.mimuw.CCSinglePivotMaster.PHASE;
import static org.apache.giraph.examples.mimuw.CCSinglePivotMaster.MAX_DEG_VERTEX;
import static org.apache.giraph.examples.mimuw.CCSinglePivotMaster.PIVOT_ACTIVE;


@Algorithm(
    name = "Connected components using Single Pivot optimization",
    description = "Finds connected components of the graph"
)
public class CCSinglePivot extends
        BasicComputation<IntWritable, IntWritable, NullWritable, IntWritable> {

    private CCSinglePivotMaster.Phases currPhase;

    @Override
    public void preSuperstep() {
        IntWritable phaseInt = getAggregatedValue(PHASE);
        currPhase = CCSinglePivotMaster.getPhase(phaseInt);
        // doesn't work without this line
        aggregate(PHASE, new IntWritable(phaseInt.get()));
    }

    @Override
    public void compute(
            Vertex<IntWritable, IntWritable, NullWritable> vertex,
            Iterable<IntWritable> messages) throws IOException {
        int currentComponent = vertex.getValue().get();

        switch (currPhase) {
            case FIND_PIVOT:
                // find vertex with max deg as a candidate for pivot
                IntWritable deg = new IntWritable(vertex.getNumEdges());
                aggregate(MAX_DEG_VERTEX, new PairWritable(deg, new IntWritable(currentComponent)));
                break;
            case INIT_PIVOT:
                PairWritable<IntWritable, IntWritable> pair = getAggregatedValue(MAX_DEG_VERTEX);
                IntWritable pivot_id = pair.getRight();
                if (pivot_id.get() == currentComponent) {
                    // I'm a chosen pivot
                    sendMessageToAllEdges(vertex, pivot_id);
                    // pivot vertex is done and no longer needed
                    vertex.voteToHalt();
                }
                break;
            case PROPAGATE_PIVOT:
                boolean changed = false;
                if (messages.iterator().hasNext()) {
                    // there might be more than 1 message, but all the same, so take 1st only
                    pivot_id = messages.iterator().next();
                    if (currentComponent != pivot_id.get()) {
                        vertex.setValue(pivot_id);
                        changed = true;
                        sendMessageToAllEdges(vertex, pivot_id);
                    }
                    // in big component, we're done here
                    vertex.voteToHalt();
                }
                if (changed) {
                    // there was an update, so next superstep has to be pivot-like
                    aggregate(PIVOT_ACTIVE, new BooleanWritable(true));
                }
                break;
            case INIT_NORMAL:
                // look at the neighbors only
                for (Edge<IntWritable, NullWritable> edge : vertex.getEdges()) {
                    int neighbor = edge.getTargetVertexId().get();
                    if (neighbor < currentComponent) {
                        currentComponent = neighbor;
                    }
                }
                // Only need to send value if it is not the own id
                if (currentComponent != vertex.getValue().get()) {
                    vertex.setValue(new IntWritable(currentComponent));
                    for (Edge<IntWritable, NullWritable> edge : vertex.getEdges()) {
                        IntWritable neighbor = edge.getTargetVertexId();
                        if (neighbor.get() > currentComponent) {
                            sendMessage(neighbor, vertex.getValue());
                        }
                    }
                }
                vertex.voteToHalt();
                break;
            case NORMAL:
                changed = false;
                // did we get a smaller id ?
                for (IntWritable message : messages) {
                    int candidateComponent = message.get();
                    if (candidateComponent < currentComponent) {
                        currentComponent = candidateComponent;
                        changed = true;
                    }
                }

                // propagate new component id to the neighbors
                if (changed) {
                    vertex.setValue(new IntWritable(currentComponent));
                    sendMessageToAllEdges(vertex, vertex.getValue());
                }
                vertex.voteToHalt();
                break;
        }
    }
}
