package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;

public class MemoryAddress<TCpuRunnerType extends CpuRunner>
        implements SingleOperandInjector<Integer, TCpuRunnerType> {
    private final int value;
    private final boolean word;

    public MemoryAddress(Byte value) {
        this.value = value & 0xFF;
        word = false;
    }

    public MemoryAddress(Integer value) {
        this.value = value & 0xFFFF;
        word = true;
    }

    @Override
    public void inject(CpuRunner cpuRunner, Integer address) {
        cpuRunner.setByte(address, value & 0xFF);
        if (word) {
            cpuRunner.setByte(address + 1, (value >>> 8) & 0xFF);
        }
    }

    @Override
    public String toString() {
        return String.format("memory[address] = %04x (word=%s)", value, word);
    }

}
