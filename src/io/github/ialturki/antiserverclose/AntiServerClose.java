// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 iAlturki — Minecraft AntiServerClose
package io.github.ialturki.antiserverclose;

import net.fabricmc.api.DedicatedServerModInitializer;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Real external-protocol client supervisor; never intercepts Minecraft stop. */
public final class AntiServerClose implements DedicatedServerModInitializer {
    private static final Path ROOT = Path.of("config", "minecraft-antiserverclose").toAbsolutePath().normalize();
    private static final Path CONFIG = ROOT.resolve("antiserverclose.properties");
    private static final String PREFIX = "[iAlturki AntiServerClose] ";
    private volatile boolean stopping;
    private volatile Process child;
    private volatile Object currentPlayer;
    private volatile boolean playerReadOk;
    private volatile String configuredName = "";
    private volatile Object ownPlayer;
    private String configuredAddress = "";
    private long launchedAt, nextAttempt;
    private boolean diagnosticsOk;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "AntiServerClose-Supervisor"); t.setDaemon(true); return t;
    });

    @Override public void onInitializeServer() {
        try {
            Files.createDirectories(ROOT);
            if (Files.getFileStore(ROOT).supportsFileAttributeView("posix"))
                Files.setPosixFilePermissions(ROOT, PosixFilePermissions.fromString("rwx------"));
            if (!Files.exists(CONFIG)) Files.writeString(CONFIG,
                "# Minecraft AntiServerClose by iAlturki. Disabled until you configure it.\n" +
                "enabled=false\nusername=\naddress=\n" +
                "# Authentication tokens will be stored on this hosted server.\n" +
                "authentication-approved=false\nreconnect-seconds=90\n", StandardCharsets.UTF_8);
            register("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents", "SERVER_STARTED", "ServerStarted", args -> { worker.submit(this::supervise); return null; });
            register("net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents", "SERVER_STOPPING", "ServerStopping", args -> { shutdown(); return null; });
            register("net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents", "END_SERVER_TICK", "EndTick", args -> { updatePlayer(args[0]); return null; });
            register("net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents", "ALLOW_DAMAGE", "AllowDamage", args -> !isOwnBotPlayer(args[0]));
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "AntiServerClose-Cleanup"));
            log("Launcher loaded. Startup will run native checks and read saved configuration.");
        } catch (Throwable e) { log("Disabled: initialization failed: " + safeError(e)); shutdown(); }
    }

    private interface Callback { Object call(Object[] args); }
    private void register(String holder, String field, String listener, Callback callback) throws Exception {
        Class<?> h = Class.forName(holder);
        Class<?> iface = Class.forName(holder + "$" + listener);
        Object handler = Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[]{iface}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) return switch(method.getName()) {
                case "toString" -> "AntiServerClose-" + listener;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> null;
            };
            return callback.call(args);
        });
        Class.forName("net.fabricmc.fabric.api.event.Event").getMethod("register", Object.class).invoke(h.getField(field).get(null), handler);
    }

    // A temporary damage veto for this exact bot entity. No saved invulnerability,
    // game-mode, position, inventory, or permissions changes; a replacement human
    // entity is not protected, even when it has the same account name.
    private boolean isOwnBotPlayer(Object entity) {
        Process running = child;
        return !stopping && entity != null && entity == ownPlayer &&
            running != null && running.isAlive();
    }

    // Runs on the Minecraft server thread. No world, entity, or game-state writes.
    private void updatePlayer(Object server) {
        if (configuredName.isBlank()) return;
        try {
            Object list = server.getClass().getMethod("getPlayerList").invoke(server);
            currentPlayer = list.getClass().getMethod("getPlayerByName", String.class).invoke(list, configuredName);
            playerReadOk = true;
        } catch (ReflectiveOperationException e) { playerReadOk = false; }
    }

    private void supervise() {
        try {
            diagnosticsOk = diagnose();
            if (!diagnosticsOk) { log("Native test failed. Bot stays disabled. See config/minecraft-antiserverclose/diagnostics.txt."); return; }
            log("Native test passed. Reading saved configuration.");
            boolean announcedDisabled = false;
            while (!stopping) {
                Properties p = new Properties();
                try (Reader r = Files.newBufferedReader(CONFIG)) { p.load(r); }
                String name = p.getProperty("username", "").trim();
                if (!name.equals(configuredName)) {
                    stopChild(); currentPlayer = null; playerReadOk = false;
                    configuredName = name;
                }
                boolean enabled = Boolean.parseBoolean(p.getProperty("enabled", "false"));
                boolean approved = Boolean.parseBoolean(p.getProperty("authentication-approved", "false"));
                if (!enabled || !approved || !name.matches("[A-Za-z0-9_]{3,16}") || !validAddress(p.getProperty("address", "").trim())) {
                    stopChild();
                    if (!announcedDisabled) { log("Disabled: Set enabled=true, authentication-approved=true, username, and an explicit address=host:port."); announcedDisabled = true; }
                } else {
                    announcedDisabled = false;
                    superviseConnection(p);
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
          catch (Throwable e) { log("Supervisor stopped safely: " + safeError(e)); }
        finally { stopChild(); }
    }

    private void superviseConnection(Properties p) throws Exception {
        String requestedAddress = p.getProperty("address", "").trim();
        if (!requestedAddress.equals(configuredAddress)) {
            stopChild(); configuredAddress = requestedAddress;
        }
        if (!playerReadOk) { stopChild(); return; }
        Process running = child;
        Object player = currentPlayer;
        long now = System.currentTimeMillis();
        long retry = reconnectSeconds(p.getProperty("reconnect-seconds", "90")) * 1000L;
        if (running != null) {
            if (!running.isAlive()) {
                int exit = running.exitValue();
                stopChild(); nextAttempt = now + retry;
                if (exit == 42) { log("Authenticated account does not match username; disabled. Check your account setup."); disable(); }
                return;
            }
            if (ownPlayer == null && player != null) {
                ownPlayer = player;
                log("Authenticated account is present in Minecraft's real player list. Temporary bot-session damage protection is active.");
            } else if (ownPlayer != null && player != null && player != ownPlayer) {
                log("Account connected in another client. Yielding; no reconnect while it remains online.");
                stopChild(); nextAttempt = now + retry;
            } else if (ownPlayer != null && player == null) {
                log("Bot left the real player list. Restarting after cooldown.");
                stopChild(); nextAttempt = now + retry;
            } else if (ownPlayer == null && now - launchedAt > 600_000) {
                log("Login did not complete in 10 minutes. Disabled to avoid repeated authentication prompts.");
                disable(); stopChild();
            }
            return;
        }
        // Never connect over an existing human session using the same account.
        if (player != null || now < nextAttempt || stopping) return;
        String address = p.getProperty("address", "").trim();
        if (!validAddress(address)) throw new IOException("Use an explicit hostname or IPv4 address and port");
        ProcessBuilder pb = nativeProcess("--address", address, "--expected-username", configuredName);
        // Output stays on the user's server. Never print stored credentials.
        pb.redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(ROOT.resolve("bot-console.log").toFile()));
        launchedAt = now; ownPlayer = null;
        if (startManaged(pb) == null) return;
        log("Started Microsoft-authenticated Botcraft 26.3 client. Device-login prompts, if needed: config/minecraft-antiserverclose/bot-console.log.");
    }

    private static boolean validAddress(String address) {
        // Explicit port only. No schemes, command arguments, whitespace, or IPv6.
        if (!address.matches("[A-Za-z0-9](?:[A-Za-z0-9.-]{0,251}[A-Za-z0-9])?:[0-9]{1,5}")) return false;
        int colon = address.lastIndexOf(':');
        String host = address.substring(0, colon);
        if (host.contains("..")) return false;
        for (String label : host.split("\\."))
            if (label.isEmpty() || label.length() > 63 || label.startsWith("-") || label.endsWith("-")) return false;
        int port = Integer.parseInt(address.substring(colon + 1));
        return port >= 1 && port <= 65535;
    }
    private static long reconnectSeconds(String value) {
        try { return Math.max(60, Math.min(600, Long.parseLong(value))); }
        catch (NumberFormatException ignored) { return 90; }
    }

    private boolean diagnose() throws Exception {
        StringBuilder report = new StringBuilder();
        report.append("OS=").append(System.getProperty("os.name")).append("\nArch=").append(System.getProperty("os.arch"))
              .append("\nJava=").append(System.getProperty("java.version")).append('\n');
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux") ||
            !Set.of("amd64", "x86_64").contains(System.getProperty("os.arch"))) {
            report.append("Unsupported native platform. No bot started.\n");
            Files.writeString(ROOT.resolve("diagnostics.txt"), report); return false;
        }
        Path bin = ROOT.resolve("native-26.3"); Files.createDirectories(bin);
        for (String file : List.of("AntiServerCloseClient", "libbotcraft.so", "libprotocolCraft.so", "libantiserverclose-timestamp-fix.so")) {
            Path dest = bin.resolve(file);
            try (InputStream in = getClass().getResourceAsStream("/native/" + file)) {
                if (in == null) throw new IOException("Missing packaged native resource: " + file);
                byte[] bytes = in.readAllBytes();
                Files.write(dest, bytes);
                report.append(file).append(" SHA256=").append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))).append('\n');
            }
            if (!dest.toFile().setExecutable(true, true)) throw new IOException("Cannot grant owner execution to " + file);
        }
        Path helpOut = ROOT.resolve("native-help.txt");
        Files.writeString(ROOT.resolve("diagnostics.txt"), report);
        Process diagnostic;
        try {
            diagnostic = startManaged(nativeProcess("--help").redirectErrorStream(true).redirectOutput(helpOut.toFile()));
            if (diagnostic == null) return false;
        } catch (IOException e) {
            report.append("Native execution failed: ").append(safeError(e)).append('\n');
            Files.writeString(ROOT.resolve("diagnostics.txt"), report);
            log(report.toString()); return false;
        }
        boolean exited = diagnostic.waitFor(15, TimeUnit.SECONDS);
        if (!exited) diagnostic.destroyForcibly();
        int code = exited ? diagnostic.exitValue() : -1;
        report.append("Native --help exit=").append(code).append('\n');
        String output = Files.exists(helpOut) ? Files.readString(helpOut) : "";
        report.append(output);
        Files.writeString(ROOT.resolve("diagnostics.txt"), report);
        log(report.toString());
        synchronized (this) { if (child == diagnostic) child = null; }
        return exited && code == 0 && output.contains("--address") && output.contains("--expected-username");
    }

    private ProcessBuilder nativeProcess(String... args) {
        Path bin = ROOT.resolve("native-26.3");
        List<String> command = new ArrayList<>(); command.add(bin.resolve("AntiServerCloseClient").toString()); command.addAll(Arrays.asList(args));
        ProcessBuilder pb = new ProcessBuilder(command).directory(ROOT.toFile());
        pb.environment().put("LD_LIBRARY_PATH", bin.toString());
        pb.environment().put("LD_PRELOAD", bin.resolve("libantiserverclose-timestamp-fix.so").toString());
        return pb;
    }
    private void disable() throws IOException {
        Properties p = new Properties(); try (Reader r = Files.newBufferedReader(CONFIG)) { p.load(r); }
        p.setProperty("enabled", "false");
        try (Writer w = Files.newBufferedWriter(CONFIG)) { p.store(w, "Automatically disabled after unsuccessful login"); }
    }
    private synchronized Process startManaged(ProcessBuilder builder) throws IOException {
        if (stopping) return null;
        child = builder.start();
        return child;
    }
    private synchronized void stopChild() {
        Process old = child; child = null; ownPlayer = null;
        if (old != null && old.isAlive()) {
            old.destroy();
            try { if (!old.waitFor(3, TimeUnit.SECONDS)) old.destroyForcibly(); }
            catch (InterruptedException e) { old.destroyForcibly(); Thread.currentThread().interrupt(); }
        }
    }
    private void shutdown() { stopping = true; stopChild(); worker.shutdownNow(); }
    private static void log(String text) { System.out.println(PREFIX + text); }
    private static String safeError(Throwable e) { return e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()); }
}
