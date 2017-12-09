package org.apache.giraph.examples.mimuw;

import org.apache.giraph.aggregators.BasicAggregator;
import org.apache.giraph.writable.tuple.PairWritable;
import org.apache.hadoop.io.IntWritable;


public class PairIntMaxAggregator extends BasicAggregator<PairWritable<IntWritable, IntWritable>> {
    public PairIntMaxAggregator() {
    }

    @Override
    public void aggregate(PairWritable<IntWritable, IntWritable> new_pair) {
        PairWritable<IntWritable, IntWritable> current_pair = this.getAggregatedValue();
        int new_a = new_pair.getLeft().get();
        int new_b = new_pair.getRight().get();
        if (current_pair.getLeft().get() < new_a) {
            this.setAggregatedValue(new_pair);
        } else if (current_pair.getLeft().get() > new_a) {
            return;
        } else if (current_pair.getRight().get() < new_b) {
            this.setAggregatedValue(new_pair);
        }
    }

    @Override
    public PairWritable<IntWritable, IntWritable> createInitialValue() {
        return new PairWritable(new IntWritable(Integer.MIN_VALUE),
                                new IntWritable(Integer.MIN_VALUE));
    }
}

