package org.jboss.aesh.terminal;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalSize {

    private int height;
    private int width;


    public TerminalSize(int height, int width) {
        setHeight(height);
        setWidth(width);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        if(width < 1)
            throw new IllegalArgumentException("Terminal width cannot be less than 1");
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        if(height < 1)
            throw new IllegalArgumentException("Terminal height cannot be less than 1");
        this.height = height;
    }

    public boolean isPositionWithinSize(CursorPosition pos) {
        return (pos.getRow() > -1 && pos.getColumn() > -1 &&
        pos.getRow() < height && pos.getColumn() < width);
    }

    @Override
    public String toString() {
        return "TerminalSize{" +
                "height=" + height +
                ", width=" + width +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalSize)) return false;

        TerminalSize that = (TerminalSize) o;

        if (height != that.height) return false;
        if (width != that.width) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = height;
        result = 31 * result + width;
        return result;
    }
}
