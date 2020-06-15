/*********************************************************************************************
 *
 * 'GamlResourceInfoProvider.java, in plugin ummisco.gama.gaml, is part of the source code of the GAMA modeling and
 * simulation platform. (c) 2007-2016 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package gama.extensions.files.gaml;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.deleteWhitespace;
import static org.apache.commons.lang.StringUtils.split;
import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.uncapitalize;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;

import gama.common.interfaces.IKeyword;
import gama.core.lang.gaml.Pragma;
import gama.core.lang.gaml.StringLiteral;
import gama.core.lang.gaml.indexer.GamlResourceIndexer;
import gama.core.lang.gaml.resource.GamlResource;
import gama.dev.utils.DEBUG;
import gaml.GAML;
import gaml.compilation.interfaces.ISyntacticElement;

@SuppressWarnings ({ "unchecked", "rawtypes" })
public class GamlResourceInfoProvider {

	public static final GamlResourceInfoProvider INSTANCE = new GamlResourceInfoProvider();

	private GamlResourceInfoProvider() {}

	private ResourceSet resourceSet;

	public GamlFileInfo getInfo(final URI originalURI, final GamlResource r, final long stamp) {

		Set<String> imports = null;
		final Set<URI> uris = GamlResourceIndexer.INSTANCE.directImportsOf(originalURI);
		for (final URI u : uris) {
			if (imports == null) {
				imports = new LinkedHashSet();
			}
			imports.add(u.deresolve(originalURI).toString());
		}

		Set<String> tags = null;
		String str = "";
		try (InputStream is = resourceSet.getURIConverter().createInputStream(originalURI);
				BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
			boolean tagsFound = false;
			while (!tagsFound && (str = reader.readLine()) != null) {
				tagsFound = str.contains("Tags: ");
			}
			if (tagsFound) {
				tags = new HashSet<>(asList(split(uncapitalize(deleteWhitespace(substringAfter(str, "Tags: "))), ',')));
			}
		} catch (final IOException e1) {
			e1.printStackTrace();
		}

		Set<String> uses = null;
		Set<String> exps = null;

		final TreeIterator<EObject> tree = GAML.getEcoreUtils().getAllContents(r, true);
		boolean processExperiments = true;

		while (tree.hasNext()) {
			final EObject e = tree.next();
			if (e instanceof Pragma) {
				final String s = ((Pragma) e).getName();
				if (IKeyword.NO_EXPERIMENT.equals(s)) {
					processExperiments = false;
				}
			} else if (e instanceof StringLiteral) {
				final String s = GAML.getEcoreUtils().getKeyOf(e);
				if (s.length() > 4) {
					final URI u = URI.createFileURI(s);
					final String ext = u.fileExtension();
					if (ext != null && !ext.isEmpty()) {
						if (uses == null) {
							uses = new LinkedHashSet();
						}
						uses.add(s);
					}
				}
			} else if (processExperiments && GAML.getEcoreUtils().isExperiment(e)) {
				String s = GAML.getEcoreUtils().getNameOf(e);
				if (s == null) {
					DEBUG.ERR("EXPERIMENT NULL");
				}
				if (GAML.getEcoreUtils().isBatch(e)) {
					s = GamlFileInfo.BATCH_PREFIX + s;
				}

				if (exps == null) {
					exps = new LinkedHashSet();
				}
				exps.add(s);
			} else if (processExperiments && GAML.getEcoreUtils().isHeadlessExperiment(e)) {
				String s = GAML.getEcoreUtils().getNameOf(e);

				if (GAML.getEcoreUtils().isBatch(e)) {
					s = GamlFileInfo.BATCH_PREFIX + s;
				}

				if (exps == null) {
					exps = new LinkedHashSet();
				}
				exps.add(s);
			}
		}

		return new GamlFileInfo(stamp, imports, uses, exps, tags);

	}

	public GamlFileInfo getInfo(final URI uri, final long stamp) {
		try {

			final GamlResource r = (GamlResource) getResourceSet().getResource(uri, true);
			return getInfo(uri, r, stamp);
		} finally {
			clearResourceSet(getResourceSet());
		}
	}

	public ISyntacticElement getContents(final URI uri) {
		try {
			final GamlResource r = (GamlResource) getResourceSet().getResource(uri, true);
			return r.buildSyntacticContents();
		} finally {
			clearResourceSet(getResourceSet());
		}
	}

	protected void clearResourceSet(final ResourceSet resourceSet) {
		final boolean wasDeliver = resourceSet.eDeliver();
		try {
			resourceSet.eSetDeliver(false);
			resourceSet.getResources().clear();
		} catch (final Exception e) {}

		finally {
			resourceSet.eSetDeliver(wasDeliver);
		}
	}

	private ResourceSet getResourceSet() {
		if (resourceSet == null) {
			resourceSet = GAML.getEcoreUtils().getResourceSet();
		}
		return resourceSet;
	}

}
