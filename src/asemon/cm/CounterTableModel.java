package asemon.cm;

import java.util.List;

import javax.swing.table.AbstractTableModel;

public abstract class CounterTableModel
extends AbstractTableModel
{
	private static final long serialVersionUID = 1L;

	abstract public int    getRowNumberForPkValue(String pkStr);
	abstract public String getPkValue(int row);
	abstract public List<String> getColNames();
}
