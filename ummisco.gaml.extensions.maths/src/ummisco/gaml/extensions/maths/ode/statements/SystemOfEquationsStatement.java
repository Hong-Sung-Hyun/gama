package ummisco.gaml.extensions.maths.ode.statements;

import java.util.*;
import msi.gama.common.interfaces.IKeyword;
import msi.gama.common.util.GuiUtils;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.precompiler.GamlAnnotations.combination;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.facet;
import msi.gama.precompiler.GamlAnnotations.facets;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.*;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.*;
import msi.gaml.compilation.ISymbol;
import msi.gaml.descriptions.IDescription;
import msi.gaml.expressions.*;
import msi.gaml.operators.Cast;
import msi.gaml.species.GamlSpecies;
import msi.gaml.statements.AbstractStatementSequence;
import msi.gaml.types.IType;
import org.apache.commons.math3.exception.*;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import ummisco.gaml.extensions.maths.ode.utils.classicalEquations.epidemiology.*;
import ummisco.gaml.extensions.maths.ode.utils.classicalEquations.populationDynamics.ClassicalLVEquations;

@symbol(name = IKeyword.EQUATION, kind = ISymbolKind.SEQUENCE_STATEMENT, with_sequence = true)
@facets(value = {
	@facet(name = IKeyword.NAME, type = IType.ID /* CHANGE */, optional = false),
	@facet(name = IKeyword.TYPE, type = IType.ID /* CHANGE */, optional = true, values = { "SI", "SIS", "SIR", "SIRS",
		"SEIR", "LV" }, doc = @doc(value = "classical models (epidemiology or population dynamic)")),
	@facet(name = IKeyword.VARS, type = IType.LIST, optional = true),
	@facet(name = IKeyword.PARAMS, type = IType.LIST, optional = true),
	@facet(name = IKeyword.SIMULTANEOUSLY, type = IType.LIST, optional = true) }, combinations = {
	@combination({ IKeyword.NAME }), @combination({ IKeyword.NAME, IKeyword.TYPE, IKeyword.VARS, IKeyword.PARAMS }),
	@combination({ IKeyword.NAME, IKeyword.SIMULTANEOUSLY }) }, omissible = IKeyword.NAME)
@inside(kinds = { ISymbolKind.SPECIES, ISymbolKind.MODEL })
/**
 * The class SystemOfEquationsStatement. 
 * This class represents a system of equations (SingleEquationStatement) that implements the interface 
 * FirstOrderDifferentialEquations and can be integrated by any of the integrators available in the Apache Commons Library.
 * 
 * @author drogoul
 * @since 26 janv. 2013
 *
 */
public class SystemOfEquationsStatement extends AbstractStatementSequence implements FirstOrderDifferentialEquations {

	public final IList<SingleEquationStatement> equations = new GamaList<SingleEquationStatement>();
	public final IList<IExpression> variables_diff = new GamaList<IExpression>();
	public final IList<IExpression> variables_nondiff = new GamaList<IExpression>();
	// public final GamaMap variables=new GamaMap();
	public final GamaList<IAgent> equations_ext = new GamaList<IAgent>();
	public final GamaList<IAgent> equaAgents = new GamaList<IAgent>();
	// private GamaList integrated_times=new GamaList();
	// public GamaList integrate_value=new GamaList();
	public double equa_t = 0;
	public IScope currentScope;
	IExpression simultan = null;

	public SystemOfEquationsStatement(final IDescription desc) {
		super(desc);
		setName(getLiteral(IKeyword.NAME));
		simultan = getFacet(IKeyword.SIMULTANEOUSLY);

	}

