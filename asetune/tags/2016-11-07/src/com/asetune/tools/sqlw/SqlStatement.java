package com.asetune.tools.sqlw;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JComponent;

public interface SqlStatement
{
	public Statement getStatement();
	
	public boolean execute() throws SQLException;

	public void readRpcReturnCodeAndOutputParameters(ArrayList<JComponent> resultCompList, boolean asPlainText) 
	throws SQLException;
}
