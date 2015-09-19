package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;

public class MemoryWord<CpuRunnerType extends CpuRunner> implements SingleOperandInjector<Integer, CpuRunnerType> {
    private final int address;

    public MemoryWord(int address) {
        if (address <= 0) {
            throw new IllegalArgumentException("Address can be only > 0!");
        }

        this.address = address;
    }

    @Override
    public void inject(CpuRunner cpuRunner, Integer value) {
        cpuRunner.setByte(address, value & 0xFF);
        cpuRunner.setByte(address + 1, (value >>> 8) & 0xFF);
    }

    @Override
    public String toString() {
        return String.format("memoryWord[%04x]", address);
    }

}
