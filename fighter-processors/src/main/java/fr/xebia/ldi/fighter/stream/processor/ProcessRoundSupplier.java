package fr.xebia.ldi.fighter.stream.processor;

import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorSupplier;

/**
 * Created by loicmdivad.
 */
public class ProcessRoundSupplier implements ProcessorSupplier {
    @Override
    public Processor get() {
        return new ProcessRound();
    }
}
