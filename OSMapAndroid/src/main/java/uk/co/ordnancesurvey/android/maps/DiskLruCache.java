/**
 * OpenSpace Android SDK Licence Terms
 *
 * The OpenSpace Android SDK is protected by © Crown copyright – Ordnance Survey 2013.[https://github.com/OrdnanceSurvey]
 *
 * All rights reserved (subject to the BSD licence terms as follows):.
 *
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * Neither the name of Ordnance Survey nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *
 *
 *
 * Based on cobbled-together DiskLruCache:
 *   https://android.googlesource.com/platform/external/okhttp/+/09d4e3660325c8d9718f621de61042bc0adf09c1/src/main/java/com/squareup/okhttp/internal/DiskLruCache.java
 *   https://android.googlesource.com/platform/external/okhttp/+/09d4e3660325c8d9718f621de61042bc0adf09c1/src/main/java/com/squareup/okhttp/internal/Util.java
 *   https://android.googlesource.com/platform/external/okhttp/+/09d4e3660325c8d9718f621de61042bc0adf09c1/src/main/java/com/squareup/okhttp/internal/StrictLineReader.java
 *
 *
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.ordnancesurvey.android.maps;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.nio.charset.Charset;

import android.util.Log;

/**
 * A cache that uses a bounded amount of space on a filesystem. Each cache
 * entry has a string key and a fixed number of values. Values are byte
 * sequences, accessible as streams or files. Each value must be between {@code
 * 0} and {@code Integer.MAX_VALUE} bytes in length.
 *
 * <p>The cache stores its data in a directory on the filesystem. This
 * directory must be exclusive to the cache; the cache may delete or overwrite
 * files from its directory. It is an error for multiple processes to use the
 * same cache directory at the same time.
 *
 * <p>This cache limits the number of bytes that it will store on the
 * filesystem. When the number of stored bytes exceeds the limit, the cache will
 * remove entries in the background until the limit is satisfied. The limit is
 * not strict: the cache may temporarily exceed it while waiting for files to be
 * deleted. The limit does not include filesystem overhead or the cache
 * journal so space-sensitive applications should set a conservative limit.
 *
 * <p>Clients call {@link #edit} to create or update the values of an entry. An
 * entry may have only one editor at one time; if a value is not available to be
 * edited then {@link #edit} will return null.
 * <ul>
 * <li>When an entry is being <strong>created</strong> it is necessary to
 * supply a full set of values; the empty value should be used as a
 * placeholder if necessary.
 * <li>When an entry is being <strong>edited</strong>, it is not necessary
 * to supply data for every value; values default to their previous
 * value.
 * </ul>
 * Every {@link #edit} call must be matched by a call to {@link Editor#commit}
 * or {@link Editor#abort}. Committing is atomic: a read observes the full set
 * of values as they were before or after the commit, but never a mix of values.
 *
 * <p>Clients call {@link #get} to read a snapshot of an entry. The read will
 * observe the value at the time that {@link #get} was called. Updates and
 * removals after the call do not impact ongoing reads.
 *
 * <p>This class is tolerant of some I/O errors. If files are missing from the
 * filesystem, the corresponding entries will be dropped from the cache. If
 * an error occurs while writing a cache value, the edit will fail silently.
 * Callers should handle other problems by catching {@code IOException} and
 * responding appropriately.
 */
final class DiskLruCache implements Closeable {
  static final Charset US_ASCII = Charset.forName("US-ASCII");
  static final Charset UTF_8 = Charset.forName("UTF-8");
  static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
  private static final String TAG = "DiskLruCache";
  private static final String JOURNAL_FILE = "journal";
  private static final String JOURNAL_FILE_TMP = "journal.tmp";
  private static final String MAGIC = "libcore.io.DiskLruCache";
  private static final String VERSION_1 = "1";
  private static final long ANY_SEQUENCE_NUMBER = -1;
  private static final String CLEAN = "CLEAN";
  private static final String DIRTY = "DIRTY";
  private static final String REMOVE = "REMOVE";
  private static final String READ = "READ";

