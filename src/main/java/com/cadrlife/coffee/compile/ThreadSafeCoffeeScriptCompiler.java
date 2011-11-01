package com.cadrlife.coffee.compile;

import com.cadrlife.coffee.jcoffeescript.JCoffeeScriptCompileException;
import com.cadrlife.coffee.jcoffeescript.JCoffeeScriptCompiler;

/*
 * Thread-safe wrapper around JCoffeeScriptCompiler.
 */
public class ThreadSafeCoffeeScriptCompiler {
	private static ThreadLocal<JCoffeeScriptCompiler> compiler = new ThreadLocal<JCoffeeScriptCompiler>() {
		protected JCoffeeScriptCompiler initialValue() {
			return new JCoffeeScriptCompiler();
		};
	};

	public String compile(String coffee) throws JCoffeeScriptCompileException {
		return compiler.get().compile(coffee);
	}
	
}
