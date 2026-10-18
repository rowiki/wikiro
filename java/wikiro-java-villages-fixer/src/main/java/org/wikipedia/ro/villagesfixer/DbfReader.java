package org.wikipedia.ro.villagesfixer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal dBase (.dbf) reader returning every non-deleted record as a header-to-trimmed-text map. */
final class DbfReader {

    private static final int HEADER_TERMINATOR = 0x0D;
    private static final int DELETED_FLAG = 0x2A;

    private DbfReader() {
    }

    static List<Map<String, String>> read(Path file, Charset charset) throws IOException {
        ByteBuffer buf = ByteBuffer.wrap(Files.readAllBytes(file)).order(ByteOrder.LITTLE_ENDIAN);
        int recordCount = buf.getInt(4);
        int headerLength = buf.getShort(8) & 0xFFFF;
        int recordLength = buf.getShort(10) & 0xFFFF;

        List<String> names = new ArrayList<>();
        List<Integer> lengths = new ArrayList<>();
        for (int pos = 32; pos < headerLength && (buf.get(pos) & 0xFF) != HEADER_TERMINATOR; pos += 32) {
            int nameEnd = pos;
            while (nameEnd < pos + 11 && buf.get(nameEnd) != 0) {
                nameEnd++;
            }
            names.add(new String(buf.array(), pos, nameEnd - pos, charset));
            lengths.add(buf.get(pos + 16) & 0xFF);
        }

        List<Map<String, String>> records = new ArrayList<>(recordCount);
        for (int i = 0; i < recordCount; i++) {
            int pos = headerLength + i * recordLength;
            if ((buf.get(pos) & 0xFF) == DELETED_FLAG) {
                continue;
            }
            pos++;
            Map<String, String> row = new LinkedHashMap<>();
            for (int f = 0; f < names.size(); f++) {
                row.put(names.get(f), new String(buf.array(), pos, lengths.get(f), charset).trim());
                pos += lengths.get(f);
            }
            records.add(row);
        }
        return records;
    }
}
