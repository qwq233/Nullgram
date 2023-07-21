package org.telegram.ui.Stories.recorder;

import android.graphics.Matrix;

public abstract class IStoryPart {
    public int id;
    public int width, height;
    public final Matrix matrix = new Matrix();
}
