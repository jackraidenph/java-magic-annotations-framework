module jmaf {
    requires java.compiler;
    requires jdk.compiler;

    exports dev.jackraidenph.jmaf.annotation.meta;
    exports dev.jackraidenph.jmaf.annotation.validation;
    exports dev.jackraidenph.jmaf.validation;
    exports dev.jackraidenph.jmaf.api;
}