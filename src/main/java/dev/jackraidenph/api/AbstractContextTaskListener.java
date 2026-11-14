package dev.jackraidenph.api;

import com.sun.source.util.TaskListener;
import com.sun.tools.javac.util.Context;

public abstract class AbstractContextTaskListener implements TaskListener {
    private final Context context;

    public AbstractContextTaskListener(Context context) {
        this.context = context;
    }
}
