package javax.microedition.media.control;

import javax.microedition.media.MediaException;

public interface VideoControl extends GUIControl {
    int USE_DIRECT_VIDEO = 1;

    Object initDisplayMode(int mode, Object arg);

    void setDisplayLocation(int x, int y);

    int getDisplayX();

    int getDisplayY();

    void setVisible(boolean visible);

    void setDisplaySize(int w, int h) throws MediaException;

    void setDisplayFullScreen(boolean full) throws MediaException;

    int getSourceWidth();

    int getSourceHeight();

    int getDisplayWidth();

    int getDisplayHeight();

    byte[] getSnapshot(String type) throws MediaException;
}
