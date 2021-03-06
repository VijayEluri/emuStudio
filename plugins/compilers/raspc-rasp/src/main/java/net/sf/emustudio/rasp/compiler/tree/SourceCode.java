/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2016, Michal Šipoš
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.rasp.compiler.tree;

import net.sf.emustudio.rasp.compiler.CompilerOutput;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author miso
 */
public class SourceCode extends AbstractTreeNode {

    private final int programStart;
    private final List<Integer> inputs = new ArrayList<>();
    private final Program program;
    private boolean programStartZero;
    private boolean programStartUndefined;

    public SourceCode(int programStart, Input inputs, Program program) {
        if (programStart == 0) {
            this.programStart = 20;
            this.programStartZero = true;
        } else if (programStart == -1) {
            this.programStart = 20;
            this.programStartUndefined = true;
        } else {
            this.programStart = programStart;
        }

        this.inputs.addAll(inputs.getAll());
        this.program = program;
    }

    public SourceCode(int programStart, Program program) {
        this(programStart, new Input(), program);
    }

    public SourceCode(Input input, Program program) {
        this(-1, input, program);
    }

    public SourceCode(Program program) {
        this(-1, new Input(), program);
    }

    @Override
    public void pass() throws Exception {
        CompilerOutput.getInstance().setProgramStart(programStart);
        CompilerOutput.getInstance().addInputs(inputs);
        program.pass();
    }

    public boolean isProgramStartZero() {
        return programStartZero;
    }

    public boolean isProgramStartUndefined() {
        return programStartUndefined;
    } 

}
