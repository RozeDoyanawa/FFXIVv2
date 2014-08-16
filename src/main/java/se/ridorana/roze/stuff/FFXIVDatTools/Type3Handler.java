package se.ridorana.roze.stuff.FFXIVDatTools;

import se.ridorana.roze.stuff.FFXIVDatTools.DatSegment.TypeHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.InvalidParameterException;
import java.util.List;

/**
 * @author Roze
 */
public class Type3Handler implements TypeHandler {

	private class BinaryType implements TypeData, ISearchable {


		@Override
		public List<Long> find(DatSegment segment, byte[] searcher) {
			return null;
		}
	}

	private static final int TYPE = 3;

	@Override
	public int getType() {
		return TYPE;
	}

	@Override
	public void readType(final DatSegment segment, final FileChannel channel) throws IOException {
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
	}

}