	/**
	 * This method separates regular statements and equations.
	 * 
	 * @see msi.gaml.statements.AbstractStatementSequence#setChildren(java.util.List)
	 */
	@Override
	public void setChildren(final List<? extends ISymbol> commands) {
		List<? extends ISymbol> cmd = commands;
		if ( getFacet(IKeyword.TYPE) != null ) {
			if ( getFacet(IKeyword.TYPE).literalValue().equals("SIR") ) {
				cmd.clear();
				cmd =
					new ClassicalSIREquations(getDescription()).SIR(getFacet(IKeyword.VARS), getFacet(IKeyword.PARAMS));
			} else if ( getFacet(IKeyword.TYPE).literalValue().equals("SI") ) {
				cmd.clear();
				cmd = new ClassicalSIEquations(getDescription()).SI(getFacet(IKeyword.VARS), getFacet(IKeyword.PARAMS));
			} else if ( getFacet(IKeyword.TYPE).literalValue().equals("SIS") ) {
				cmd.clear();
				cmd =
					new ClassicalSISEquations(getDescription()).SIS(getFacet(IKeyword.VARS), getFacet(IKeyword.PARAMS));
			} else if ( getFacet(IKeyword.TYPE).literalValue().equals("SIRS") ) {
				cmd.clear();
				cmd =
					new ClassicalSIRSEquations(getDescription()).SIRS(getFacet(IKeyword.VARS),
						getFacet(IKeyword.PARAMS));
			} else if ( getFacet(IKeyword.TYPE).literalValue().equals("SEIR") ) {
				cmd.clear();
				cmd =
					new ClassicalSEIREquations(getDescription()).SEIR(getFacet(IKeyword.VARS),
						getFacet(IKeyword.PARAMS));
			} else if ( getFacet(IKeyword.TYPE).literalValue().equals("LV") ) {
				cmd.clear();
				cmd = new ClassicalLVEquations(getDescription()).LV(getFacet(IKeyword.VARS), getFacet(IKeyword.PARAMS));
			} else {
				GamaRuntimeException.error(getFacet(IKeyword.TYPE).literalValue().equals("SI") +
					" is not a recognized classical equation");
			}
		}

		final List<ISymbol> others = new ArrayList<ISymbol>();
		for ( final ISymbol s : cmd ) {
			if ( s instanceof SingleEquationStatement ) {
				((SingleEquationStatement) s).etablishVar();
				equations.add((SingleEquationStatement) s);
				for ( int i = 0; i < ((SingleEquationStatement) s).getVars().size(); i++ ) {
					IExpression v = ((SingleEquationStatement) s).getVar(i);

					if ( ((SingleEquationStatement) s).getOrder() > 0 ) {
						if ( !variables_diff.contains(v) ) {
							variables_diff.add(v);
						}
					} else {
						if ( !variables_nondiff.contains(v) ) {
							variables_nondiff.add(v);
						}
					}
				}

			} else {
				others.add(s);
			}
		}

		super.setChildren(others);
	}

	@Override
	public Object privateExecuteIn(final IScope scope) throws GamaRuntimeException {
		// We execute whatever is declared in addition to the equations (could
		// be initializations,
		// etc.)
		equaAgents.clear();
		for ( int i = 0; i < equations.size(); i++ ) {
			equaAgents.add(scope.getAgentScope());
		}
		if ( simultan != null ) {
			equations_ext.clear();
			final Object t = simultan.value(scope);
			if ( t != null ) {
				if ( t instanceof GamaList ) {
					final GamaList lst = ((GamaList) t).listValue(scope);
					for ( int i = 0; i < lst.size(); i++ ) {
						final Object o = lst.get(i);

						if ( o instanceof IAgent ) {
							final IAgent remoteAgent = (IAgent) o;
							if ( !remoteAgent.dead() && !equations_ext.contains(remoteAgent) ) {
								equations_ext.add(remoteAgent);
							}
						} else if ( o instanceof GamlSpecies ) {
							final Iterator<IAgent> ia = ((GamlSpecies) o).iterable(scope).iterator();
							while (ia.hasNext()) {
								final IAgent remoteAgent = ia.next();
								if ( !remoteAgent.dead() && !equations_ext.contains(remoteAgent) ) {
									equations_ext.add(remoteAgent);
								}
							}
						}
					}

				} else {
					if ( !equations_ext.contains(t) ) {
						equations_ext.add((IAgent) t);
					}
				}
			}
		}

		return super.privateExecuteIn(scope);
	}

	private void addEquationsExtern(final IAgent remoteAgent, final String eqName) {
		final SystemOfEquationsStatement ses =
			remoteAgent.getSpecies().getStatement(SystemOfEquationsStatement.class, eqName);
		if ( ses != null ) {
			// final int n = equaAgents.size();

			for ( int i = 0; i < ses.equations.size(); i++ ) {
				equaAgents.add(remoteAgent);
			}

			equations.addAll(ses.equations);

			variables_diff.addAll(ses.variables_diff);

		}

	}

	private void removeEquationsExtern(final IAgent remoteAgent, final String eqName) {
		final SystemOfEquationsStatement ses =
			remoteAgent.getSpecies().getStatement(SystemOfEquationsStatement.class, eqName);
		if ( ses != null ) {

			equations.removeAll(ses.equations);

			variables_diff.removeAll(ses.variables_diff);
		}

	}

	public void addExtern(final String eqName) {
		if ( equations_ext.size() > 0 ) {

			for ( int i = 0, n = equations_ext.size(); i < n; i++ ) {
				final IAgent remoteAgent = equations_ext.get(i);
				if ( !remoteAgent.dead() ) {
					addEquationsExtern(remoteAgent, eqName);
				}

			}

		}

	}

