package se.ridorana.roze.stuff.FFXIVDatTools.font;

import se.ridorana.roze.stuff.FFXIVDatTools.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Roze on 2014-08-17.
 *
 */
@SuppressWarnings( {"FieldCanBeLocal", "UnusedDeclaration"} )
public class BitmapFont {

	private final static ConcurrentHashMap<String, Image> imageCache = new ConcurrentHashMap<>();


	//private TextureDataTools.OverlayableImageWrapper[] images;
	private Image[] images;
	private GlyphTable list;
	private DatSegment tableFile;
	private int indexMod = 0;
	private String texFilter = null;

	private IWordBreaker wordBreaker = new IWordBreaker() {
		@Override
		public boolean doBreak(char c) {
			return !Character.isLetterOrDigit(c);
		}
	};

	public BitmapFont(String ftd, String texFilter, final float size) throws IOException, DatSegment.HandlerException, ImageDecoding.ImageDecodingException {
		FileBlockWrapper fbw = IndexReader.getSegmentByPathname(ftd);
		tableFile = fbw.getSegment(true, true);
		list = new GlyphTable(tableFile);
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for (Glyph g : list.getGlyphs()) {
			int index = g.getImageIndex();
			if (index < min) {
				min = index;
			}
			if (index > max) {
				max = index;
			}
		}
		//min-= min % 4;
		//max+= 4 - max % 4;
		indexMod = min;
		this.texFilter = texFilter;
		images = new Image[(max + 1) * 4];
		for (int i = min; i < (max + 1); i++) {
			String path = String.format(texFilter, (i + 1));
			images[i * 4] = getCachedImage(path, TextureDataTools.BGRAChannel.Blue);
			images[i * 4 + 1] = getCachedImage(path, TextureDataTools.BGRAChannel.Green);
			images[i * 4 + 2] = getCachedImage(path, TextureDataTools.BGRAChannel.Red);
			images[i * 4 + 3] = getCachedImage(path, TextureDataTools.BGRAChannel.Alpha);
		}

	}

	protected static Image getCachedImage(String path, TextureDataTools.BGRAChannel channel) throws IOException, DatSegment.HandlerException, ImageDecoding.ImageDecodingException {
		String key = path + "." + channel.getValue();
		if (imageCache.containsKey(key)) {
			return imageCache.get(key);
		} else {
			DatSegment texSegment = IndexReader.getSegmentByPathname(path).getSegment(true, true);
			BufferedImage image = TextureDataTools.getChannel(texSegment, channel);
			imageCache.put(key, image);
			return image;
		}
	}

	public Image getImage(Glyph glyph) {
		return images[(glyph.getImageIndex() << 2) + glyph.getChannel()];
	}

	public int getWordLength(String s, int offset, int length, IWordBreaker breaker) {
		int w = 0;
		if (length == 0) {
			length = s.length() - offset;
		}
		for (int i = offset; i < length; i++) {
			char c = s.charAt(i);
			if (breaker != null && breaker.doBreak(c)) {
				break;
			} else {
				Glyph glyph = list.getGlyphForChar(c);
				w += glyph.getW();
			}
		}
		return w;
	}

