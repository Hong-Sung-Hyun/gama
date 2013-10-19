/*
 * GAMA - V1.4 http://gama-platform.googlecode.com
 * 
 * (c) 2007-2011 UMI 209 UMMISCO IRD/UPMC & Partners (see below)
 * 
 * Developers :
 * 
 * - Alexis Drogoul, UMI 209 UMMISCO, IRD/UPMC (Kernel, Metamodel, GAML), 2007-2012
 * - Vo Duc An, UMI 209 UMMISCO, IRD/UPMC (SWT, multi-level architecture), 2008-2012
 * - Patrick Taillandier, UMR 6228 IDEES, CNRS/Univ. Rouen (Batch, GeoTools & JTS), 2009-2012
 * - Beno�t Gaudou, UMR 5505 IRIT, CNRS/Univ. Toulouse 1 (Documentation, Tests), 2010-2012
 * - Phan Huy Cuong, DREAM team, Univ. Can Tho (XText-based GAML), 2012
 * - Pierrick Koch, UMI 209 UMMISCO, IRD/UPMC (XText-based GAML), 2010-2011
 * - Romain Lavaud, UMI 209 UMMISCO, IRD/UPMC (RCP environment), 2010
 * - Francois Sempe, UMI 209 UMMISCO, IRD/UPMC (EMF model, Batch), 2007-2009
 * - Edouard Amouroux, UMI 209 UMMISCO, IRD/UPMC (C++ initial porting), 2007-2008
 * - Chu Thanh Quang, UMI 209 UMMISCO, IRD/UPMC (OpenMap integration), 2007-2008
 */
package msi.gama.util.file;

import java.io.*;
import java.util.List;
import msi.gama.metamodel.shape.ILocation;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaList;
import msi.gama.util.matrix.IMatrix;
import msi.gaml.operators.Files;
import msi.gaml.types.*;
import au.com.bytecode.opencsv.CSVReader;
import com.vividsolutions.jts.geom.Envelope;

public class GamaTextFile extends GamaFile<Integer, String> {

	private String csvSeparator = null;

	public GamaTextFile(final IScope scope, final String pathName) throws GamaRuntimeException {
		super(scope, pathName);
	}

	@Override
	protected void checkValidity() throws GamaRuntimeException {
		super.checkValidity();
		if ( !GamaFileType.isTextFile(getFile().getName()) ) { throw GamaRuntimeException.error("The extension " +
			this.getExtension() + " is not recognized for text files"); }
	}

	@Override
	protected IGamaFile _copy(final IScope scope) {
		return null;
	}

	public void setCsvSeparators(final String string) {
		if ( string.length() >= 1 ) {
			csvSeparator = string;
		}
	}

	//
	// @Override
	// protected boolean _isFixedLength() {
	// return false;
	// }

	@Override
	protected IMatrix _matrixValue(final IScope scope, final ILocation preferredSize) throws GamaRuntimeException {
		if ( csvSeparator != null ) {
			try {
				CSVReader reader = new CSVReader(new FileReader(getPath()), csvSeparator.charAt(0));
				List<String[]> strings = reader.readAll();
				return GamaMatrixType.from(scope, strings, preferredSize);
			} catch (FileNotFoundException e) {} catch (IOException e) {
				throw new GamaRuntimeException(e);
			}
		} else {
			final String string = stringValue(scope);
			if ( string == null ) { return null; }
			return GamaMatrixType.from(scope, string, preferredSize); // Necessary ?
		}
		return null;
	}

	@Override
	public String _stringValue(final IScope scope) throws GamaRuntimeException {
		getContents(scope);
		StringBuilder sb = new StringBuilder(buffer.length(scope) * 200);
		for ( String s : buffer.iterable(scope) ) {
			sb.append(s).append("\n"); // TODO Factorize the different calls to "new line" ...
		}
		sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	@Override
	public String getKeyword() {
		return Files.TEXT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see msi.gama.util.GamaFile#fillBuffer()
	 */
	@Override
	protected void fillBuffer(final IScope scope) throws GamaRuntimeException {
		if ( buffer != null ) { return; }
		try {
			final BufferedReader in = new BufferedReader(new FileReader(getFile()));
			final GamaList<String> allLines = new GamaList();
			String str;
			str = in.readLine();
			while (str != null) {
				allLines.add(str);
				str = in.readLine();
			}
			in.close();
			buffer = allLines;
		} catch (final IOException e) {
			throw GamaRuntimeException.create(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see msi.gama.util.GamaFile#flushBuffer()
	 */
	@Override
	protected void flushBuffer() throws GamaRuntimeException {
		// TODO A faire.

	}

	@Override
	public Envelope computeEnvelope(final IScope scope) {
		Envelope boundsEnv = null;
		if ( getExtension().equals("asc") ) {
			try {
				File ascFile = getFile();
				InputStream ips = new FileInputStream(ascFile);
				InputStreamReader ipsr = new InputStreamReader(ips);
				BufferedReader in = new BufferedReader(ipsr);

				String[] nbColsStr = in.readLine().split(" ");
				int nbCols = Integer.valueOf(nbColsStr[nbColsStr.length - 1]);
				String[] nbRowsStr = in.readLine().split(" ");
				int nbRows = Integer.valueOf(nbRowsStr[nbRowsStr.length - 1]);
				String[] xllcornerStr = in.readLine().split(" ");
				double xllcorner = Double.valueOf(xllcornerStr[xllcornerStr.length - 1]);
				String[] yllcornerStr = in.readLine().split(" ");
				double yllcorner = Double.valueOf(yllcornerStr[yllcornerStr.length - 1]);
				String[] cellSizeStr = in.readLine().split(" ");
				double cellSize = Double.valueOf(cellSizeStr[cellSizeStr.length - 1]);
				boundsEnv =
					new Envelope(xllcorner, xllcorner + cellSize * nbCols, yllcorner, yllcorner + cellSize * nbRows);
				in.close();
			} catch (IOException e) {
				throw GamaRuntimeException.create(e);
			}
		}
		return boundsEnv;

	}

}
