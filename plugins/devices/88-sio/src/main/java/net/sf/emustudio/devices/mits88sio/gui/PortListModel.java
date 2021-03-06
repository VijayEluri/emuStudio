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
package net.sf.emustudio.devices.mits88sio.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractListModel;

public class PortListModel extends AbstractListModel<String> {
    private final List<Integer> ports = new ArrayList<>();

    public boolean add(int port) {
        if (ports.contains(port)) {
            return false;
        }
        ports.add(port);
        fireContentsChanged(this, 0, ports.size() - 1);
        
        return true;
    }
    
    public void addAll(Collection<Integer> ports) {
        this.ports.addAll(ports);
        fireContentsChanged(this, 0, this.ports.size() - 1);
    }
    
    public Collection<Integer> getAll() {
        return Collections.unmodifiableCollection(ports);
    }
    
    public void clear() {
        ports.clear();
        fireContentsChanged(this, 0, ports.size() - 1);
    }
    
    public void removeAt(int index) {
        ports.remove(index);
        fireContentsChanged(this, 0, ports.size() - 1);
    }

    public boolean contains(int port) {
        return ports.contains(port);
    }
    
    @Override
    public int getSize() {
        return ports.size();
    }

    @Override
    public String getElementAt(int index) {
        return String.format("0x%x", ports.get(index));
    }
    
}