	/**
	 * @param g
	 * @param s
	 * @param x
	 * @param y
	 * @param color
	 */
	public Dimension drawString(Graphics g, String s, int x, int y, Color color) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		BufferedImage image = stringToImage(s, color);
		if ( image != null ) {
			g2d.drawImage(image, x, y, null);
			return new Dimension(image.getWidth(), image.getHeight());
		}
		return new Dimension(0, 0);
	}

	public Dimension stringBounds(String str) {
		return stringBounds(str.toCharArray());
	}

	public Dimension stringBounds(char[] str) {
		int w = 0, h = 0;
		for (char c : str) {
			Glyph g = list.getGlyphForChar(c);
			w += g.w + g.xPreOff + g.xPostOff;
			if (g.h > h) {
				h = g.h;
			}
		}
		return new Dimension(w, h);
	}

	protected BufferedImage stringToImage(String str, Color color) {
		char[] chars = str.toCharArray();
		Dimension bounds = stringBounds(chars);
		if ( bounds.width > 0 && bounds.height > 0 ) {
			bounds.width += 2;
			BufferedImage image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_4BYTE_ABGR);
			Graphics graphics = image.createGraphics();
			//((Graphics2D)graphics).setComposite(AlphaComposite.SrcIn);
			graphics.setColor(new Color(0, 0, 0, 0));
			graphics.fillRect(0, 0, bounds.width, bounds.height);
			int x = 0, y = 0;
			for ( char c : chars ) {
				Glyph glyph = list.getGlyphForChar(c);
				int gw = glyph.w;
				int gh = glyph.h;
				int gx = glyph.x;
				int gy = glyph.y;
				int mod = glyph.xPostOff;
				graphics.drawImage(getImage(glyph), x + mod, y - glyph.yOffs, x + gw + mod, y + gh - glyph.yOffs, gx, gy, gx + gw, gy + gh, null);
				x += gw + glyph.xPreOff + mod;
			}
			graphics.setColor(color);
			((Graphics2D)graphics).setComposite(AlphaComposite.SrcIn);
			graphics.fillRect(0, 0, bounds.width, bounds.height);
			graphics.dispose();
			return image;
		}
		return null;
	}

	/**
	 * @param g
	 * @param text
	 * @param x
	 * @param y
	 * @param color
	 * @param justification
	 */
	public void drawStringLFAware(Graphics g, String text, int x, int y, Color color, Justification justification) {
		String[] strings = text.split("\r*\n");
		BufferedImage[] bufferedImages = new BufferedImage[strings.length];
		int i = 0;
		int maxX = 0;
		int maxY = 0;
		for (String m : strings) {
			bufferedImages[i] = stringToImage(m, color);
			if (bufferedImages[i].getWidth() > maxX) {
				maxX = bufferedImages[i].getWidth();
			}
			maxY += bufferedImages[i].getHeight();
			i++;
		}
		int localY = y;
		for (BufferedImage image : bufferedImages) {
			int localX = x;
			switch (justification) {
				case Center: {
					localX += maxX / 2 - image.getWidth() / 2;
					break;
				}
				case Right: {
					localX += maxX - image.getWidth();
					break;
				}
			}
			g.drawImage(image, localX, localY, null);
			localY += image.getHeight();
		}
	}


	public static enum Justification {
		Left, Right, Center
	}

	/**
	 *
	 */
	private interface IWordBreaker {
		public boolean doBreak(char c);
	}

	/**
	 *
	 */
	public static class SizableBitmapFont {

		private final Map<Float, BitmapFont> sizeToFontAssociations = new ConcurrentHashMap<>();
		private TreeSet<Float> availableSizes = new TreeSet<>();

		/**
		 * @param fontNameFilter
		 * @param texFilter
		 * @param sizeKeys
		 * @throws ImageDecoding.ImageDecodingException
		 * @throws DatSegment.HandlerException
		 * @throws IOException
		 */
		public SizableBitmapFont(String fontNameFilter, String texFilter, float... sizeKeys) throws ImageDecoding.ImageDecodingException, DatSegment.HandlerException, IOException {
			String[] names = new String[sizeKeys.length];
			int i = 0;
			for ( float f : sizeKeys ) {
				availableSizes.add(f);
				names[i] = String.format(fontNameFilter, sizeToString(f));
				sizeToFontAssociations.put(f, new BitmapFont(names[i], texFilter, (int)f));
			}
		}

		/**
		 * @param set
		 * @param item
		 * @param <T>
		 * @return
		 */
		public static <T> T getBestMatch(SortedSet<T> set, T item) {
			if ( set.contains(item) ) {
				return item;
			}
			SortedSet<T> tail = set.tailSet(item);
			if ( tail.isEmpty() ) {
				return set.last();
			}
			return tail.first();
		}

		/**
		 * @param set
		 * @param item
		 * @param mode
		 * @return
		 */
		public static float getClosest(NavigableSet<Float> set, float item, ClosestMode mode) {
			switch ( mode ) {
				case Upper: {
					if ( set.contains(item) ) {
						return item;
					}
					final Float higher = set.higher(item);
					return higher == null ? set.last() : higher;
				}
				case Lower: {
					if ( set.contains(item) ) {
						return item;
					}
					final Float lower = set.lower(item);
					return lower == null ? set.first() : lower;
				}
				case Closest:
				default: {
					if ( set.contains(item) ) {
						return item;
					}
					final Float higher = set.higher(item);
					final Float lower = set.lower(item);
					if ( lower != null ) {
						if ( higher != null ) {
							return (higher - item > item - lower) ? lower : higher;
						} else {
							return lower;
						}
					} else {
						return higher;
					}
				}
			}
		}

		public Dimension stringBounds(String str, float size) {
			return stringBounds(str.toCharArray(), size);
		}

		public Dimension stringBounds(char[] str, float size) {
			int w = 0, h = 0;
			BitmapFont bf = getClosestFont(size);
			for ( char c : str ) {
				Glyph g = bf.list.getGlyphForChar(c);
				w += g.w + g.xPreOff + g.xPostOff;
				if ( g.h > h ) {
					h = g.h;
				}
			}
			return new Dimension(w, h);
		}

		/**
		 * @param size
		 * @return
		 */
		private int sizeToString(float size) {
			if ( (float)((int)size) == size ) {
				return (int)size;
			} else {
				return Integer.parseInt(Float.toString(size).replaceAll("[.,]", ""));
			}
		}

		/**
		 * @param size
		 * @return
		 */
		private BitmapFont getClosestFont(float size) {
			return sizeToFontAssociations.get(getClosest(availableSizes, size, ClosestMode.Lower));
		}

		/**
		 * @param g
		 * @param text
		 * @param x
		 * @param y
		 * @param color
		 * @param size
		 */
		public Dimension drawString(Graphics g, String text, int x, int y, Color color, int size) {
			return getClosestFont(size).drawString(g, text, x, y, color);
		}

		/**
		 * @param g
		 * @param text
		 * @param x
		 * @param y
		 * @param color
		 * @param size
		 * @param justification
		 */
		public void drawStringLFAware(Graphics g, String text, int x, int y, Color color, int size, Justification justification) {
			getClosestFont(size).drawStringLFAware(g, text, x, y, color, justification);
		}

		/**
		 *
		 */
		public static enum ClosestMode {
			Upper, Lower, Closest
		}

	}

}
