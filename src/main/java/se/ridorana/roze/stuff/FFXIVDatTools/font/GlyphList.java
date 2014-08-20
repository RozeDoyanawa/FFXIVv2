package se.ridorana.roze.stuff.FFXIVDatTools.font;

import se.ridorana.roze.stuff.FFXIVDatTools.DatSegment;
import se.ridorana.roze.stuff.FFXIVDatTools.IndexReader;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Roze on 2014-08-17.
 */
public class GlyphList {
	List<Glyph> glyphs = new ArrayList<>();
	ConcurrentHashMap<Character, Glyph> charmap = new ConcurrentHashMap<>();

	DatSegment source;

	private int imageWidth;

	private int imageHeight;

	private static final Glyph DEFAULT_UNKNOWN_GLYPH = new Glyph(' ', (short) 0, 0, 0, 0, 0, 0, 0);

	private Glyph unknownGlyph = DEFAULT_UNKNOWN_GLYPH;

	public GlyphList(DatSegment segment) throws UnsupportedEncodingException {
		if (!segment.isDataLoaded()) {
			throw new RuntimeException("Segment data not loaded");
		}
		this.source = segment;
		byte[] data = segment.getData();
		String mime = new String(data, 0, 8);
		if (!mime.equals("fcsv0100")) {
			throw new RuntimeException(
					"Invalid segment in file " + IndexReader.FileBlock.getSearchPath(segment.getSourceBlock())
			);
		}
		ByteBuffer map = MappedByteBuffer.wrap(data);
		map.order(ByteOrder.LITTLE_ENDIAN);
		int count = map.getInt(36);
		imageWidth = map.getShort(32 + 16);
		imageHeight = map.getShort(32 + 18);
		byte[] bytes = new byte[4];
		for (int i = 0; i < count; i++) {
			int location = 64 + i * 16;
			bytes[0] = map.get(location + 3);
			bytes[1] = map.get(location + 2);
			bytes[2] = map.get(location + 1);
			bytes[3] = map.get(location + 0);
			int mod = 0;
			if (bytes[2] == 0) {
				mod = 3;
			} else if (bytes[1] == 0) {
				mod = 2;
			} else if (bytes[0] == 0) {
				mod = 1;
			}
			Glyph g = new Glyph(
					new String(bytes, mod, 4 - mod, "UTF-8").charAt(0), map.getShort(location + 6),
					map.getShort(location + 8), map.getShort(location + 10), map.get(location + 12),
					map.get(location + 13), map.get(location + 14), map.get(location + 15)
			);
			if (g.character == ' ') {
				unknownGlyph = g;
			}
			glyphs.add(g);
			charmap.put(g.character, g);
		}

	}

	public List<Glyph> getGlyphs() {
		return glyphs;
	}

	public DatSegment getSource() {
		return source;
	}

	public int getImageHeight() {
		return imageHeight;
	}

	public int getImageWidth() {
		return imageWidth;
	}

	/**
	 * @param c
	 * @return
	 */
	public Glyph getGlyphForChar(char c) {
		Glyph g = charmap.get(c);
		return (g == null ? unknownGlyph : g);
	}

	@Override
	public String toString() {
		return "GlyphList=(" + glyphs.toString() + ")";
	}
}
