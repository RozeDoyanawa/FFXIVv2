package se.ridorana.roze.stuff.FFXIVDatTools;

import se.ridorana.roze.stuff.FFXIVDatTools.IndexReader.FileBlock;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Hashtable;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class DatSegment {


	/**
	 * @author Roze
	 */
	private static final class FrameInfo {
		private final int consumedOut;
		private final int consumedIn;

		public FrameInfo(final int consumedOut, final int consumedIn) {
			this.consumedOut = consumedOut;
			this.consumedIn = consumedIn;
		}
	}

	/**
	 * @author Roze
	 */
	public static final class HandlerException extends Exception {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Constructs a new HandlerException with a given error message
		 *
		 * @param message The message
		 */
		public HandlerException(final String message) {
			super(message);
		}

	}

	/**
	 * @author Roze
	 */
	public static final class InvalidOffsetException extends IOException {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

	}

	/**
	 * @author Roze
	 */
	public static interface TypeHandler {
		public int getType();

		/**
		 * This message should handle reading extended header processing for the
		 * given segment
		 *
		 * @param segment
		 * @param channel
		 * @throws IOException
		 */
		public int readType(DatSegment segment, FileChannel channel) throws IOException;
	}

	private static final Hashtable<Integer, TypeHandler> types = new Hashtable<>();

	private static final long MAX_FRAME_SIZE = 32016;

	/**
	 * Init default handlers
	 */
	static {
		try {
			addType(new Type4Handler());
			addType(new Type2Handler());
			addType(new Type3Handler());
		} catch (final HandlerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add a new Type handler, throws HandlerException if a handler for the type
	 * value is allready registered
	 *
	 * @param handler The new handler
	 * @throws HandlerException
	 */
	public static final void addType(final TypeHandler handler) throws HandlerException {
		final int type = handler.getType();
		if (types.containsKey(type)) {
			throw new HandlerException("Handler for type " + type + " allready registerd, unregister first");
		}
		types.put(type, handler);
	}

	/**
	 * Load a Channel to the underlying file
	 *
	 * @param segment
	 * @return
	 * @throws FileNotFoundException
	 */
	private static FileChannel getFileChannel(final DatSegment segment) throws FileNotFoundException {
		if (segment.path != null) {
			final RandomAccessFile raf = new RandomAccessFile(segment.path, "r");
			return raf.getChannel();
		} else {
			throw new NullPointerException("Segment data path cannot be null when dataStream is null");
		}
	}

	/**
	 * Unregister a type handler for the given type value
	 *
	 * @param type
	 */
	public static final void removeType(final Integer type) {
		types.remove(type);
	}

	private final String path;

	private final long offset;

	private int frameCount;

	private int headerLength;

	private int type;

	private long firstDataFrame = -1;

	private TypeData typeData = null;

	private long uncompressedSize = -1;

	private byte[] data;

	private byte[] rawHeader;

	private FileBlock sourceBlock;

	private boolean dataLoaded;

	private boolean headersLoaded;

	/**
	 * Constructs a new dat-segment from the given file block
	 *
	 * @param datFilepath
	 * @param block
	 */
	public DatSegment(final String datFilepath, final FileBlock block) {
		this.path = datFilepath;
		this.sourceBlock = block;
		this.offset = FileBlock.getOffset(block);
	}

	/**
	 * Constructs a new dat-segment based on a custom offset
	 *
	 * @param datFilepath
	 * @param offset
	 */
	public DatSegment(final String datFilepath, final long offset) {
		this.path = datFilepath;
		this.offset = offset;
	}

	/**
	 * Returns the data-array this segment has with it.
	 * <p/>
	 * Returns null if no data or {@link DatSegment#loadData(FileChannel)} has
	 * not been called prior to this.
	 *
	 * @return byte[] with this segments data
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * @return
	 */
	public int getFrameCount() {
		return frameCount;
	}

	/**
	 * @return
	 */
	public int getHeaderLength() {
		return headerLength;
	}

	public long getOffset() {
		return offset;
	}

	public String getPath() {
		return path;
	}

	/**
	 * @return
	 */
	public FileBlock getSourceBlock() {
		return sourceBlock;
	}

	public int getType() {
		return type;
	}

	public TypeData getTypeData() {
		return typeData;
	}

	public long getUncompressedSize() {
		return uncompressedSize;
	}

	public boolean isDataLoaded() {
		return dataLoaded;
	}

	public boolean isHeadersLoaded() {
		return headersLoaded;
	}

	/**
	 * @param dataStream
	 * @throws IOException
	 */
	public void loadData(FileChannel dataStream) throws IOException {
		if (dataStream == null) {
			dataStream = getFileChannel(this);
		}
		if (uncompressedSize < 0) {
			throw new NullPointerException("This segment has no data length assosiated with it");
		}
		final byte[] data = new byte[(int) uncompressedSize];
		int dataOffset = 0;
		long frameOffset = firstDataFrame;
		FrameInfo frame;
		do {
			frame = readFrame(data, dataOffset, frameOffset, dataStream);
			if (frame != null) {
				final int padding = (128 - (((int) frame.consumedIn) % 128));
				frameOffset += frame.consumedIn + (padding < 128 ? padding : 0);
				dataOffset += frame.consumedOut;
			} else {
				break;
			}
		} while (frame != null);
		this.data = data;
		this.dataLoaded = true;
	}

	/**
	 * @param dataStream
	 * @throws IOException
	 * @throws HandlerException
	 */
	public void loadHeader(FileChannel dataStream) throws IOException, HandlerException {
		if (dataStream == null) {
			dataStream = getFileChannel(this);
		}
		if (offset > dataStream.size()) {
			throw new InvalidOffsetException();
		}
		final ByteBuffer bb = dataStream.map(MapMode.READ_ONLY, offset, 8);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		final int headerLength = bb.getInt();
		final int headerType = bb.getInt();
		if ((headerLength > 10000) || (headerType < 0) || (headerType > 128)) {
			throw new InvalidOffsetException();
		}
		this.headerLength = headerLength;
		this.type = headerType;
		this.headersLoaded = true;
		if (types.containsKey(headerType)) {
			types.get(headerType).readType(this, dataStream);
		} else {
			throw new HandlerException(
					"No handler for type " + headerType + " was found, cannot read segment, position: " + offset
			);
		}
		if ( IndexReader.isStoreHeaderData() ) {
			final ByteBuffer bb2 = dataStream.map(MapMode.READ_ONLY, offset, headerLength);
			this.rawHeader = new byte[headerLength];
			bb2.get(this.rawHeader);
		}
	}

	public byte[] getRawHeader() {
		if ( rawHeader == null ) {
			if ( IndexReader.isStoreHeaderData() ) {
				throw new NullPointerException("Raw header not cached");
			} else {
				throw new NullPointerException("Header is null for unknown reason");
			}
		}
		return rawHeader;
	}

	private FrameInfo readFrame(final byte[] into, final int intoOffset, final long frameLocation, final FileChannel channel) throws IOException {
		final long remaining = channel.size() - frameLocation;
		if (remaining < 128) {
			return null;
		}
		final ByteBuffer bb = channel.map(MapMode.READ_ONLY, frameLocation, Math.min(MAX_FRAME_SIZE, remaining));
		bb.order(ByteOrder.LITTLE_ENDIAN);

		final long frameMagic = bb.getLong();
		if (frameMagic == 16) {
			int compressedSize = bb.getInt();
			final int uncompressedSize = bb.getInt();
			if (compressedSize == 32000) {
				compressedSize = uncompressedSize;
				bb.get(into, intoOffset, compressedSize);
			} else {
				final byte[] compressed = new byte[compressedSize];
				bb.get(compressed);
				final Inflater inf = new Inflater(true);
				inf.setInput(compressed);
				// final byte[] inflateed = new byte[0];
				try {
					if (intoOffset < 0 || uncompressedSize < 0 || intoOffset > into.length - uncompressedSize) {
						throw new InvalidOffsetException();
					}
					inf.inflate(into, intoOffset, uncompressedSize);
				} catch (final DataFormatException e) {
					e.printStackTrace();
				}

			}
			return new FrameInfo(uncompressedSize, compressedSize + 16);
		} else {
			return null;
		}
	}

	void setFirstDataFrame(final long firstDataFrame) {
		this.firstDataFrame = firstDataFrame;
	}

	void setFrameCount(final int frameCount) {
		this.frameCount = frameCount;
	}

	void setTypeData(final TypeData typeData) {
		this.typeData = typeData;
	}

	void setUncompressedSize(final long uncompressedSize) {
		this.uncompressedSize = uncompressedSize;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("DatSegment[");
		sb.append("offset=" + offset);
		sb.append(", type=" + type);
		sb.append(", headerLength=" + headerLength);
		sb.append(", frameCount=" + frameCount);
		sb.append(", uncompressedSize=" + uncompressedSize);
		sb.append(", firstFrame@=" + firstDataFrame);
		if (typeData != null) {
			sb.append(", typeData=" + typeData.toString());
		}
		sb.append("]");
		return sb.toString();
	}

	public void unload() {
		this.data = null;
		this.typeData = null;
		this.dataLoaded = false;
	}

}
