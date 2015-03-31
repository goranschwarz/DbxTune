package com.asetune.ui.autocomplete.completions;

import java.io.Serializable;

/**
* Holds information about columns
*/
public class FunctionColumnInfo
implements Serializable
{
	private static final long serialVersionUID = 1L;

	public String _colName       = null;
	public int    _colPos        = -1;
	public String _colType       = null;
//	public String _colType2      = null;
	public int    _colLength     = -1;
	public int    _colIsNullable = -1;
	public String _colRemark     = null;
	public String _colDefault    = null;
//	public int    _colPrec       = -1;
	public int    _colScale      = -1;
}

