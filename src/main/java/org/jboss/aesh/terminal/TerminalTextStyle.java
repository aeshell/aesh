/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.terminal;

/**
 * Specify a text style.
 * There are some values that nullify other values.
 * eg:
 * - specifying faint AND bold will nullify bold
 * bold AND italic will nullify bold
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class TerminalTextStyle {
    private boolean bold = false;
    private boolean faint = false;
    private boolean italic = false;
    private boolean underline = false;
    private boolean blink = false;
    private boolean invert = false;
    private boolean crossedOut = false;
    private boolean conceal = false;

    private int length = -1;

    private static byte BOLD_OFF = 22;
    private static byte ITALIC_OFF = 23;
    private static byte UNDERLINE_OFF = 24;
    private static byte BLINK_OFF = 25;
    private static byte INVERT_OFF = 27;
    private static byte REVEAL = 28;
    private static byte CROSSED_OUT_OFF = 29;
    private static char SEPARATOR = ';';

    public TerminalTextStyle() {
    }

    public TerminalTextStyle(CharacterType type) {
        if(type == CharacterType.BOLD)
            bold = true;
        else if(type == CharacterType.FAINT)
            faint = true;
        else if(type == CharacterType.ITALIC)
            italic = true;
        else if(type == CharacterType.UNDERLINE)
            underline = true;
        else if(type == CharacterType.BLINK)
            blink = true;
        else if(type == CharacterType.INVERT)
            invert = true;
        else if(type == CharacterType.CROSSED_OUT)
            crossedOut = true;
        else if(type == CharacterType.CONCEAL)
            conceal = true;
    }

    public TerminalTextStyle(boolean bold, boolean faint, boolean italic, boolean underline,
                             boolean blink, boolean invert, boolean crossedOut) {
        this.bold = bold;
        this.faint = faint;
        this.italic = italic;
        this.underline = underline;
        this.blink = blink;
        this.invert = invert;
        this.crossedOut = crossedOut;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isFaint() {
        return faint;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public boolean isBlink() {
        return blink;
    }

    public void setBlink(boolean blink) {
        this.blink = blink;
    }

    public boolean isInvert() {
        return invert;
    }

    public void setInvert(boolean invert) {
        this.invert = invert;
    }

    public boolean isCrossedOut() {
        return crossedOut;
    }

    public void setCrossedOut(boolean crossedOut) {
        this.crossedOut = crossedOut;
    }

    public boolean isConceal() {
        return conceal;
    }

    public void setConceal(boolean conceal) {
        this.conceal = conceal;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        if(bold) {
            if(builder.length() > 0)
                builder.append(';');
            builder.append(CharacterType.BOLD.getValue());
        }
        if(faint) {
            if(builder.length() > 0)
                builder.append(';');
            builder.append(CharacterType.FAINT.getValue());
        }
        if(italic) {
            if(builder.length() > 0)
                builder.append(';');
            builder.append(CharacterType.ITALIC.getValue());
        }
        if(underline) {
            if(builder.length() > 0)
                builder.append(';');
            builder.append(CharacterType.UNDERLINE.getValue());
        }
        if(blink) {
            if(builder.length() > 0)
                builder.append(';');
            builder.append(CharacterType.BLINK.getValue());
        }
        if(invert) {
            if(builder.length() > 0)
                builder.append(';');
            builder.append(CharacterType.INVERT.getValue());
        }
        if(crossedOut) {
            if(builder.length() > 0)
                builder.append(';');
            builder.append(CharacterType.CROSSED_OUT.getValue());
        }
        if(conceal) {
            if(builder.length() > 0)
                builder.append(';');
            builder.append(CharacterType.CONCEAL.getValue());
        }

        if(length < 0)
            length = builder.length();

        return builder.toString();
    }

    public int getLength() {
        if(length < 0)
            toString();
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TerminalTextStyle)) return false;

        TerminalTextStyle that = (TerminalTextStyle) o;

        if (blink != that.blink) return false;
        if (bold != that.bold) return false;
        if (crossedOut != that.crossedOut) return false;
        if (faint != that.faint) return false;
        if (invert != that.invert) return false;
        if (italic != that.italic) return false;
        if (underline != that.underline) return false;
        if (conceal != that.conceal) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (bold ? 1 : 0);
        result = 31 * result + (faint ? 1 : 0);
        result = 31 * result + (italic ? 1 : 0);
        result = 31 * result + (underline ? 1 : 0);
        result = 31 * result + (blink ? 1 : 0);
        result = 31 * result + (invert ? 1 : 0);
        result = 31 * result + (crossedOut ? 1 : 0);
        result = 31 * result + (conceal ? 1 : 0);
        return result;
    }

    public String getValueComparedToPrev(TerminalTextStyle prev) {
        StringBuilder builder = new StringBuilder();
        if(!this.equals(prev)) {
            if(prev.isBold() || prev.isFaint()) {
                if(builder.length() > 0)
                    builder.append(SEPARATOR);
                builder.append(BOLD_OFF);
            }
            if(prev.isUnderline()) {
                if(builder.length() > 0)
                    builder.append(SEPARATOR);
                builder.append(UNDERLINE_OFF);
            }
            if(prev.isItalic()) {
                if(builder.length() > 0)
                    builder.append(SEPARATOR);
                builder.append(ITALIC_OFF);
            }
            if(prev.isBlink()) {
                if(builder.length() > 0)
                    builder.append(SEPARATOR);
                builder.append(BLINK_OFF);
            }
            if(prev.isInvert()) {
                if(builder.length() > 0)
                    builder.append(SEPARATOR);
                builder.append(INVERT_OFF);
            }
            if(prev.isCrossedOut()) {
                if(builder.length() > 0)
                    builder.append(SEPARATOR);
                builder.append(CROSSED_OUT_OFF);
            }
            if(prev.isConceal()) {
                if(builder.length() > 0)
                    builder.append(SEPARATOR);
                builder.append(REVEAL);
            }
        }

        String str = toString();
        if(str.length() > 0 && builder.length() > 0)
            return builder.append(SEPARATOR).append(str).toString();
        else
            return builder.append(str).toString();
    }
}
