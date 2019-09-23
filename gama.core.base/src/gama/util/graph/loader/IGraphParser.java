/*******************************************************************************************************
 *
 * gama.util.graph.loader.IGraphParser.java, in plugin gama.core,
 * is part of the source code of the GAMA modeling and simulation platform (v. 1.8)
 * 
 * (c) 2007-2018 UMI 209 UMMISCO IRD/SU & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 * 
 ********************************************************************************************************/
package gama.util.graph.loader;

import gama.runtime.scope.IScope;

/**
 * A graph parser is in charge of parsing a graph stored in a file, and to transmit the corresponding events to a graph
 * parser listener.
 * 
 * @author Samuel Thiriot
 * 
 */
public interface IGraphParser {

	public void parseFile(IScope scope, IGraphParserListener listener, String filename);

}