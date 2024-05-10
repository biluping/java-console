package com.myboy;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.myboy.classloader.AgentClassLoader;
import com.myboy.domain.ao.ExecAO;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import lombok.val;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.TerminalBuilder;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Comparator;
import java.util.List;

public class Attach {
    public static void main(String[] args) throws Exception {

        if (!Attach.class.getClassLoader().toString().startsWith(AgentClassLoader.namePrefix)) {
            String jdkVersion = System.getProperty("java.version");
            if (jdkVersion.startsWith("1.")) {
                if (jdkVersion.startsWith("1.8")) {
                    try {
                        // custom class loader to load current jar and tools.jar
                        AgentClassLoader customClassLoader = new AgentClassLoader(
                                new URL[]{toolsJarUrl(), currentUrl()},
                                ClassLoader.getSystemClassLoader().getParent()
                        );
                        Class<?> mainClass = Class.forName("com.myboy.Attach", true, customClassLoader);
                        Method mainMethod = mainClass.getMethod("main", String[].class);
                        mainMethod.invoke(null, (Object) args);
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println(jdkVersion + " is not supported");
                    return;
                }
            }
        }


        val terminal = TerminalBuilder.builder().build();
        val reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        List<VirtualMachineDescriptor> jps = VirtualMachine.list();
        jps.sort(Comparator.comparing(VirtualMachineDescriptor::displayName));
        for (int i = 0; i < jps.size(); i++) {
            System.out.printf("[%d] %s%n", i, jps.get(i).displayName().split(" ")[0]);
        }

        String index;
        do {
            index = reader.readLine("请输入要 attach 的进程: ");
        } while (!NumberUtil.isNumber(index));
        VirtualMachineDescriptor descriptor = jps.get(Integer.parseInt(index));

        try {
            VirtualMachine attach = VirtualMachine.attach(descriptor.id());
            URL jarUrl = Attach.class.getProtectionDomain().getCodeSource().getLocation();
            String curJarPath = Paths.get(jarUrl.toURI()).toString();
            attach.loadAgent(curJarPath);
            attach.detach();
            System.out.println("attach success");
        } catch (Exception e) {
            if (!ObjUtil.equal("0", e.getMessage())) {
                e.printStackTrace();
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            HttpUtil.get("http://localhost:7070/quit");
            System.out.println("attach quit");
        }));

        while (true) {
            String line = reader.readLine("cmd # ");
            if (StrUtil.isBlank(line)) continue;
            if (StrUtil.equals(line.trim(), "quit")) break;
            String post = HttpUtil.post("http://localhost:7070", JSONUtil.toJsonStr(new ExecAO(line)));
            System.out.println(post);
        }
    }

    private static URL toolsJarUrl() throws Exception {
        String javaHome = System.getProperty("java.home");
        File toolsJarFile = new File(javaHome, "../lib/tools.jar");
        if (!toolsJarFile.exists()) {
            throw new Exception("tools.jar not found at: " + toolsJarFile.getPath());
        }
        return toolsJarFile.toURI().toURL();
    }

    private static URL currentUrl() {
        ProtectionDomain domain = Attach.class.getProtectionDomain();
        CodeSource codeSource = domain.getCodeSource();
        return codeSource.getLocation();
    }
}
