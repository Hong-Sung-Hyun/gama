/*
 * generated by Xtext
 */
package gama.core.lang.ui;

import com.google.inject.Injector;
import gama.ui.modeling.internal.ModelingActivator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.xtext.ui.guice.AbstractGuiceAwareExecutableExtensionFactory;
import org.osgi.framework.Bundle;

/**
 * This class was generated. Customizations should only happen in a newly
 * introduced subclass. 
 */
public class GamlExecutableExtensionFactory extends AbstractGuiceAwareExecutableExtensionFactory {

	@Override
	protected Bundle getBundle() {
		return Platform.getBundle(ModelingActivator.PLUGIN_ID);
	}
	
	@Override
	protected Injector getInjector() {
		ModelingActivator activator = ModelingActivator.getInstance();
		return activator != null ? activator.getInjector(ModelingActivator.GAMA_CORE_LANG_GAML) : null;
	}

}