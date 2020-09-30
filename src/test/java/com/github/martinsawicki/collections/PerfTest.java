/**
 * Copyright (c) Martin Sawicki. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for license information.
 */
package com.github.martinsawicki.collections;

import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations=2)
@Measurement(iterations=3)
public class PerfTest {
    public static final void main(final String args[]) throws RunnerException, IOException {
        Main.main(args);
    }

    @Param({"100", "1000", "10000", "100000", "1000000"})
    int length;

    static final UnaryOperator<Long> NEXT_ITEM_GENERATOR = (i) -> Math.round(Math.random() * 100.0);
    static final UnaryOperator<Character> NEXT_ITEM_TRANSFORM_GENERATOR = (i) -> (char) (65 + Math.round(Math.random() * 26.0));
    static final Function<Character, String> TRANSFORMER = (i) -> Character.toString(i);

    @Benchmark
    public long benchmarkChainCount() {
        return Chainable
                .from(1l)
                .chain(NEXT_ITEM_GENERATOR)
                .first(this.length)
                .count();
    }

    @Benchmark
    public long benchmarkStreamCount() {
        return Stream
            .iterate(1l, NEXT_ITEM_GENERATOR)
            .limit(length)
            .count();
    }

    @Benchmark
    public long benchmarkChainTransform() {
        return Chainable
                .from((char) 65)
                .chain(NEXT_ITEM_TRANSFORM_GENERATOR)
                .first(length)
                .transform(TRANSFORMER)
                .count();        
    }

    @Benchmark
    public long benchmarkStreamMap() {
        return Stream
                .iterate((char) 65, NEXT_ITEM_TRANSFORM_GENERATOR)
                .limit(length)
                .map(TRANSFORMER)
                .count();        
    }

    static final Function<Character, Iterable<String>> CHAIN_TRANSFORMER = (i) -> Chainable.from(Character.toString(i), Character.toString(i), Character.toString(i));

    @Benchmark
    public long benchmarkChainTransformAndFlatten() {
        return Chainable
                .from((char) 65)
                .chain(NEXT_ITEM_TRANSFORM_GENERATOR)
                .first(length)
                .transformAndFlatten(CHAIN_TRANSFORMER)
                .count();
    }

    static final Function<Character, Stream<String>> STREAM_TRANSFORMER = (i) -> Stream.of(Character.toString(i), Character.toString(i), Character.toString(i));

    @Benchmark
    public long benchmarkStreamFlatMap() {
        return Stream
                .iterate((char) 65, NEXT_ITEM_TRANSFORM_GENERATOR)
                .limit(length)
                .flatMap(STREAM_TRANSFORMER)
                .count();
    }
}
