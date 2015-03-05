package se.ridorana.roze.stuff.FFXIVDatTools;

import se.ridorana.roze.stuff.FFXIVDatTools.DatSegment.TypeHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Roze
 */
public class Type2Handler implements TypeHandler {

	public class BinaryType implements TypeData, ISearchable {


		@Override
		public List<Long> find(DatSegment segment, byte[] searcher) {
			return _find(segment, searcher, true);
		}


		public String toString(final DatSegment segment) {
			return Arrays.toString(segment.getData());
		}
	}


	public static final int TYPE = 2;


	public static class KPM {
		/**
		 * Search the data byte array for the first occurrence of the byte array pattern within given boundaries.
		 *
		 * @param data
		 * @param start   First index in data
		 * @param stop    Last index in data so that stop-start = length
		 * @param pattern What is being searched. '*' can be used as wildcard for "ANY character"
		 * @return
		 */
		public static int indexOf(byte[] data, int start, int stop, byte[] pattern) {
			if (data == null || pattern == null) {
				return -1;
			}

			int[] failure = computeFailure(pattern);

			int j = 0;

			for (int i = start; i < stop; i++) {
				while (j > 0 && (pattern[j] != '*' && pattern[j] != data[i])) {
					j = failure[j - 1];
				}
				if (pattern[j] == '*' || pattern[j] == data[i]) {
					j++;
				}
				if (j == pattern.length) {
					return i - pattern.length + 1;
				}
			}
			return -1;
		}

		/**
		 * Computes the failure function using a boot-strapping process,
		 * where the pattern is matched against itself.
		 */
		private static int[] computeFailure(byte[] pattern) {
			int[] failure = new int[pattern.length];

			int j = 0;
			for (int i = 1; i < pattern.length; i++) {
				while (j > 0 && pattern[j] != pattern[i]) {
					j = failure[j - 1];
				}
				if (pattern[j] == pattern[i]) {
					j++;
				}
				failure[i] = j;
			}

			return failure;
		}
	}

	public static final List<Long> _find(final DatSegment segment, final byte[] searcher, final boolean all) {
		final byte[] data = segment.getData();
		List<Long> list = null;
		int mod = 0;
		while ((mod = KPM.indexOf(data, mod, data.length, searcher)) >= 0) {
			if (list == null) {
				list = new LinkedList<>();
			}
			list.add((long) mod);
			mod++;
		}
		/*for(int i = 0; i < data.length; i++){
			if(data[i] == searcher[mod]){
				mod++;
				if(mod == searcher.length){
					if (list == null) {
						list = new ArrayList<>();
					}
					list.add((long)i);
				}
			}else if(mod > 0){
				i--; mod = 0;
			}
		}*/
		return list;
	}


	@Override
	public int getType() {
		return TYPE;
	}

	@Override
	public int readType(final DatSegment segment, final FileChannel channel) throws IOException {
		if (segment.getType() != TYPE) {
			throw new InvalidParameterException("Invalid type for this handler");
		}
		final int headerLength = segment.getHeaderLength();
		final ByteBuffer bb = channel.map(MapMode.READ_ONLY, segment.getOffset(), headerLength);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.position(8); // fseek($fpIn, $address + 0x08, SEEK_SET);
		segment.setUncompressedSize(bb.getInt());
		bb.position(20); // fseek($fpIn, $address + 24, SEEK_SET);
		// $header['extraHeaderLength'] = readInt32($fpIn);
		segment.setFrameCount(bb.getInt());
		segment.setFirstDataFrame(segment.getOffset() + headerLength);

		switch (headerLength) {
			case 128: {

			}
		}

		segment.setTypeData(new BinaryType());
		return headerLength;
	}

}
