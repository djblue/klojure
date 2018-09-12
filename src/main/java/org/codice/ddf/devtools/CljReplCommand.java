package org.codice.ddf.devtools;

import clojure.lang.RT;
import clojure.lang.Symbol;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import sun.misc.Signal;

import java.lang.reflect.Field;
import java.util.Hashtable;

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

        Thread.currentThread()
                .setContextClassLoader(Nrepl.class.getClassLoader());

        RT.var("clojure.core", "require")
                .invoke(Symbol.intern("rebel-readline.main"));

        try {
            RT.var("rebel-readline.main", "-main")
                    .invoke();
        } finally {
            signals.set(null, signalsSave);
            handlers.set(null, handlersSave);
        }

        return null;
    }
}
