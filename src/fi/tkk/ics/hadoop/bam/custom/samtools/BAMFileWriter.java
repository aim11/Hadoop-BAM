// Copied to use our custom BlockCompressedOutputStream. Excludes indexing
// support to massively reduce dependencies.
//
// Required for GetSortedBAMHeader.

/*
 * The MIT License
 *
 * Copyright (c) 2009 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.tkk.ics.hadoop.bam.custom.samtools;

import net.sf.samtools.SAMException;

import net.sf.samtools.util.BinaryCodec;

import fi.tkk.ics.hadoop.bam.custom.samtools.BAMFileConstants;
import fi.tkk.ics.hadoop.bam.custom.samtools.BAMRecordCodec;
import fi.tkk.ics.hadoop.bam.custom.samtools.BlockCompressedOutputStream;
import fi.tkk.ics.hadoop.bam.custom.samtools.SAMFileWriterImpl;
import fi.tkk.ics.hadoop.bam.custom.samtools.SAMRecord;

import java.io.DataOutputStream;
import java.io.File;
import java.io.OutputStream;

/**
 * Concrete implementation of SAMFileWriter for writing gzipped BAM files.
 */
public class BAMFileWriter extends SAMFileWriterImpl {

    private final BinaryCodec outputBinaryCodec;
    private BAMRecordCodec bamRecordCodec = null;
    private final BlockCompressedOutputStream blockCompressedOutputStream;

    public BAMFileWriter(final File path) {
        blockCompressedOutputStream = new BlockCompressedOutputStream(path);
        outputBinaryCodec = new BinaryCodec(new DataOutputStream(blockCompressedOutputStream));
        outputBinaryCodec.setOutputFileName(path.toString());
    }

    public BAMFileWriter(final File path, final int compressionLevel) {
        blockCompressedOutputStream = new BlockCompressedOutputStream(path, compressionLevel);
        outputBinaryCodec = new BinaryCodec(new DataOutputStream(blockCompressedOutputStream));
        outputBinaryCodec.setOutputFileName(path.toString());
    }

    public BAMFileWriter(final OutputStream os, final File file) {
        blockCompressedOutputStream = new BlockCompressedOutputStream(os, file);
        outputBinaryCodec = new BinaryCodec(new DataOutputStream(blockCompressedOutputStream));
        outputBinaryCodec.setOutputFileName(file.getAbsolutePath());
    }

    public BAMFileWriter(final OutputStream os, final File file, int compressionLevel) {
        blockCompressedOutputStream = new BlockCompressedOutputStream(os, file, compressionLevel);
        outputBinaryCodec = new BinaryCodec(new DataOutputStream(blockCompressedOutputStream));
        outputBinaryCodec.setOutputFileName(file.getAbsolutePath());
    }

    private void prepareToWriteAlignments() {
        if (bamRecordCodec == null) {
            bamRecordCodec = new BAMRecordCodec(getFileHeader());
            bamRecordCodec.setOutputStream(outputBinaryCodec.getOutputStream());
        }
    }

    protected void writeAlignment(final SAMRecord alignment) {
        prepareToWriteAlignments();

            bamRecordCodec.encode(alignment);
    }

    protected void writeHeader(final String textHeader) {
        outputBinaryCodec.writeBytes(BAMFileConstants.BAM_MAGIC);

        // calculate and write the length of the SAM file header text and the header text
        outputBinaryCodec.writeString(textHeader, true, false);

        // write the sequences binarily.  This is redundant with the text header
        outputBinaryCodec.writeInt(getFileHeader().getSequenceDictionary().size());
        for (final SAMSequenceRecord sequenceRecord: getFileHeader().getSequenceDictionary().getSequences()) {
            outputBinaryCodec.writeString(sequenceRecord.getSequenceName(), true, true);
            outputBinaryCodec.writeInt(sequenceRecord.getSequenceLength());
        }
    }

    protected void finish() {
        outputBinaryCodec.close();
    }

    protected String getFilename() {
        return outputBinaryCodec.getOutputFileName();
    }
}