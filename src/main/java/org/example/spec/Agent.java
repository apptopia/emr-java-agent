package org.example.spec;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static org.objectweb.asm.Opcodes.ASM5;

public final class Agent {
    public static void premain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new RequireInClinitTransformer());
    }

    private static final class RequireInClinitTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            try {
                if (loader == null) {
                    return classfileBuffer;
                }
                if (loader.getResource("clojure/lang/RT.class") == null) {
                    return classfileBuffer;
                }
                if (className != null && className.startsWith("org/example/spec/")) {
                    return classfileBuffer;
                }
                if (className != null && className.startsWith("clojure/lang/")) {
                    return classfileBuffer;
                }

                ClassReader reader = new ClassReader(classfileBuffer);
                ClassWriter writer = new ClassWriter(0);
                reader.accept(new AddRequireToClinitClassVisitor(ASM5, writer, loader), 0);
                return writer.toByteArray();
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(5);
                return null;
            }
        }
    }
}
