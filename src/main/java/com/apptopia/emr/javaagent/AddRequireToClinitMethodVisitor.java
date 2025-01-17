package com.apptopia.emr.javaagent;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public final class AddRequireToClinitMethodVisitor extends MethodVisitor {
    private final String className;
    private final int version;

    public AddRequireToClinitMethodVisitor(int api, MethodVisitor methodVisitor, String className, int version) {
        super(api, methodVisitor);
        this.className = className;
        this.version = version;
    }

    @Override
    public void visitCode() {
        String scriptBase = className.split("\\$")[0];
        String scriptBasePath = "/" + scriptBase;
        String scriptBaseClass = scriptBase.replace('/', '.');

        Label start = new Label();
        Label handler = new Label();
        Label end = new Label();
        super.visitCode();

        super.visitTryCatchBlock(start, handler, handler, "java/lang/Exception");

        super.visitLabel(start);

        // Do nothing if loading this exact namespace
        super.visitLdcInsn(scriptBasePath);
        super.visitLdcInsn("clojure.core");
        super.visitLdcInsn("*pending-paths*");
        super.visitMethodInsn(INVOKESTATIC, "clojure/lang/RT", "var", "(Ljava/lang/String;Ljava/lang/String;)Lclojure/lang/Var;", false);
        super.visitMethodInsn(INVOKEINTERFACE, "clojure/lang/IDeref", "deref", "()Ljava/lang/Object;", true);
        super.visitMethodInsn(INVOKESTATIC, "clojure/lang/RT", "first", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false);
        super.visitJumpInsn(IFNE, end);

        // Load the namespace
        super.visitLdcInsn("clojure.core");
        super.visitLdcInsn("serialized-require");
        super.visitMethodInsn(INVOKESTATIC, "clojure/lang/RT", "var", "(Ljava/lang/String;Ljava/lang/String;)Lclojure/lang/Var;", false);
        super.visitMethodInsn(INVOKEVIRTUAL, "clojure/lang/Var", "getRawRoot", "()Ljava/lang/Object;", false);
        super.visitTypeInsn(CHECKCAST, "clojure/lang/IFn");

        super.visitLdcInsn(scriptBaseClass);
        super.visitMethodInsn(INVOKESTATIC, "clojure/lang/Symbol", "intern", "(Ljava/lang/String;)Lclojure/lang/Symbol;", false);

        super.visitMethodInsn(INVOKEINTERFACE, "clojure/lang/IFn", "invoke", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        super.visitInsn(POP);

        super.visitJumpInsn(GOTO, end);

        // Handle exceptions if any occurred
        super.visitLabel(handler);
        if (stackFramesEnabled()) {
            super.visitFrame(F_FULL, 0, new Object[]{}, 1, new Object[]{"java/lang/Exception"});
        }
        if (Agent.DEBUG_EMR_JAVA_AGENT) {
            super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false);
        } else {
            super.visitInsn(POP);
        }

        super.visitLabel(end);
        if (stackFramesEnabled()) {
            super.visitFrame(F_FULL, 0, new Object[]{}, 0, new Object[]{});
        }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(Math.max(4, maxStack), maxLocals);
    }

    private int majorVersion(int version) {
        return version & 0xFFFF;
    }

    private boolean stackFramesEnabled() {
        return majorVersion(version) >= V1_6;
    }
}
