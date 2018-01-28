package fr.xebia.ldi.fighter.stream.processor;

import fr.xebia.ldi.fighter.schema.Victory;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder;
import org.apache.kafka.streams.state.internals.MeteredWindowStore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;

import java.lang.management.OperatingSystemMXBean;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static fr.xebia.ldi.fighter.stream.utils.Parsing.groupedDataKey;
import static fr.xebia.ldi.fighter.stream.utils.Parsing.parseWindowKey;

/**
 * Created by loicmdivad.
 */
public class ProcessVictory implements Processor {

    private ProcessorContext context;
    private WindowStore<GenericRecord, Long> victoryStore;

    @Override
    @SuppressWarnings("unchecked")
    public void init(ProcessorContext context) {
        this.context = context;
        this.victoryStore = (WindowStore) this.context.getStateStore("VICTORIES-STORE");
    }

    @Override
    public void process(Object key, Object value) {

        long now = DateTime.now().getMillis();
        long since = now - TimeUnit.SECONDS.toMillis(15);
        long windowStart = computeWindowStart(since);

        GenericRecord groupKey = groupedDataKey((Victory) value);

        WindowStoreIterator<Long> it = this.victoryStore.fetch(groupKey, windowStart, now);

        if(it.hasNext()){
            KeyValue<Long, Long> keyValue = it.next();
            long total = keyValue.value + 1L;
            this.victoryStore.put(groupKey, total, windowStart);
            KeyValue<GenericRecord, GenericRecord> kvDisplay = parseWindowKey(windowStart, groupKey, total);
            context.forward(kvDisplay.key, kvDisplay.value);
        } else {
            this.victoryStore.put(groupKey, 1L, windowStart);
            KeyValue<GenericRecord, GenericRecord> kvDisplay = parseWindowKey(windowStart, groupKey, 1);
            context.forward(kvDisplay.key, kvDisplay.value);
        }

    }

    @Override
    public void punctuate(long timestamp) {

    }

    @Override
    public void close() {

    }

    private static Long computeWindowStart(long timestamp) {
        DateTime datetime = new DateTime(timestamp);
        int start = datetime.get(DateTimeFieldType.secondOfMinute()) / 15;
        DateTime windowStart = datetime.secondOfMinute().setCopy(start * 15);
        return windowStart.getMillis();
    }

    /*private static Long kWindowStart(long timestamp) {
        long windowStart = (Math.max(0, timestamp - sizeMs + advanceMs) / advanceMs) * advanceMs;
    }*/

}
