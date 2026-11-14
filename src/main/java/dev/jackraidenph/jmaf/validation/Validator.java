package dev.jackraidenph.jmaf.validation;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.List;

public interface Validator {
    void test(Element element, TypeElement validatedAnnotation, List<String> args);

    String toString(List<String> args, Elements elements);
}
