/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package net.sf.emustudio.ssem.memory.impl;

import emulib.plugins.memory.Memory;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MemoryContextImplTest {

    @Test
    public void testAfterClearObserversAreNotified() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        Memory.MemoryListener listener = createMock(Memory.MemoryListener.class);
        listener.memoryChanged(eq(-1));
        expectLastCall().once();
        replay(listener);

        context.addMemoryListener(listener);
        context.clear();

        verify(listener);
    }

    @Test
    public void testReadWithoutWritReturnsZero() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        assertEquals(0L, (long)context.read(10));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadAtInvalidLocationThrows() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        context.read(-1);
    }

    @Test
    public void testAfterReadNoObserversAreNotified() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        Memory.MemoryListener listener = createMock(Memory.MemoryListener.class);
        replay(listener);

        context.addMemoryListener(listener);
        context.read(10);

        verify(listener);
    }

    @Test
    public void testAfterWriteObserversAreNotified() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        Memory.MemoryListener listener = createMock(Memory.MemoryListener.class);
        listener.memoryChanged(eq(10));
        expectLastCall().once();
        replay(listener);

        context.addMemoryListener(listener);
        context.write(10, (byte)134);

        verify(listener);
    }

    @Test
    public void testWriteReallyWritesCorrectValueAtCorrectLocation() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        context.write(10, (byte)134);
        assertEquals((byte)134, (byte)context.read(10));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteAtInvalidLocationThrows() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        context.write(-1, (byte)134);
    }

    @Test
    public void testGetSizeReturnsNumberOfCells() throws Exception {
        MemoryContextImpl context = new MemoryContextImpl();

        assertEquals(MemoryContextImpl.NUMBER_OF_CELLS, context.getSize());
    }

    @Test
    public void testClassTypeIsByte() throws Exception {
        assertEquals(Byte.class, new MemoryContextImpl().getDataType());
    }

    @Test
    public void testReadWordIsSupported() throws Exception {
        assertArrayEquals(new Byte[] {0,0,0,0}, new MemoryContextImpl().readWord(0));
    }

    @Test
    public void testWriteWordIsSupported() throws Exception {
        MemoryContextImpl mem = new MemoryContextImpl();
        
        Byte[] row = new Byte[] {1,2,3,4};
        mem.writeWord(0, row);
        
        assertArrayEquals(row, mem.readWord(0));
    }
}
