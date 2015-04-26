/*
 * Created on 6.2.2008, 8:46:46
 *
 * Copyright (C) 2008-2014 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.devices.mits88disk.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Performs disk operations on single drive.
 */
public class Drive {
    private static final Logger LOGGER = LoggerFactory.getLogger(Drive.class);

    public final static short DEFAULT_SECTORS_COUNT = 32;
    public final static short DEFAULT_SECTOR_LENGTH = 137;

    private volatile short sectorsCount = DEFAULT_SECTORS_COUNT;
    private volatile short sectorLength = DEFAULT_SECTOR_LENGTH;

    private volatile short track;
    private volatile short sector;
    private volatile short sectorOffset;

    private File mountedFloppy = null;
    private SeekableByteChannel imageChannel;
    private boolean selected = false;

    private volatile DriveListener listener;
    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1);

    /*
     7   6   5   4   3   2   1   0
     +---+---+---+---+---+---+---+---+
     | R | Z | I | X | X | H | M | W |
     +---+---+---+---+---+---+---+---+

     W - When 0, write circuit ready to write another byte.
     M - When 0, head movement is allowed
     H - When 0, indicates head is loaded for read/write
     X - not used (will be 0)
     I - When 0, indicates interrupts enabled (not used this emulator)
     Z - When 0, indicates head is on track 0
     R - When 0, indicates that read circuit has new byte to read
     */
    private short port1status;
    private short port2status;

    public static final class DriveParameters {
        public final short port1status;
        public final short port2status;

        public final short track;
        public final short sector;
        public final short sectorOffset;

        public final File mountedFloppy;

        public DriveParameters(short port1status, short port2status, short track, short sector, short sectorOffset,
                               File mountedFloppy) {
            this.port1status = port1status;
            this.port2status = port2status;
            this.track = track;
            this.sector = sector;
            this.sectorOffset = sectorOffset;
            this.mountedFloppy = mountedFloppy;
        }
    }

    public interface DriveListener {

        public void driveSelect(boolean sel);

        public void driveParamsChanged(DriveParameters parameters);
    }

    public Drive() {
        track = 0;
        sector = sectorsCount;
        sectorOffset = sectorLength;
        port1status = 0xE7; // 11100111b
    }

    public void setSectorsCount(short sectorsCount) {
        if (sectorsCount <= 0) {
            throw new IllegalArgumentException("Sectors count must be > 0");
        }
        this.sectorsCount = sectorsCount;
    }

    public void setSectorLength(short sectorLength) {
        if (sectorLength <= 0) {
            throw new IllegalArgumentException("Sector length must be > 0");
        }
        this.sectorLength = sectorLength;
    }

    public short getSectorsCount() {
        return sectorsCount;
    }

    public short getSectorLength() {
        return sectorLength;
    }

    public void setDriveListener(DriveListener listener) {
        this.listener = listener;
    }

    private void notifyDiskSelected() {
        DriveListener tmpListener = listener;
        if (tmpListener != null) {
            tmpListener.driveSelect(selected);
        }
    }

    private void notifyParamsChanged() {
        DriveListener tmpListener = listener;
        if (tmpListener != null) {
            tmpListener.driveParamsChanged(new DriveParameters(
                    port1status, port2status, track, sector, sectorOffset, mountedFloppy
            ));
        }
    }

    public DriveParameters getDriveParameters() {
        return new DriveParameters(port1status, port2status, track, sector, sectorOffset, mountedFloppy);
    }

    public void select() {
        selected = true;
        port1status = 0xE5; // 11100101b
        port2status = 0xC1;
        sector = sectorsCount;
        sectorOffset = sectorLength;
        if (track == 0) {
            port1status &= 0xBF;
        } // head is on track 0
        notifyDiskSelected();
        notifyParamsChanged();
    }

    public void deselect() {
        selected = false;
        port1status = 0xE7;
        port2status = 0xC1;
        notifyDiskSelected();
        notifyParamsChanged();
    }

    public void mount(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.isFile() || !file.exists()) {
            throw new IOException("Specified file name doesn't point to a file");
        }
        umount();
        this.mountedFloppy = file;
        Set<OpenOption> optionSet = new HashSet<>();
        optionSet.add(StandardOpenOption.READ);
        optionSet.add(StandardOpenOption.WRITE);

        imageChannel = Files.newByteChannel(file.toPath(), optionSet);
    }

    public void umount() {
        mountedFloppy = null;
        try {
            if (imageChannel != null) {
                imageChannel.close();
            }
        } catch (IOException e) {
            LOGGER.error("Could not umount disk image", e);
        }
    }

    public File getImageFile() {
        return mountedFloppy;
    }

    public short getPort1status() {
        return port1status;
    }

    public short getPort2status() {
        return port2status;
    }

    /**
     * Drive Control (Device 11 OUT):
     *
     * +---+---+---+---+---+---+---+---+ | W | C | D | E | U | H | O | I |
     * +---+---+---+---+---+---+---+---+
     *
     * I - When 1, steps head IN one track O - When 1, steps head OUT out track
     * H - When 1, loads head to drive surface U - When 1, unloads head E -
     * Enables interrupts (ignored this simulator) D - Disables interrupts
     * (ignored this simulator) C - When 1 lowers head current (ignored this
     * simulator) W - When 1, starts Write Enable sequence: W bit on device 10
     * (see above) will go 1 and data will be read from port 12 until 137 bytes
     * have been read by the controller from that port. The W bit will go off
     * then, and the sector data will be written to disk. Before you do this,
     * you must have stepped the track to the desired number, and waited until
     * the right sector number is presented on device 11 IN, then set this bit.
     */
    public void writeToPort2(short val) {
        if ((val & 0x01) != 0) { /* Step head in */
            track++;
            // if (track > 76) track = 76;
            sector = sectorsCount;
            sectorOffset = sectorLength;
        }
        if ((val & 0x02) != 0) { /* Step head out */
            track--;
            if (track < 0) {
                track = 0;
                port1status &= 0xBF; // head is on track 0
            }
            sector = sectorsCount;
            sectorOffset = sectorLength;
        }
        if ((val & 0x04) != 0) { /* Head load */
            // 11111011
            port1status &= 0xFB; /* turn on head loaded bit */
            port1status &= 0x7F; /* turn on 'read data available */

            port2status = (short)((sector << 1) & 0x3E | 0xC0);
        }
        if ((val & 0x08) != 0) { /* Head Unload */
            port1status |= 0x04; /* turn off 'head loaded' */
            port1status |= 0x80; /* turn off 'read data avail */

            sector = sectorsCount;
            sectorOffset = sectorLength;
        }
        /* Interrupts & head current are ignored */
        if ((val & 0x80) != 0) { /* write sequence start */
            sectorOffset = 0; // sectorLength-1;
            port1status &= 0xFE; /* enter new write data on */
        }
        notifyParamsChanged();
    }

    public void nextSectorIfHeadIsLoaded() {
        if (((~port1status) & 0x04) != 0) { /* head loaded? */
            sector = (short)((sector + 1) % 32);
            sectorOffset = sectorLength;
            port2status = (short)((sector << 1) & 0x3E | 0xC0);
        } else {
            // head not loaded - sector true is 1 (false)
            port2status = 0xC1;
        }
        notifyParamsChanged();
    }

    public void writeData(int data) throws IOException {
        int i = sectorOffset;

        if (sectorOffset < sectorLength) {
            sectorOffset++;
        } else {
            port1status |= 1; /* ENWD off */
            notifyParamsChanged();
            return;
        }

        byteBuffer.clear();
        byteBuffer.put((byte) (data & 0xFF));
        byteBuffer.flip();

        imageChannel.position(sectorsCount * sectorLength * track + sectorLength * sector + i);
        imageChannel.write(byteBuffer);

        notifyParamsChanged();
    }

    public short readData() throws IOException {
        if (mountedFloppy == null) {
            return 0;
        }
        int previousOffset;

        if (sectorOffset >= sectorLength) {
            previousOffset = 0;
        } else {
            previousOffset = sectorOffset;
        }

        if (sectorOffset >= sectorLength) {
            sectorOffset = 1;
        } else {
            sectorOffset++;
        }

        imageChannel.position(sectorsCount * sectorLength * track + sectorLength * sector + previousOffset);
        notifyParamsChanged();

        byteBuffer.clear();
        int bytesRead = imageChannel.read(byteBuffer);
        if (bytesRead != byteBuffer.capacity()) {
            throw new IOException("Could not read data from disk image");
        }
        byteBuffer.flip();
        return (short) (byteBuffer.get() & 0xFF);
    }

    public int getSector() {
        return sector;
    }

    public int getTrack() {
        return track;
    }

    public int getOffset() {
        return sectorOffset;
    }

}