	public void removeExtern(final IScope scope, final String eqName) {
		if ( equations_ext.size() > 0 ) {
			for ( int i = 0, n = equations_ext.size(); i < n; i++ ) {
				final Object o = equations_ext.get(i);
				if ( o instanceof IAgent ) {
					final IAgent remoteAgent = (IAgent) o;
					if ( !remoteAgent.dead() ) {
						removeEquationsExtern(remoteAgent, eqName);
					}
				} else if ( o instanceof GamlSpecies ) {
					final Iterator<IAgent> ia = ((GamlSpecies) o).iterable(scope).iterator();
					while (ia.hasNext()) {
						final IAgent remoteAgent = ia.next();
						if ( !remoteAgent.dead() ) {
							removeEquationsExtern(remoteAgent, eqName);
						}
					}
				}
			}
			equaAgents.clear();
		}
	}

	/**
	 * This method is bound to be called by the integrator of the equations
	 * system (instantiated in SolveStatement).
	 * 
	 * @see org.apache.commons.math3.ode.FirstOrderDifferentialEquations#computeDerivatives(double, double[], double[])
	 */

	public void assignValue(final double time, final double[] y) {
		// TODO Should be rewritten in a more correct way (by calling
		// scope.setAgentVarValue(...)
		for ( int i = 0, n = equations.size(); i < n; i++ ) {
			final SingleEquationStatement s = equations.get(i);
			if ( s.getOrder() == 0 ) {
				continue;
			}

			final IAgent remoteAgent = equaAgents.get(i);
			boolean pushed = false;
			if ( !remoteAgent.dead() ) {
				pushed = currentScope.push(remoteAgent);
				try {
					if ( s.getVar_t() instanceof IVarExpression ) {
						((IVarExpression) s.getVar_t()).setVal(currentScope, time, false);
					}
					if ( variables_diff.get(i) instanceof IVarExpression ) {
						((IVarExpression) variables_diff.get(i)).setVal(currentScope, y[i], false);
					} else if ( variables_diff.get(i) instanceof MapExpression ) {
						System.out.println(((MapExpression) variables_diff.get(i)).valuesArray());

					}
				} catch (final Exception ex1) {
					GuiUtils.debug(ex1);
				} finally {
					if ( pushed ) {
						currentScope.pop(remoteAgent);
					}
				}
			}

		}

		for ( int i = 0, n = equations.size(); i < n; i++ ) {
			final SingleEquationStatement s = equations.get(i);
			if ( s.getOrder() == 0 ) {
				IExpression tmp = ((UnaryOperator) s.getFunction()).arg(0);
				Object v = s.getExpression().value(currentScope);
				if ( tmp instanceof AgentVariableExpression ) {
					((AgentVariableExpression) tmp).setVal(currentScope, v, false);
				}
			}

		}

	}

	@Override
	public void computeDerivatives(final double time, final double[] y, final double[] ydot)
		throws MaxCountExceededException, DimensionMismatchException {
		/*
		 * the y value is calculed automatically inside integrator's algorithm
		 * just get y, and assign value to Variables in GAMA, which is use by
		 * GAMA modeler
		 */

		assignValue(time, y);

		/*
		 * with variables assigned, calcul new value of expression in function
		 * loop through equations (internal and external) to get singleequation
		 * value
		 */

		// TODO Should be rewritten in a more correct way : scope.execute(s,
		// agent)...
		for ( int i = 0, n = getDimension(); i < n; i++ ) {

			boolean pushed = false;
			if ( equaAgents.size() > 0 ) {
				pushed = currentScope.push(equaAgents.get(i));
			}
			try {
				ydot[i] = Cast.asFloat(currentScope, equations.get(i).executeOn(currentScope));
			} catch (final Exception ex1) {
				GuiUtils.debug(ex1);
			} finally {
				if ( equaAgents.size() > 0 ) {
					if ( pushed ) {
						currentScope.pop(equaAgents.get(i));
					}
				}
			}
		}

	}

	/**
	 * The dimension of the equations system is simply, here, the number of
	 * equations.
	 * 
	 * @see org.apache.commons.math3.ode.FirstOrderDifferentialEquations#getDimension()
	 */
	@Override
	public int getDimension() {
		int count = 0;
		for ( int i = 0; i < equations.size(); i++ ) {
			if ( equations.get(i).getOrder() > 0 ) {
				count++;
			}
		}
		return count;
	}

	@Override
	public void leaveScope(final IScope scope) {
		currentScope = null;
		super.leaveScope(scope);
	}

	@Override
	public void enterScope(final IScope scope) {
		super.enterScope(scope);
		currentScope = scope;
	}
}
