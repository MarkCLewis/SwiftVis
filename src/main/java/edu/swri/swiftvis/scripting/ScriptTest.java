/*
 * Created on Jun 6, 2007
 */
package edu.swri.swiftvis.scripting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JFileChooser;

public class ScriptTest {
    public static void main(String[] args) {
        try {
            JFileChooser chooser=new JFileChooser();
            ClassLoader sysClassLoader=ClassLoader.getSystemClassLoader();
            var sysClass=URLClassLoader.class;
            while(chooser.showOpenDialog(null)!=JFileChooser.CANCEL_OPTION) {
                try {
                    Method method=sysClass.getDeclaredMethod("addURL", new Class[]{URL.class});
                    method.setAccessible(true);
                    method.invoke(sysClassLoader,chooser.getSelectedFile().toURI().toURL());
                } catch(InvocationTargetException e) {
                    e.printStackTrace();
                } catch(NoSuchMethodException e) {
                    e.printStackTrace();
                } catch(IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } catch(MalformedURLException e) {
            e.printStackTrace();
        }
        ScriptEngineManager manager=new ScriptEngineManager();
        List<ScriptEngineFactory> factories=manager.getEngineFactories();
        for(ScriptEngineFactory sef:factories) {
            System.out.println(sef.getEngineName()+" "+sef.getEngineVersion()+" "+sef.getLanguageName()+" "+sef.getLanguageVersion());
            List<String> names=sef.getNames();
            for(String s:names) {
                System.out.println("\t"+s);
            }
        }
        ScriptEngine engine=manager.getEngineByName("ECMAScript");
        if(engine!=null) {
            engine.put("str", "Hi Mom!");
            try {
                engine.eval("print(str);");
            } catch (ScriptException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No jython found.");
        }
    }
}
