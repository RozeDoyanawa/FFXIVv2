package se.ridorana.roze.stuff.FFXIVDatTools.font;

/**
 *
 */
public class Glyph {
	int channel;

	char character;

	int x;
	int y;
	int w;
	int h;
	int xoff;
	int yoff;
	int imageIndex;

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getW() {
		return w;
	}

	public void setW(int w) {
		this.w = w;
	}

	public int getH() {
		return h;
	}

	public void setH(int h) {
		this.h = h;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		this.channel = channel;
	}

	public int getImageIndex() {
		return imageIndex;
	}

	public void setImageIndex(int index) {
		this.imageIndex = index;
	}

	public char getCharacter() {
		return character;
	}

	public int getXoff() {
		return xoff;
	}

	public int getYoff() {
		return yoff;
	}

	Glyph(char chr, short imageIndex, int x, int y, int w, int h, int xoff, int yoff) {
		this.channel = imageIndex % 4; // 0 == Blue, 1 = Green, 2 = Red, 3 = Alpha
		this.imageIndex = imageIndex >> 2;
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		this.character = chr;
		this.xoff = xoff;
		this.yoff = yoff;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Glpyh=(x=");
		sb.append(x);
		sb.append(",y=");
		sb.append(y);
		sb.append(",w=");
		sb.append(w);
		sb.append(",h=");
		sb.append(h);
		sb.append(",something=");
		sb.append(imageIndex);
		sb.append(",c=");
		sb.append(character);
		sb.append(")");
		return sb.toString();
	}
}
