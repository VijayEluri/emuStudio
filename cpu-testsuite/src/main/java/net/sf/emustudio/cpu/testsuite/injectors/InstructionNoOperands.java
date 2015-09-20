/*
 * Copyright (C) 2015 Peter Jakubčo
 * KISS, YAGNI, DRY
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.emustudio.cpu.testsuite.injectors;

import net.sf.emustudio.cpu.testsuite.CpuRunner;
import net.sf.emustudio.cpu.testsuite.runners.SingleOperandInjector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstructionNoOperands<OperandType extends Number, CpuRunnerType extends CpuRunner>
        implements SingleOperandInjector<OperandType, CpuRunnerType> {
    private final List<Integer> opcodes;

    public InstructionNoOperands(int... instruction) {
        List<Integer> tmpList = new ArrayList<>();
        for (int opcode : instruction) {
            tmpList.add(opcode);
        }
        this.opcodes = Collections.unmodifiableList(tmpList);
    }

    @Override
    public void inject(CpuRunnerType cpuRunner, OperandType unused) {
        cpuRunner.setProgram(opcodes);
    }

    @Override
    public String toString() {
        return String.format("instruction: %s", Utils.toHexString(opcodes.toArray()));
    }

}
