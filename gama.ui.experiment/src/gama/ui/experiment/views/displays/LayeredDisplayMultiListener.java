/*********************************************************************************************
 *
 * 'LayeredDisplayMultiListener.java, in plugin gama.ui.experiment.experiment, is part of the source code of the GAMA
 * modeling and simulation platform. (c) 2007-2016 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package gama.ui.experiment.views.displays;

import java.util.function.Consumer;

import org.eclipse.swt.graphics.Point;

import gama.GAMA;
import gama.common.interfaces.gui.IGui;
import gama.common.interfaces.outputs.IDisplaySurface;
import gama.common.util.PlatformUtils;
import gama.ui.base.utils.WorkbenchHelper;

public class LayeredDisplayMultiListener {

	private final LayeredDisplayDecorator view;
	private final IDisplaySurface surface;
	volatile boolean mouseIsDown;
	volatile boolean inMenu;
	volatile long lastEnterTime;
	volatile Point lastEnterPosition = new Point(0, 0);
	volatile boolean suppressNextEnter;
	final Consumer<Integer> keyListener;

	public LayeredDisplayMultiListener(final IDisplaySurface surface, final LayeredDisplayDecorator deco) {
		this.view = deco;
		this.surface = surface;

		keyListener = (keyCode) -> {
			switch (keyCode.intValue()) {
				case 'o':
					deco.toggleOverlay();
					break;
				case 'l':
					deco.toggleSideControls();
					break;
				case 'k':
					if (deco.isFullScreen()) {
						deco.toggleInteractiveConsole();
					}
					break;
				case 't':
					deco.toggleToolbar();
			}
		};
	}

	public void keyPressed(final char e) {
		surface.dispatchKeyEvent(e);
		WorkbenchHelper.asyncRun(view.displayOverlay);
	}

	public void keyReleased(final int e, final boolean command) {
		if (!command)
			return;
		keyListener.accept(e);
	}

	public void mouseEnter(final int x, final int y, final boolean modifier, final int button) {
		if (suppressNextEnter) {
			// DEBUG.LOG("One mouse enter suppressed");
			suppressNextEnter = false;
			return;
		}
		if (modifier)
			return;

		setMousePosition(x, y);
		if (button > 0)
			return;
		final long currentTime = System.currentTimeMillis();
		if (currentTime - lastEnterTime < 100 && lastEnterPosition.x == x && lastEnterPosition.y == y)
			return;
		lastEnterTime = System.currentTimeMillis();
		lastEnterPosition = new Point(x, y);
		// DEBUG.LOG("Mouse entering " + e);
		surface.dispatchMouseEvent(IGui.MouseEnter);
	}

	public void mouseExit(final int x, final int y, final boolean modifier, final int button) {
		final long currentTime = System.currentTimeMillis();
		if (currentTime - lastEnterTime < 100 && lastEnterPosition.x == x && lastEnterPosition.y == y)
			return;
		setMousePosition(-1, -1);
		if (button > 0)
			return;
		// DEBUG.LOG("Mouse exiting " + e);
		surface.dispatchMouseEvent(IGui.MouseExit);
		// if (!view.isFullScreen() && WorkaroundForIssue1353.isInstalled()) {
		// suppressNextEnter = true;
		// DEBUG.LOG("Invoking WorkaroundForIssue1353");
		// WorkaroundForIssue1353.showShell();
		// }

	}

	public void mouseHover(final int button) {
		if (button > 0)
			return;
		// DEBUG.LOG("Mouse hovering on " + view.getPartName());
		surface.dispatchMouseEvent(IGui.MouseHover);
	}

	public void mouseMove(final int x, final int y, final boolean modifier) {
		WorkbenchHelper.asyncRun(view.displayOverlay);
		if (modifier)
			return;
		// DEBUG.LOG("Mouse moving on " + view.getPartName());

		if (mouseIsDown) {
			surface.draggedTo(x, y);
			surface.dispatchMouseEvent(IGui.DragDetect);
		} else {
			setMousePosition(x, y);
			surface.dispatchMouseEvent(IGui.MouseMove);
		}

	}

	/**
	 * Mouse down event fired
	 *
	 * @param x
	 *            the x coordinate relative to the display (in pixels, not model coordinates)
	 * @param y
	 *            the y coordinate relative to the display (in pixels, not model coordinates)
	 * @param button
	 *            the button clicked (1 for left, 2 for middle, 3 for right)
	 * @param modifier
	 *            whetehr ALT, CTRL, CMD, META or other modifiers are used
	 */
	public void mouseDown(final int x, final int y, final int button, final boolean modifier) {
		setMousePosition(x, y);
		if (inMenu) {
			inMenu = false;
			return;
		}
		if (modifier)
			return;
		if (PlatformUtils.isWindows() && button == 3)
			// see Issue #2756: Windows emits the mouseDown(...) event *before* the menuDetected(..) one.
			// No need to patch mouseUp(...) right now
			return;
		mouseIsDown = true;
		// DEBUG.LOG("Mouse down on " + view.getPartName());
		surface.dispatchMouseEvent(IGui.MouseDown);
	}

	/**
	 * Mouse up event fired
	 *
	 * @param x
	 *            the x coordinate relative to the display (in pixels, not model coordinates)
	 * @param y
	 *            the y coordinate relative to the display (in pixels, not model coordinates)
	 * @param button
	 *            the button clicked (1 for left, 2 for middle, 3 for right)
	 * @param modifier
	 *            whetehr ALT, CTRL, CMD, META or other modifiers are used
	 */
	public void mouseUp(final int x, final int y, final int button, final boolean modifier) {
		// In case the mouse has moved (for example on a menu)
		if (!mouseIsDown)
			return;
		setMousePosition(x, y);
		if (modifier)
			return;
		mouseIsDown = false;
		// DEBUG.LOG("Mouse up on " + view.getPartName());
		// if (!view.isFullScreen() && WorkaroundForIssue1353.isInstalled()) {
		// WorkaroundForIssue1353.showShell();
		// }
		surface.dispatchMouseEvent(IGui.MouseUp);
	}

	public void menuDetected(final int x, final int y) {
		if (inMenu)
			return;
		if (inMenu)
			return;
		// DEBUG.LOG("Menu detected on " + view.getPartName());
		inMenu = true;
		inMenu = surface.canTriggerContextualMenu();
		setMousePosition(x, y);
		surface.selectAgentsAroundMouse();
		surface.dispatchMouseEvent(IGui.MenuDetect);
		if (inMenu) {
			surface.selectAgentsAroundMouse();
		}
	}

	public void dragDetected() {
		// DEBUG.LOG("Mouse drag detected on " + view.getPartName());
		// surface.draggedTo(e.x, e.y);
		surface.dispatchMouseEvent(IGui.DragDetect);
	}

	public void focusGained() {
		// if (!ok()) { return; }
		// if (suppressNextEnter) {
		// DEBUG.LOG("One mouse enter suppressed");
		// suppressNextEnter = false;
		// return;
		// }
		// DEBUG.LOG("Control has gained focus");
		// surface.dispatchMouseEvent(SWT.MouseEnter);
		// Thread.dumpStack();
	}

	public void focusLost() {
		// if (!ok()) { return; }
		// surface.dispatchMouseEvent(SWT.MouseExit);

		// DEBUG.LOG("Control has lost focus");
		// Thread.dumpStack();
	}

	private void setMousePosition(final int x, final int y) {
		surface.setMousePosition(x, y);
		GAMA.getGui().setMouseLocationInModel(surface.getModelCoordinates());
	}

}