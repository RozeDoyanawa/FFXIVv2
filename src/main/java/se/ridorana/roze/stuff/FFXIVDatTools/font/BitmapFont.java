package se.ridorana.roze.stuff.FFXIVDatTools.font;

import se.ridorana.roze.stuff.FFXIVDatTools.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Roze on 2014-08-17.
 */
public class BitmapFont {


	//private TextureDataTools.OverlayableImageWrapper[] images;

	private final static ConcurrentHashMap<String, Image> imageCache = new ConcurrentHashMap<>();

	private Image[] images;

	private GlyphList list;

	private DatSegment tableFile;

	private int indexMod = 0;

	public Image getImage(Glyph glyph) {
		return images[(glyph.getImageIndex() << 2) + glyph.getChannel()];
	}

	private String texFilter = null;

	public BitmapFont(String ftd, String texFilter) throws IOException, DatSegment.HandlerException, ImageDecoding.ImageDecodingException {
		FileBlockWrapper fbw = IndexReader.getSegmentByPathname(ftd);
		tableFile = fbw.getSegment(true, true);
		list = new GlyphList(tableFile);
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

	private interface C {
		public boolean doBreak(char c);
	}

	private C nonWordBreaker = new C() {
		@Override
		public boolean doBreak(char c) {
			if (!Character.isLetterOrDigit(c)) {
				return true;
			}
			return false;
		}
	};

	public int getWordLength(String s, int offset, int length, C breaker) {
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
		g2d.drawImage(image, x, y, null);
		return new Dimension(image.getWidth(), image.getHeight());
	}

	protected Dimension stringBounds(String str) {
		return stringBounds(str.toCharArray());
	}

	protected Dimension stringBounds(char[] str) {
		int w = 0, h = 0;
		for (char c : str) {
			Glyph g = list.getGlyphForChar(c);
			w += g.w + g.xoff + g.yoff;
			if (g.h > h) {
				h = g.h;
			}
		}
		return new Dimension(w, h);
	}


	protected BufferedImage stringToImage(String str, Color color) {
		char[] chars = str.toCharArray();
		Dimension bounds = stringBounds(chars);
		BufferedImage image = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_4BYTE_ABGR);
		Graphics graphics = image.createGraphics();
		//((Graphics2D)graphics).setComposite(AlphaComposite.SrcIn);
		graphics.setColor(new Color(0, 0, 0, 0));
		graphics.fillRect(0, 0, bounds.width, bounds.height);
		int x = 0, y = 0;
		for (char c : chars) {
			Glyph glyph = list.getGlyphForChar(c);
			int gw = glyph.w;
			int gh = glyph.h;
			int gx = glyph.x;
			int gy = glyph.y;
			int mod = glyph.yoff;
			graphics.drawImage(getImage(glyph), x + mod, y, x + gw + mod, y + gh, gx, gy, gx + gw, gy + gh, null);
			x += gw + glyph.xoff + mod;
		}
		graphics.setColor(color);
		((Graphics2D) graphics).setComposite(AlphaComposite.SrcIn);
		graphics.fillRect(0, 0, bounds.width, bounds.height);
		graphics.dispose();
		return image;
	}

	public static enum Justification {
		Left, Right, Center
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

}
