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

import org.apache.giraph.aggregators.BooleanOrAggregator;
import org.apache.giraph.aggregators.IntOverwriteAggregator;
import org.apache.giraph.master.DefaultMasterCompute;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.log4j.Logger;

public class CCSinglePivotMaster extends DefaultMasterCompute {

    private static final Logger LOG = Logger.getLogger(CCSinglePivotMaster.class);

    public static final String PHASE = "cccompute.phase";

    public static final String PIVOT_ACTIVE = "cccompute.pivot_active";

    public static final String MAX_DEG_VERTEX = "cccompute.pivot_vertex";

    public enum Phases {
        FIND_PIVOT,
        INIT_PIVOT,
        PROPAGATE_PIVOT,
        INIT_NORMAL,
        NORMAL
    }

    @Override
    public void initialize() throws InstantiationException, IllegalAccessException {
        registerPersistentAggregator(PHASE, IntOverwriteAggregator.class);
        registerAggregator(MAX_DEG_VERTEX, PairIntMaxAggregator.class);
        registerPersistentAggregator(PIVOT_ACTIVE, BooleanOrAggregator.class);
    }

    @Override
    public void compute() {
        LOG.info("Superstep " + getSuperstep());
        if (getSuperstep() == 0) {
            LOG.info("Set phase FIND_PIVOT");
            setPhase(Phases.FIND_PIVOT);
            return;
        }

        if (getSuperstep() == 1) {
            LOG.info("Set phase INIT_PIVOT");
            setPhase(Phases.INIT_PIVOT);
            return;
        }

        if (getSuperstep() == 2) {
            LOG.info("Set phase PROPAGATE_PIVOT");
            setPhase(Phases.PROPAGATE_PIVOT);
            return;
        }

        if (getPhase() == Phases.PROPAGATE_PIVOT) {
            BooleanWritable active_pivoting = getAggregatedValue(PIVOT_ACTIVE);
            if (!active_pivoting.get()) {
                // pivot component converged, go to the next phase
                LOG.info("Set phase INIT_NORMAL");
                setPhase(Phases.INIT_NORMAL);
            } else {
                LOG.info("Pivot propagation phase still active..");
                // pivoting was active in last superstep - reset for incoming superstep
                setAggregatedValue(PIVOT_ACTIVE, new BooleanWritable(false));
            }
            return;
        }

        if (getPhase() == Phases.INIT_NORMAL) {
            LOG.info("Set phase NORMAL");
            setPhase(Phases.NORMAL);
            return;
        }

        LOG.info(getPhase().toString() + " phase active");
    }

    /**
     * Sets the next phase of the algorithm.
     *
     * @param phase Next phase.
     */
    private void setPhase(Phases phase) {
        setAggregatedValue(PHASE, new IntWritable(phase.ordinal()));
    }

    /**
     * Get current phase.
     *
     * @return Current phase as enumerator.
     */
    private Phases getPhase() {
        IntWritable phaseInt = getAggregatedValue(PHASE);
        return getPhase(phaseInt);
    }

    /**
     * Helper function to convert from internal aggregated value to a Phases
     * enumerator.
     *
     * @param phaseInt An integer that matches a position in the Phases enumerator.
     * @return A Phases' item for the given position.
     */
    public static Phases getPhase(IntWritable phaseInt) {
        return Phases.values()[phaseInt.get()];
    }

}
