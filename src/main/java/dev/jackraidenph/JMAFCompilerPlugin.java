package dev.jackraidenph;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import dev.jackraidenph.util.JMAFTaskListener;

public class JMAFCompilerPlugin implements Plugin {
    public static final String NAME = "JMAFCompilerPlugin";

    @Override
    public void init(JavacTask task, String... args) {
        task.addTaskListener(new JMAFTaskListener(((BasicJavacTask) task).getContext()));
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public boolean autoStart() {
        return true;
    }
}