  // This cache uses a journal file named "journal". A typical journal file
  // looks like this:
  //     libcore.io.DiskLruCache
  //     1
  //     100
  //     2
  //
  //     CLEAN 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054
  //     DIRTY 335c4c6028171cfddfbaae1a9c313c52
  //     CLEAN 335c4c6028171cfddfbaae1a9c313c52 3934 2342
  //     REMOVE 335c4c6028171cfddfbaae1a9c313c52
  //     DIRTY 1ab96a171faeeee38496d8b330771a7a
  //     CLEAN 1ab96a171faeeee38496d8b330771a7a 1600 234
  //     READ 335c4c6028171cfddfbaae1a9c313c52
  //     READ 3400330d1dfc7f3f7f4b8d4d803dfcf6
  //
  // The first five lines of the journal form its header. They are the
  // constant string "libcore.io.DiskLruCache", the disk cache's version,
  // the application's version, the value count, and a blank line.
  //
  // Each of the subsequent lines in the file is a record of the state of a
  // cache entry. Each line contains space-separated values: a state, a key,
  // and optional state-specific values.
  //   o DIRTY lines track that an entry is actively being created or updated.
  //     Every successful DIRTY action should be followed by a CLEAN or REMOVE
  //     action. DIRTY lines without a matching CLEAN or REMOVE indicate that
  //     temporary files may need to be deleted.
  //   o CLEAN lines track a cache entry that has been successfully published
  //     and may be read. A publish line is followed by the lengths of each of
  //     its values.
  //   o READ lines track accesses for LRU.
  //   o REMOVE lines track entries that have been deleted.
  //
  // The journal file is appended to as cache operations occur. The journal may
  // occasionally be compacted by dropping redundant lines. A temporary file named
  // "journal.tmp" will be used during compaction; that file should be deleted if
  // it exists when the cache is opened.

  private final File directory;
  private final File journalFile;
  private final File journalFileTmp;
  private final int appVersion;
  private final long maxSize;
  private final int valueCount;
  private long size = 0;
  private Writer journalWriter;
  private final LinkedHashMap<String, Entry> lruEntries =
      new LinkedHashMap<String, Entry>(0, 0.75f, true);
  private int redundantOpCount;

  /**
   * To differentiate between old and current snapshots, each entry is given
   * a sequence number each time an edit is committed. A snapshot is stale if
   * its sequence number is not equal to its entry's sequence number.
   */
  private long nextSequenceNumber = 0;

  /** This cache uses a single background thread to evict entries. */
  private final ExecutorService executorService =
      new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
  private final Callable<Void> cleanupCallable = new Callable<Void>() {
    @Override public Void call() throws Exception {
      synchronized (DiskLruCache.this) {
        if (journalWriter == null) {
          return null; // closed
        }
        trimToSize();
        if (journalRebuildRequired()) {
          rebuildJournal();
          redundantOpCount = 0;
        }
      }
      return null;
    }
  };

  private DiskLruCache(File directory, int appVersion, int valueCount, long maxSize) {
    this.directory = directory;
    this.appVersion = appVersion;
    this.journalFile = new File(directory, JOURNAL_FILE);
    this.journalFileTmp = new File(directory, JOURNAL_FILE_TMP);
    this.valueCount = valueCount;
    this.maxSize = maxSize;
  }

