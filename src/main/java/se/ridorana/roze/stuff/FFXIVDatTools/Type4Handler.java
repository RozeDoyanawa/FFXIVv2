package se.ridorana.roze.stuff.FFXIVDatTools;

import se.ridorana.roze.stuff.FFXIVDatTools.DatSegment.TypeHandler;
import se.ridorana.roze.stuff.FFXIVDatTools.ImageDecoding.ImageDecodingException;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.InvalidParameterException;
import java.util.Hashtable;

/**
 * @author Roze
 */
public class Type4Handler implements TypeHandler {

	public static final int VALUE_4444_CHANNEL_RED = 0;
	public static final int VALUE_4444_CHANNEL_GREEN = 1;
	public static final int VALUE_4444_CHANNEL_BLUE = 2;
	public static final int VALUE_4444_CHANNEL_ALPHA = 3;


	/**
	 * @author Roze
	 */
	public static final class TextureData implements TypeData {
		public static final BufferedImage decode(final DatSegment segment) throws ImageDecodingException {
			return decode(segment, null);
		}

		public static final BufferedImage decode(final DatSegment segment, final Hashtable<String, Object> parameters) throws ImageDecodingException {
			if (segment.getType() != TYPE) {
				throw new InvalidParameterException("Type cannot be other than 4");
			}
			final byte[] data = segment.getData();
			if (data == null) {
				throw new NullPointerException("Data is null");
			}
			final TextureData td = ((Type4Handler.TextureData) segment.getTypeData());
			switch (td.textureCompressionType) {
				case 32: {
					return ImageDecoding.decodeImageDX1(
							data, td.uncompressedWidth, td.uncompressedHeight, td.uncompressedWidth / 4,
							td.uncompressedHeight / 4
					);
				}
				case 48: {
					return ImageDecoding.decodeImageRaw(data, td.uncompressedWidth, td.uncompressedHeight, 0, 0);
				}
				case 49: {
					return ImageDecoding.decodeImageDX5(
							data, td.uncompressedWidth, td.uncompressedHeight, td.uncompressedWidth / 4,
							td.uncompressedHeight / 4
					);
				}
				case 64: {
					if (parameters != null) {
						if (parameters.containsKey("4444.channel")) {
							Object q = parameters.get("4444.channel");
							return ImageDecoding.decodeImage4444split1channel(
									data, td.uncompressedWidth, td.uncompressedHeight, 0, 0,
									(q instanceof Integer ? (Integer) q : 0)
							);
						}
						if (parameters.containsKey("1008.4444.mergedSplit") && parameters.get(
								"1008.4444.mergedSplit"
						).equals(ImageDecoding.ON_VALUE)) {
							return ImageDecoding.decodeImage4444split(
									data, td.uncompressedWidth, td.uncompressedHeight, 0, 0
							);
						}
					}
					return ImageDecoding.decodeImage4444(data, td.uncompressedWidth, td.uncompressedHeight, 0, 0);
				}
				case 65: {
					return ImageDecoding.decodeImage5551(
							data, td.uncompressedWidth, td.uncompressedHeight, 0, 0, parameters
					);
				}
				case 80: {
					return ImageDecoding.decodeImageRGBA(data, td.uncompressedWidth, td.uncompressedHeight, 0, 0);
				}
				case 81: {
					return ImageDecoding.decodeImageRGBA(data, td.uncompressedWidth, td.uncompressedHeight, 0, 0);
				}
			}
			throw new ImageDecodingException("Unsupported format: " + td.textureCompressionType);
		}

		private int uncompressedWidth;

		private int uncompressedHeight;

		private int textureCompressionType;

		/**
		 * @return
		 */
		public int getTextureCompressionType() {
			return textureCompressionType;
		}

		/**
		 * @return
		 */
		public int getUncompressedHeight() {
			return uncompressedHeight;
		}

		/**
		 * @return
		 */
		public int getUncompressedWidth() {
			return uncompressedWidth;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("TextureData[");
			sb.append("type=" + textureCompressionType);
			sb.append(", width=" + uncompressedWidth);
			sb.append(", height=" + uncompressedHeight);
			sb.append("]");
			return sb.toString();
		}

	}

	public static final int TYPE = 4;

	private static final int TYPE4_IMG_HEADER_LENGTH = 80;

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
		final TextureData data = new TextureData();
		final ByteBuffer bb = channel.map(
				MapMode.READ_ONLY, segment.getOffset(), headerLength + TYPE4_IMG_HEADER_LENGTH
		);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		bb.position(8); // fseek($fpIn, $address + 0x08, SEEK_SET);
		segment.setUncompressedSize(bb.getInt()); // $header['uncompressedSize']
		// =
		// readInt32($fpIn);
		bb.position(24); // fseek($fpIn, $address + 24, SEEK_SET);
		// $header['extraHeaderLength'] = readInt32($fpIn);
		segment.setFirstDataFrame(segment.getOffset() + headerLength + TYPE4_IMG_HEADER_LENGTH);
		bb.position(headerLength + 8);
		data.textureCompressionType = bb.get(headerLength + 4);
		data.uncompressedWidth = bb.getShort();
		data.uncompressedHeight = bb.getShort();
		switch (segment.getHeaderLength()) {
			case 364: {
				bb.position(216);
				segment.setFrameCount(bb.getInt());
				break;
			}
			default: {
				bb.position(20);
				segment.setFrameCount(bb.getInt());
			}
		}
		segment.setTypeData(data);
	}

}
