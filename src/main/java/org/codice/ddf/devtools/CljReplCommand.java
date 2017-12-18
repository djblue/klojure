package org.codice.ddf.devtools;

import clojure.lang.PersistentHashMap;
import clojure.lang.RT;
import clojure.lang.Symbol;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import sun.misc.Signal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

@Command(scope = "clj", name = "repl", description = "Start clojure repl")
@Service
public class CljReplCommand implements Action {
    @Override
    public Object execute() throws Exception {

        Field signals = Signal.class.getDeclaredField("signals");
        signals.setAccessible(true);
        Hashtable signalsSave = (Hashtable) ((Hashtable) signals.get(null)).clone();

        Field handlers = Signal.class.getDeclaredField("handlers");
        handlers.setAccessible(true);
        Hashtable handlersSave = (Hashtable) ((Hashtable) handlers.get(null)).clone();

        List<Object> args = new ArrayList<>();
        Thread.currentThread()
                .setContextClassLoader(Nrepl.class.getClassLoader());

        args.add(PersistentHashMap.create(RT.keyword(null,"port"), 0));
        RT.var("clojure.core", "require")
                .invoke(Symbol.intern("reply.main"));
        RT.var("reply.main", "launch-nrepl")
                .applyTo(RT.seq(args));

        signals.set(null, signalsSave);
        handlers.set(null, handlersSave);

        return null;
    }
}
