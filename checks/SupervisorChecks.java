// SPDX-License-Identifier: GPL-3.0-only
// Copyright (c) 2026 iAlturki — Minecraft AntiServerClose
import io.github.ialturki.antiserverclose.AntiServerClose;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SupervisorChecks {
    private static class Child extends Process {
        boolean alive = true;
        public OutputStream getOutputStream() { return OutputStream.nullOutputStream(); }
        public InputStream getInputStream() { return InputStream.nullInputStream(); }
        public InputStream getErrorStream() { return InputStream.nullInputStream(); }
        public int waitFor() { alive = false; return 0; }
        public boolean waitFor(long timeout, TimeUnit unit) { return !alive; }
        public int exitValue() { if (alive) throw new IllegalThreadStateException(); return 0; }
        public void destroy() { alive = false; }
        public Process destroyForcibly() { alive = false; return this; }
        public boolean isAlive() { return alive; }
    }
    static void set(AntiServerClose b, String name, Object value) throws Exception {
        Field f = AntiServerClose.class.getDeclaredField(name); f.setAccessible(true); f.set(b,value);
    }
    static Object get(AntiServerClose b, String name) throws Exception {
        Field f = AntiServerClose.class.getDeclaredField(name); f.setAccessible(true); return f.get(b);
    }
    static Object call(AntiServerClose b, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = AntiServerClose.class.getDeclaredMethod(name,types); m.setAccessible(true); return m.invoke(b,args);
    }
    static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        for (String address : List.of("server.example.com:25565", "127.0.0.1:25565", "a:1", "localhost:65535"))
            require((Boolean)call(new AntiServerClose(),"validAddress",new Class[]{String.class},address),"Valid explicit server address rejected: " + address);
        for (String address : List.of("", "server.example.com", "https://server.example.com:25565", "host:0", "host:65536", "host:abc", "host:25565 --login bad", "a..b:25565", "bad-.example:25565", "[::1]:25565"))
            require(!(Boolean)call(new AntiServerClose(),"validAddress",new Class[]{String.class},address),"Invalid address accepted: " + address);
        require((Long)call(new AntiServerClose(),"reconnectSeconds",new Class[]{String.class},"oops") == 90,"Malformed cooldown must fall back safely");
        require((Long)call(new AntiServerClose(),"reconnectSeconds",new Class[]{String.class},"-10") == 60,"Cooldown must have a minimum");
        require((Long)call(new AntiServerClose(),"reconnectSeconds",new Class[]{String.class},"9223372036854775807") == 600,"Cooldown must have a maximum without overflow");
        Properties p = new Properties(); p.setProperty("address","server.example.com:25565"); p.setProperty("reconnect-seconds","90");
        AntiServerClose b = new AntiServerClose(); set(b,"configuredAddress",p.getProperty("address")); set(b,"playerReadOk",true); set(b,"currentPlayer",new Object());
        call(b,"superviseConnection",new Class[]{Properties.class},p);
        require(get(b,"child") == null,"Must not connect over an existing player session");
        Child c = new Child(); Object bot = new Object(); set(b,"child",c); set(b,"ownPlayer",bot);
        require((Boolean)call(b,"isOwnBotPlayer",new Class[]{Object.class},bot),"A living native bot must have damage protection");
        require(!(Boolean)call(b,"isOwnBotPlayer",new Class[]{Object.class},new Object()),"A human/replacement entity must retain normal damage behavior");
        c.alive = false;
        require(!(Boolean)call(b,"isOwnBotPlayer",new Class[]{Object.class},bot),"Protection must cease when the native child exits");
        c.alive = true;
        set(b,"currentPlayer",new Object());
        call(b,"superviseConnection",new Class[]{Properties.class},p);
        require(!c.alive && get(b,"child") == null,"Replacement human session must stop only the bot process");
        require((Long)get(b,"nextAttempt") > System.currentTimeMillis(),"Yield must schedule a cooldown");
        c = new Child(); set(b,"child",c); set(b,"ownPlayer",bot); set(b,"currentPlayer",null);
        call(b,"superviseConnection",new Class[]{Properties.class},p);
        require(!c.alive,"Disconnected AFK example must be terminated even though its own loop stays alive");
        c = new Child(); set(b,"child",c); set(b,"playerReadOk",false);
        call(b,"superviseConnection",new Class[]{Properties.class},p);
        require(!c.alive,"A failed player lookup must fail closed");
        ProcessBuilder pb = (ProcessBuilder)call(b,"nativeProcess",new Class[]{String[].class},(Object)new String[]{"--address","server.example.com:25565","--expected-username","ExamplePlayer"});
        require(pb.command().get(pb.command().size()-1).equals("ExamplePlayer"),"Native client must check the configured Microsoft account name");
        require(pb.environment().get("LD_PRELOAD").endsWith("native-26.3" + File.separator + "libantiserverclose-timestamp-fix.so"),"Correct timestamp library must apply only to the child process");
        c = new Child(); set(b,"child",c); call(b,"shutdown",new Class[]{});
        require(!c.alive && (Boolean)get(b,"stopping"),"Minecraft shutdown must also stop its child client");
        require(!(Boolean)call(b,"isOwnBotPlayer",new Class[]{Object.class},bot),"Shutdown must cease protection");
        require(call(b,"startManaged",new Class[]{ProcessBuilder.class},new ProcessBuilder("must-not-run")) == null,"A shutdown racing with launch must not create an orphan child");
        System.out.println("PASS: explicit addresses, bounded cooldown, human-session yield, disconnect recovery, lookup failure, Microsoft-only client arguments, bot-only damage protection, shutdown cleanup.");
    }
}
