package dev.jackraidenph.jmaf.api;

import com.sun.source.util.TaskListener;
import com.sun.tools.javac.util.Context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractContextTaskListener implements TaskListener {
    private final Context context;
    private final List<StringAttributeInterpolator> interpolators;

    public AbstractContextTaskListener(Context context, Collection<StringAttributeInterpolator> interpolators) {
        this.context = context;
        this.interpolators = new ArrayList<>(interpolators);
    }

    public List<StringAttributeInterpolator> getInterpolators() {
        return interpolators;
    }

    public Context getContext() {
        return context;
    }
}
