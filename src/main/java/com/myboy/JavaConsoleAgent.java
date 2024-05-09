package com.myboy;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.myboy.domain.ao.ExecAO;
import io.javalin.Javalin;
import io.javalin.http.Context;
import lombok.SneakyThrows;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaConsoleAgent {

    private static Instrumentation instrumentation;
    private static final Pattern pattern = Pattern.compile("\\s*def\\s+(\\w+)\\s*=\\s*(.*)");
    private static final SimpleBindings bindings = new SimpleBindings();
    private static final ScriptEngine groovy = new ScriptEngineManager().getEngineByName("groovy");
    private static Javalin app = null;

    private static void initBinds() {
        try {
            Object getFun = groovy.eval("{clazz -> com.liubs.findinstances.jvmti.InstancesOfClass.getInstances(Class.forName(\"$clazz\"))}");
            bindings.put("get", getFun);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    public static void agentmain(String args, Instrumentation inst) {
        if (instrumentation != null) {
            System.out.println("Already attached before");
            return;
        }
        instrumentation = inst;
        initBinds();
        app = Javalin.create()
                .post("/", JavaConsoleAgent::exec)
                .get("/quit", ctx -> {
                    new Thread(() -> {
                        ThreadUtil.sleep(1, TimeUnit.SECONDS);
                        app.stop();
                        bindings.clear();
                        instrumentation = null;
                    }).start();
                }).start(7070);
    }

    @SneakyThrows
    private static void exec(Context context) {
        ExecAO execAO = context.bodyAsClass(ExecAO.class);
        System.out.println("script: " + execAO.getScript());
        Matcher matcher = pattern.matcher(execAO.getScript());

        try {
            if (matcher.find() && matcher.groupCount() == 2) {
                bindings.put(matcher.group(1), groovy.eval(matcher.group(2), bindings));
                System.out.println("add variable: " + matcher.group(1));
            } else {
                Object eval = groovy.eval(execAO.getScript(), bindings);
                if (eval != null) {
                    context.result(eval.toString());
                }
            }
        } catch (Exception e) {
            context.result(ExceptionUtil.stacktraceToString(e));
        }
    }
}
