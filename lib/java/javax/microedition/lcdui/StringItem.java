package javax.microedition.lcdui;

public class StringItem extends Item {
    private String text;

    public StringItem(String label, String text) {
        super(label);
        this.text = text;
    }

    public StringItem(String label, String text, int appearance) {
        this(label, text);
    }

    public String getText() {
        return text;
    }

    public void setText(String t) {
        text = t;
    }

    String text() {
        return label == null ? text : (text == null ? label : label + " " + text);
    }
}
