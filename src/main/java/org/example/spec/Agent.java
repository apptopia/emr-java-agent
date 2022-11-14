package org.example.spec;

import clojure.lang.RT;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.security.ProtectionDomain;

import static clojure.lang.RT.LOADER_SUFFIX;
import static org.objectweb.asm.Opcodes.ASM5;

public final class Agent {
    public static void premain(String args, Instrumentation instrumentation) {
        instrumentation.addTransformer(new RequireInClinitTransformer());
    }

    private static final class RequireInClinitTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
            try {
                ClassLoader checkLoader = loader;
                while (checkLoader != null && !checkLoader.equals(RT.class.getClassLoader())) {
                    checkLoader = checkLoader.getParent();
                }
                if (checkLoader == null) {
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
