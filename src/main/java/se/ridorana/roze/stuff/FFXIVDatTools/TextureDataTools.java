package se.ridorana.roze.stuff.FFXIVDatTools;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.RGBImageFilter;
import java.util.HashMap;

/**
 * Created by Roze on 2014-08-17.
 */
public class TextureDataTools {


	public static enum BGRAChannel {
		Blue(Type4Handler.VALUE_4444_CHANNEL_BLUE), Green(Type4Handler.VALUE_4444_CHANNEL_GREEN), Red(
				Type4Handler.VALUE_4444_CHANNEL_RED
		), Alpha(Type4Handler.VALUE_4444_CHANNEL_ALPHA);

		int value;

		public int getValue() {
			return value;
		}

		BGRAChannel(int value) {
			this.value = value;
		}
	}

	public static BufferedImage getChannel(DatSegment segment, BGRAChannel channel) throws ImageDecoding.ImageDecodingException {
		HashMap<String, Object> imageLoadParameters = new HashMap<>();
		imageLoadParameters.put(ImageDecoding.GRAYSCALE_TRANSPARENCY_D5551_KEY, ImageDecoding.OFF_VALUE);
		imageLoadParameters.put("1008.4444.mergedSplit", ImageDecoding.ON_VALUE);
		imageLoadParameters.put("4444.channel", channel.value);
		return Type4Handler.TextureData.decode(segment, imageLoadParameters);
	}

	public static class OverlayableImageWrapper {
		OverlayFilter filter;
		Image image;

		public OverlayFilter getFilter() {
			return filter;
		}

		public Image getImage() {
			return image;
		}

		public OverlayableImageWrapper(OverlayFilter filter, Image image) {
			this.filter = filter;
			this.image = image;
		}
	}

	/**
	 *
	 */
	public static class OverlayFilter extends RGBImageFilter {

		Color base = Color.black;

		/**
		 * @return
		 */
		public Color getBase() {
			return base;
		}

		/**
		 * @param base
		 */
		public void setBase(Color base) {
			this.base = base;
		}

		@Override
		public int filterRGB(int x, int y, int rgb) {
			return (rgb & 0xff000000) | (base.getRGB() & 0x00ffffff);
		}
	}

	/**
	 * @param segment
	 * @param channel
	 * @return
	 * @throws ImageDecoding.ImageDecodingException
	 */
	public static OverlayableImageWrapper getOverlaySupportedChannel(DatSegment segment, BGRAChannel channel) throws ImageDecoding.ImageDecodingException {
		BufferedImage image = getChannel(segment, channel);
		OverlayFilter of = new OverlayFilter();
		FilteredImageSource fis = new FilteredImageSource(image.getSource(), of);
		Image i = Toolkit.getDefaultToolkit().createImage(fis);
		return new OverlayableImageWrapper(of, i);
	}
}
