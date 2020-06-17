import sample.internal.DummySingleElementAnnotation;

// Example module declaration with annotation.

@DummySingleElementAnnotation("dummy")
module sample {
    requires java.compiler;
    exports sample.exported;
    opens sample.internal to java.compiler;
    uses javax.tools.Tool;
    provides java.lang.Object with sample.internal.Internal;
}