  /**
   * Opens the cache in {@code directory}, creating a cache if none exists
   * there.
   *
   * @param directory a writable directory
   * @param valueCount the number of values per cache entry. Must be positive.
   * @param maxSize the maximum number of bytes this cache should use to store
   * @throws IOException if reading or writing the cache directory fails
   */
  static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize)
      throws IOException {
    if (maxSize <= 0) {
      throw new IllegalArgumentException("maxSize <= 0");
    }
    if (valueCount <= 0) {
      throw new IllegalArgumentException("valueCount <= 0");
    }

    // prefer to pick up where we left off
    DiskLruCache cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
    if (cache.journalFile.exists()) {
      try {
        cache.readJournal();
        cache.processJournal();
        cache.journalWriter = new BufferedWriter(new FileWriter(cache.journalFile, true));
        return cache;
      } catch (IOException journalIsCorrupt) {
        Log.w(TAG,"Directory"
                + " is corrupt: "
                + journalIsCorrupt.getMessage()
                + ", removing");
        cache.delete();
      }
    }

    // create a new empty cache
    directory.mkdirs();
    cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
    cache.rebuildJournal();
    return cache;
  }

  private void readJournal() throws IOException {
    StrictLineReader reader = new StrictLineReader(new FileInputStream(journalFile), US_ASCII);
    try {
      String magic = reader.readLine();
      String version = reader.readLine();
      String appVersionString = reader.readLine();
      String valueCountString = reader.readLine();
      String blank = reader.readLine();
      if (!MAGIC.equals(magic) || !VERSION_1.equals(version) || !Integer.toString(appVersion)
          .equals(appVersionString) || !Integer.toString(valueCount).equals(valueCountString) || !""
          .equals(blank)) {
        throw new IOException("unexpected journal header: ["
            + magic
            + ", "
            + version
            + ", "
            + valueCountString
            + ", "
            + blank
            + "]");
      }

      while (true) {
        try {
          readJournalLine(reader.readLine());
        } catch (EOFException endOfJournal) {
          break;
        }
      }
    } finally {
      closeQuietly(reader);
    }
  }

  private void readJournalLine(String line) throws IOException {
    String[] parts = line.split(" ");
    if (parts.length < 2) {
      throw new IOException("unexpected journal line: " + line);
    }

    String key = parts[1];
    if (parts[0].equals(REMOVE) && parts.length == 2) {
      lruEntries.remove(key);
      return;
    }

    Entry entry = lruEntries.get(key);
    if (entry == null) {
      entry = new Entry(key);
      lruEntries.put(key, entry);
    }

    if (parts[0].equals(CLEAN) && parts.length == 2 + valueCount) {
      entry.readable = true;
      entry.currentEditor = null;
      entry.setLengths(Arrays.copyOfRange(parts, 2, parts.length));
    } else if (parts[0].equals(DIRTY) && parts.length == 2) {
      entry.currentEditor = new Editor(entry);
    } else if (parts[0].equals(READ) && parts.length == 2) {
      // this work was already done by calling lruEntries.get()
    } else {
      throw new IOException("unexpected journal line: " + line);
    }
  }

  /**
   * Computes the initial size and collects garbage as a part of opening the
   * cache. Dirty entries are assumed to be inconsistent and will be deleted.
   */
  private void processJournal() {
    deleteIfExists(journalFileTmp);
    for (Iterator<Entry> i = lruEntries.values().iterator(); i.hasNext(); ) {
      Entry entry = i.next();
      if (entry.currentEditor == null) {
        for (int t = 0; t < valueCount; t++) {
          size += entry.lengths[t];
        }
      } else {
        entry.currentEditor = null;
        for (int t = 0; t < valueCount; t++) {
          deleteIfExists(entry.getCleanFile(t));
          deleteIfExists(entry.getDirtyFile(t));
        }
        i.remove();
      }
    }
  }

  /**
   * Creates a new journal that omits redundant information. This replaces the
   * current journal if it exists.
   */
  private synchronized void rebuildJournal() throws IOException {
    if (journalWriter != null) {
      journalWriter.close();
    }

    Writer writer = new BufferedWriter(new FileWriter(journalFileTmp));
    writer.write(MAGIC);
    writer.write("\n");
    writer.write(VERSION_1);
    writer.write("\n");
    writer.write(Integer.toString(appVersion));
    writer.write("\n");
    writer.write(Integer.toString(valueCount));
    writer.write("\n");
    writer.write("\n");

    for (Entry entry : lruEntries.values()) {
      if (entry.currentEditor != null) {
        writer.write(DIRTY + ' ' + entry.key + '\n');
      } else {
        writer.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
      }
    }

    writer.close();
    journalFileTmp.renameTo(journalFile);
    journalWriter = new BufferedWriter(new FileWriter(journalFile, true));
  }

  private static void deleteIfExists(File file) {
    file.delete();
  }

  /**
   * Returns a snapshot of the entry named {@code key}, or null if it doesn't
   * exist is not currently readable. If a value is returned, it is moved to
   * the head of the LRU queue.
   */
  synchronized Snapshot get(String key) throws IOException {
    checkNotClosed();
    validateKey(key);
    Entry entry = lruEntries.get(key);
    if (entry == null) {
      return null;
    }

    if (!entry.readable) {
      return null;
    }

    // Open all streams eagerly to guarantee that we see a single published
    // snapshot. If we opened streams lazily then the streams could come
    // from different edits.
    InputStream[] ins = new InputStream[valueCount];
    try {
      for (int i = 0; i < valueCount; i++) {
        ins[i] = new FileInputStream(entry.getCleanFile(i));
      }
    } catch (FileNotFoundException e) {
      // a file must have been deleted manually!
      return null;
    }

    redundantOpCount++;
    journalWriter.append(READ + ' ' + key + '\n');
    if (journalRebuildRequired()) {
      executorService.submit(cleanupCallable);
    }

    return new Snapshot(key, entry.sequenceNumber, ins);
  }

  /**
   * Returns an editor for the entry named {@code key}, or null if another
   * edit is in progress.
   */
  Editor edit(String key) throws IOException {
    return edit(key, ANY_SEQUENCE_NUMBER);
  }

  private synchronized Editor edit(String key, long expectedSequenceNumber) throws IOException {
    checkNotClosed();
    validateKey(key);
    Entry entry = lruEntries.get(key);
    if (expectedSequenceNumber != ANY_SEQUENCE_NUMBER && (entry == null
        || entry.sequenceNumber != expectedSequenceNumber)) {
      return null; // snapshot is stale
    }
    if (entry == null) {
      entry = new Entry(key);
      lruEntries.put(key, entry);
    } else if (entry.currentEditor != null) {
      return null; // another edit is in progress
    }

    Editor editor = new Editor(entry);
    entry.currentEditor = editor;

    // flush the journal before creating files to prevent file leaks
    journalWriter.write(DIRTY + ' ' + key + '\n');
    journalWriter.flush();
    return editor;
  }

  /** Returns the directory where this cache stores its data. */
  File getDirectory() {
    return directory;
  }

  /**
   * Returns the maximum number of bytes that this cache should use to store
   * its data.
   */
  long maxSize() {
    return maxSize;
  }

  /**
   * Returns the number of bytes currently being used to store the values in
   * this cache. This may be greater than the max size if a background
   * deletion is pending.
   */
  synchronized long size() {
    return size;
  }

  private synchronized void completeEdit(Editor editor, boolean success) throws IOException {
    Entry entry = editor.entry;
    if (entry.currentEditor != editor) {
      throw new IllegalStateException();
    }

    // if this edit is creating the entry for the first time, every index must have a value
    if (success && !entry.readable) {
      for (int i = 0; i < valueCount; i++) {
        if (!editor.written[i]) {
          editor.abort();
          throw new IllegalStateException("Newly created entry didn't create value for index " + i);
        }
        if (!entry.getDirtyFile(i).exists()) {
          editor.abort();
          Log.w(TAG, "Newly created entry doesn't have file for index " + i);
          return;
        }
      }
    }

    for (int i = 0; i < valueCount; i++) {
      File dirty = entry.getDirtyFile(i);
      if (success) {
        if (dirty.exists()) {
          File clean = entry.getCleanFile(i);
          dirty.renameTo(clean);
          long oldLength = entry.lengths[i];
          long newLength = clean.length();
          entry.lengths[i] = newLength;
          size = size - oldLength + newLength;
        }
      } else {
        deleteIfExists(dirty);
      }
    }

    redundantOpCount++;
    entry.currentEditor = null;
    if (entry.readable | success) {
      entry.readable = true;
      journalWriter.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
      if (success) {
        entry.sequenceNumber = nextSequenceNumber++;
      }
    } else {
      lruEntries.remove(entry.key);
      journalWriter.write(REMOVE + ' ' + entry.key + '\n');
    }

    if (size > maxSize || journalRebuildRequired()) {
      executorService.submit(cleanupCallable);
    }
  }

  /**
   * We only rebuild the journal when it will halve the size of the journal
   * and eliminate at least 2000 ops.
   */
  private boolean journalRebuildRequired() {
    final int redundantOpCompactThreshold = 2000;
    return redundantOpCount >= redundantOpCompactThreshold && redundantOpCount >= lruEntries.size();
  }

  /**
   * Drops the entry for {@code key} if it exists and can be removed. Entries
   * actively being edited cannot be removed.
   *
   * @return true if an entry was removed.
   */
  synchronized boolean remove(String key) throws IOException {
    checkNotClosed();
    validateKey(key);
    Entry entry = lruEntries.get(key);
    if (entry == null || entry.currentEditor != null) {
      return false;
    }

    for (int i = 0; i < valueCount; i++) {
      File file = entry.getCleanFile(i);
      if (!file.delete()) {
        throw new IOException("failed to delete " + file);
      }
      size -= entry.lengths[i];
      entry.lengths[i] = 0;
    }

    redundantOpCount++;
    journalWriter.append(REMOVE + ' ' + key + '\n');
    lruEntries.remove(key);

    if (journalRebuildRequired()) {
      executorService.submit(cleanupCallable);
    }

    return true;
  }

  /** Returns true if this cache has been closed. */
  boolean isClosed() {
    return journalWriter == null;
  }

  private void checkNotClosed() {
    if (journalWriter == null) {
      throw new IllegalStateException("cache is closed");
    }
  }

  /** Force buffered operations to the filesystem. */
  synchronized void flush() throws IOException {
    checkNotClosed();
    trimToSize();
    journalWriter.flush();
  }

  /** Closes this cache. Stored values will remain on the filesystem. */
  @Override
  public synchronized void close() throws IOException {
    if (journalWriter == null) {
      return; // already closed
    }
    for (Entry entry : new ArrayList<Entry>(lruEntries.values())) {
      if (entry.currentEditor != null) {
        entry.currentEditor.abort();
      }
    }
    trimToSize();
    journalWriter.close();
    journalWriter = null;
  }

  private void trimToSize() throws IOException {
    while (size > maxSize) {
      Map.Entry<String, Entry> toEvict = lruEntries.entrySet().iterator().next();
      remove(toEvict.getKey());
    }
  }

  void touch(String key) {
    lruEntries.get(key);
  }


  /**
   * Closes the cache and deletes all of its stored values. This will delete
   * all files in the cache directory including files that weren't created by
   * the cache.
   */
  void delete() throws IOException {
    close();
    deleteContents(directory);
  }

  private void validateKey(String key) {
    if (key.contains(" ") || key.contains("\n") || key.contains("\r")) {
      throw new IllegalArgumentException(
          "keys must not contain spaces or newlines: \"" + key + "\"");
    }
  }

  private static byte[] inputStreamToBytes(InputStream in) throws IOException {
    return readFully(in);
  }

  /** A snapshot of the values for an entry. */
  final class Snapshot implements Closeable {
    private final String key;
    private final long sequenceNumber;
    private final InputStream[] ins;

    private Snapshot(String key, long sequenceNumber, InputStream[] ins) {
      this.key = key;
      this.sequenceNumber = sequenceNumber;
      this.ins = ins;
    }

    /**
     * Returns an editor for this snapshot's entry, or null if either the
     * entry has changed since this snapshot was created or if another edit
     * is in progress.
     */
    Editor edit() throws IOException {
      return DiskLruCache.this.edit(key, sequenceNumber);
    }

    /** Returns the unbuffered stream with the value for {@code index}. */
    InputStream getInputStream(int index) {
      return ins[index];
    }

    /** Returns the string value for {@code index}. */
    byte[] getBytes(int index) throws IOException {
      return inputStreamToBytes(getInputStream(index));
    }

    @Override public void close() {
      for (InputStream in : ins) {
        closeQuietly(in);
      }
    }
  }

  /** Edits the values for an entry. */
  final class Editor {
    private final Entry entry;
    private final boolean[] written;
    private boolean hasErrors;

    private Editor(Entry entry) {
      this.entry = entry;
      this.written = (entry.readable) ? null : new boolean[valueCount];
    }

    /**
     * Returns an unbuffered input stream to read the last committed value,
     * or null if no value has been committed.
     */
    InputStream newInputStream(int index) throws IOException {
      synchronized (DiskLruCache.this) {
        if (entry.currentEditor != this) {
          throw new IllegalStateException();
        }
        if (!entry.readable) {
          return null;
        }
        return new FileInputStream(entry.getCleanFile(index));
      }
    }

    /**
     * Returns the last committed value as a string, or null if no value
     * has been committed.
     */
    byte[] getBytes(int index) throws IOException {
      InputStream in = newInputStream(index);
      return in != null ? inputStreamToBytes(in) : null;
    }

    /**
     * Returns a new unbuffered output stream to write the value at
     * {@code index}. If the underlying output stream encounters errors
     * when writing to the filesystem, this edit will be aborted when
     * {@link #commit} is called. The returned output stream does not throw
     * IOExceptions.
     */
    OutputStream newOutputStream(int index) throws IOException {
      synchronized (DiskLruCache.this) {
        if (entry.currentEditor != this) {
          throw new IllegalStateException();
        }
        if (!entry.readable) {
          written[index] = true;
        }
        return new FaultHidingOutputStream(new FileOutputStream(entry.getDirtyFile(index)));
      }
    }

    /** Sets the value at {@code index} to {@code value}. */
    void set(int index, byte[] value) throws IOException {
      OutputStream output = null;
      try {
        output = newOutputStream(index);
        output.write(value);
      } finally {
        closeQuietly(output);
      }
    }

    /**
     * Commits this edit so it is visible to readers.  This releases the
     * edit lock so another edit may be started on the same key.
     */
    void commit() throws IOException {
      if (hasErrors) {
        completeEdit(this, false);
        remove(entry.key); // the previous entry is stale
      } else {
        completeEdit(this, true);
      }
    }

    /**
     * Aborts this edit. This releases the edit lock so another edit may be
     * started on the same key.
     */
    void abort() throws IOException {
      completeEdit(this, false);
    }

    private final class FaultHidingOutputStream extends FilterOutputStream {
      private FaultHidingOutputStream(OutputStream out) {
        super(out);
      }

      @Override public void write(int oneByte) {
        try {
          out.write(oneByte);
        } catch (IOException e) {
          hasErrors = true;
        }
      }

      @Override public void write(byte[] buffer, int offset, int length) {
        try {
          out.write(buffer, offset, length);
        } catch (IOException e) {
          hasErrors = true;
        }
      }

      @Override public void close() {
        try {
          out.close();
        } catch (IOException e) {
          hasErrors = true;
        }
      }

      @Override public void flush() {
        try {
          out.flush();
        } catch (IOException e) {
          hasErrors = true;
        }
      }
    }
  }

  private final class Entry {
    private final String key;

    /** Lengths of this entry's files. */
    private final long[] lengths;

    /** True if this entry has ever been published. */
    private boolean readable;

    /** The ongoing edit or null if this entry is not being edited. */
    private Editor currentEditor;

    /** The sequence number of the most recently committed edit to this entry. */
    private long sequenceNumber;

    private Entry(String key) {
      this.key = key;
      this.lengths = new long[valueCount];
    }

    String getLengths() {
      StringBuilder result = new StringBuilder();
      for (long size : lengths) {
        result.append(' ').append(size);
      }
      return result.toString();
    }

    /** Set lengths using decimal numbers like "10123". */
    private void setLengths(String[] strings) throws IOException {
      if (strings.length != valueCount) {
        throw invalidLengths(strings);
      }

      try {
        for (int i = 0; i < strings.length; i++) {
          lengths[i] = Long.parseLong(strings[i]);
        }
      } catch (NumberFormatException e) {
        throw invalidLengths(strings);
      }
    }

    private IOException invalidLengths(String[] strings) throws IOException {
      throw new IOException("unexpected journal line: " + Arrays.toString(strings));
    }

    File getCleanFile(int i) {
      return new File(directory, key + "." + i);
    }

    File getDirtyFile(int i) {
      return new File(directory, key + "." + i + ".tmp");
    }
  }
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Junk drawer of utility methods. */

  /**
   * Closes {@code closeable}, ignoring any checked exceptions. Does nothing
   * if {@code closeable} is null.
   */
  private static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (RuntimeException rethrown) {
        throw rethrown;
      } catch (Exception ignored) {
      }
    }
  }

  /** Recursively delete everything in {@code dir}. */
  // TODO: this should specify paths as Strings rather than as Files
  private static void deleteContents(File dir) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) {
      throw new IllegalArgumentException("not a directory: " + dir);
    }
    for (File file : files) {
      if (file.isDirectory()) {
        deleteContents(file);
      }
      if (!file.delete()) {
        throw new IOException("failed to delete file: " + file);
      }
    }
  }

  /** Returns the remainder of 'reader' as a string, closing it when done. */
  private static byte[] readFully(InputStream input) throws IOException {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buffer = new byte[2048];
      int count;
      while ((count = input.read(buffer)) != -1) {
      output.write(buffer, 0, count);
      }
      return output.toByteArray();
    } finally {
      input.close();
    }
  }

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Buffers input from an {@link InputStream} for reading lines.
 *
 * This class is used for buffered reading of lines. For purposes of this class, a line ends with
 * "\n" or "\r\n". End of input is reported by throwing {@code EOFException}. Unterminated line at
 * end of input is invalid and will be ignored, the caller may use {@code hasUnterminatedLine()}
 * to detect it after catching the {@code EOFException}.
 *
 * This class is intended for reading input that strictly consists of lines, such as line-based
 * cache entries or cache journal. Unlike the {@link BufferedReader} which in conjunction with
 * {@link InputStreamReader} provides similar functionality, this class uses different
 * end-of-input reporting and a more restrictive definition of a line.
 *
 * This class supports only charsets that encode '\r' and '\n' as a single byte with value 13
 * and 10, respectively, and the representation of no other character contains these values.
 * We currently check in constructor that the charset is one of US-ASCII, UTF-8 and ISO-8859-1.
 * The default charset is US_ASCII.
 */
