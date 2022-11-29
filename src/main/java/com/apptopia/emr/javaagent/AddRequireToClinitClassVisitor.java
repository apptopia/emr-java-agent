package com.apptopia.emr.javaagent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.net.URL;

import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.RETURN;

public final class AddRequireToClinitClassVisitor extends ClassVisitor {
    private final ClassLoader loader;

    private String className;
    private int version;
    private boolean skip;

    private boolean visitedStaticBlock = false;

    public AddRequireToClinitClassVisitor(int api, ClassVisitor visitor, ClassLoader loader) {
        super(api, visitor);
        this.loader = loader;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
        this.version = version;
        String scriptBase = className.split("\\$")[0];
        skip = getResource(loader, scriptBase + "__init.class") == null
                && getResource(loader, scriptBase + ".clj") == null
                && getResource(loader, scriptBase + ".cljc") == null;
        if (!skip && Agent.DEBUG_EMR_JAVA_AGENT) {
            System.err.println("!!!: " + name + "(" + version + ")");
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (!skip && mv != null && "<clinit>".equals(name)) {
            visitedStaticBlock = true;
            mv = new AddRequireToClinitMethodVisitor(api, mv, className, version);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (!skip && !visitedStaticBlock) {
            MethodVisitor mv = super.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv = new AddRequireToClinitMethodVisitor(api, mv, className, version);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        super.visitEnd();
    }

    static public URL getResource(ClassLoader loader, String name) {
        if (loader == null) {
            return ClassLoader.getSystemResource(name);
        } else {
            return loader.getResource(name);
        }
    }
}
