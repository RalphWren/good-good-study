package com.luckypeng.study.flink.window;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows;
import org.apache.flink.streaming.api.windowing.evictors.Evictor;
import org.apache.flink.streaming.api.windowing.triggers.CountTrigger;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.streaming.runtime.operators.windowing.TimestampedValue;
import org.junit.Test;

import java.util.Iterator;

/**
 * 驱逐器
 * @author chenzhipeng
 * @date 2019/3/4 16:58
 */
public class EvictorTest {
    @Test
    public void test() throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<String> dataStream = env.socketTextStream("localhost", 12348);
        dataStream
                .map(Integer::parseInt)
                .windowAll(GlobalWindows.create())
                .evictor(new MyEvictor())
                .trigger(CountTrigger.of(5))
                .max(0)
                .print();
        env.execute();
    }
}

/**
 * 在 window 函数操作执行之前丢弃偶数元素
 */
class MyEvictor implements Evictor<Integer, GlobalWindow> {

    @Override
    public void evictBefore(Iterable<TimestampedValue<Integer>> elements, int size, GlobalWindow window, EvictorContext evictorContext) {
        for (Iterator<TimestampedValue<Integer>> iterator = elements.iterator(); iterator.hasNext();){
            if (iterator.next().getValue() % 2 == 0) {
                iterator.remove();
            }
        }
    }

    @Override
    public void evictAfter(Iterable<TimestampedValue<Integer>> elements, int size, GlobalWindow window, EvictorContext evictorContext) {

    }
}
