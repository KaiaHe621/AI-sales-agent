package com.jichi.prompt.utils;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

public class CodeCompileValidator {

    public static ValidationResult validate(String code, String className) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fileManager =
                     compiler.getStandardFileManager(diagnostics, null, null)) {

            JavaFileObject sourceFile = new InMemoryJavaFileObject(className, code);

            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics,
                    null, null, List.of(sourceFile));

            boolean success = task.call();
            List<String> errors = diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                    .map(Diagnostic::toString)
                    .toList();

            return new ValidationResult(success, errors);
        } catch (Exception e) {
            return new ValidationResult(false, List.of(e.getMessage()));
        }
    }

    record ValidationResult(boolean compileSuccess, List<String> errors) {}

    /** 内存 JavaFileObject，把源码字符串包装成编译器可识别的对象 */
    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String code;

        InMemoryJavaFileObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                  Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }

        @Override
        public OutputStream openOutputStream() {
            return new ByteArrayOutputStream();
        }
    }
}