private final class StrictLineReader implements Closeable {
  private static final byte CR = (byte) '\r';
  private static final byte LF = (byte) '\n';

  private final InputStream in;
  private final Charset charset;

  // Buffered data is stored in {@code buf}. As long as no exception occurs, 0 <= pos <= end
  // and the data in the range [pos, end) is buffered for reading. At end of input, if there is
  // an unterminated line, we set end == -1, otherwise end == pos. If the underlying
  // {@code InputStream} throws an {@code IOException}, end may remain as either pos or -1.
  private byte[] buf;
  private int pos;
  private int end;

  /**
   * Constructs a new {@code LineReader} with the specified charset and the default capacity.
   *
   * @param in the {@code InputStream} to read data from.
   * @param charset the charset used to decode data.
   * Only US-ASCII, UTF-8 and ISO-8859-1 is supported.
   * @throws NullPointerException if {@code in} or {@code charset} is null.
   * @throws IllegalArgumentException if the specified charset is not supported.
   */
  StrictLineReader(InputStream in, Charset charset) {
    this(in, 8192, charset);
  }

  /**
   * Constructs a new {@code LineReader} with the specified capacity and charset.
   *
   * @param in the {@code InputStream} to read data from.
   * @param capacity the capacity of the buffer.
   * @param charset the charset used to decode data.
   * Only US-ASCII, UTF-8 and ISO-8859-1 is supported.
   * @throws NullPointerException if {@code in} or {@code charset} is null.
   * @throws IllegalArgumentException if {@code capacity} is negative or zero
   * or the specified charset is not supported.
   */
  StrictLineReader(InputStream in, int capacity, Charset charset) {
    if (in == null || charset == null) {
      throw new NullPointerException();
    }
    if (capacity < 0) {
      throw new IllegalArgumentException("capacity <= 0");
    }
    if (!(charset.equals(US_ASCII) || charset.equals(UTF_8) || charset.equals(ISO_8859_1))) {
      throw new IllegalArgumentException("Unsupported encoding");
    }

    this.in = in;
    this.charset = charset;
    buf = new byte[capacity];
  }

