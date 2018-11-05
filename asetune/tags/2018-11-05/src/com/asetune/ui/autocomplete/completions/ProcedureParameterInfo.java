package com.asetune.ui.autocomplete.completions;

import java.io.Serializable;

/**
 * Holds information about parameters
 */
public class ProcedureParameterInfo
implements Serializable
{
	private static final long serialVersionUID = 1L;

	public String _paramName       = null;
	public int    _paramPos        = -1;
	public String _paramInOutType  = null;
	public String _paramType       = null;
//	public String _paramType2      = null;
	public int    _paramLength     = -1;
	public int    _paramIsNullable = -1;
	public String _paramRemark     = null;
	public String _paramDefault    = null;
//	public int    _paramPrec       = -1;
	public int    _paramScale      = -1;
}
