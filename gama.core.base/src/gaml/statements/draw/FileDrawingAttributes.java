/*******************************************************************************************************
 *
 * gaml.statements.draw.FileDrawingAttributes.java, in plugin gama.core, is part of the source code of the GAMA
 * modeling and simulation platform (v. 1.8)
 *
 * (c) 2007-2018 UMI 209 UMMISCO IRD/SU & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gaml.statements.draw;

import org.locationtech.jts.geom.ShapeType;

import gama.common.geometry.AxisAngle;
import gama.common.geometry.Scaling3D;
import gama.common.interfaces.IAgent;
import gama.metamodel.shape.GamaPoint;
import gama.util.GamaColor;

public class FileDrawingAttributes extends DrawingAttributes {

	public final IAgent agentIdentifier;
	public boolean useCache = true;

	public FileDrawingAttributes(final Scaling3D size, final AxisAngle rotation, final GamaPoint location,
			final GamaColor color, final GamaColor border, final IAgent agent, final Double lineWidth,
			final boolean isImage, final Boolean lighting) {
		super(size, rotation, location, color, border, lighting);
		this.agentIdentifier = agent;
		setLineWidth(lineWidth);
		setType(isImage ? ShapeType.POLYGON : ShapeType.THREED_FILE);
	}

	public FileDrawingAttributes(final GamaPoint location, final boolean isImage) {
		super(null, null, location, null, null, null);
		agentIdentifier = null;
		setType(isImage ? ShapeType.POLYGON : ShapeType.THREED_FILE);
	}

	@Override
	public boolean useCache() {
		return useCache;
	}

	@Override
	public IAgent getAgentIdentifier() {
		return agentIdentifier;
	}

	public void setUseCache(final boolean b) {
		useCache = b;
	}

}