  /**
   * Closes the reader by closing the underlying {@code InputStream} and
   * marking this reader as closed.
   *
   * @throws IOException for errors when closing the underlying {@code InputStream}.
   */
  @Override
  public void close() throws IOException {
    synchronized (in) {
      if (buf != null) {
        buf = null;
        in.close();
      }
    }
  }

  /**
   * Reads the next line. A line ends with {@code "\n"} or {@code "\r\n"},
   * this end of line marker is not included in the result.
   *
   * @return the next line from the input.
   * @throws IOException for underlying {@code InputStream} errors.
   * @throws EOFException for the end of source stream.
   */
  String readLine() throws IOException {
    synchronized (in) {
      if (buf == null) {
        throw new IOException("LineReader is closed");
      }

      // Read more data if we are at the end of the buffered data.
      // Though it's an error to read after an exception, we will let {@code fillBuf()}
      // throw again if that happens; thus we need to handle end == -1 as well as end == pos.
      if (pos >= end) {
        fillBuf();
      }
      // Try to find LF in the buffered data and return the line if successful.
      for (int i = pos; i != end; ++i) {
        if (buf[i] == LF) {
          int lineEnd = (i != pos && buf[i - 1] == CR) ? i - 1 : i;
          String res = new String(buf, pos, lineEnd - pos, charset);
          pos = i + 1;
          return res;
        }
      }

      // Let's anticipate up to 80 characters on top of those already read.
      ByteArrayOutputStream out = new ByteArrayOutputStream(end - pos + 80) {
        @Override
        public String toString() {
          int length = (count > 0 && buf[count - 1] == CR) ? count - 1 : count;
          return new String(buf, 0, length, charset);
        }
      };

      while (true) {
        out.write(buf, pos, end - pos);
        // Mark unterminated line in case fillBuf throws EOFException or IOException.
        end = -1;
        fillBuf();
        // Try to find LF in the buffered data and return the line if successful.
        for (int i = pos; i != end; ++i) {
          if (buf[i] == LF) {
            if (i != pos) {
              out.write(buf, pos, i - pos);
            }
            pos = i + 1;
            return out.toString();
          }
        }
      }
    }
  }

  /**
   * Reads new input data into the buffer. Call only with pos == end or end == -1,
   * depending on the desired outcome if the function throws.
   *
   * @throws IOException for underlying {@code InputStream} errors.
   * @throws EOFException for the end of source stream.
   */
  private void fillBuf() throws IOException {
    int result = in.read(buf, 0, buf.length);
    if (result == -1) {
      throw new EOFException();
    }
    pos = 0;
    end = result;
  }
}
}
