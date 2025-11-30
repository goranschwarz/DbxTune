/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.sybase.ddlgen.container;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Vector;

import com.sybase.ddlgen.DDLBaseException;
import com.sybase.ddlgen.DDLGlobalParameters;
import com.sybase.ddlgen.DDLThread;
import com.sybase.ddlgen.item.DDLBaseItem;
import com.sybase.ddlgen.item.IndexItem;
import com.sybase.ddlgen.sql.ASConnection;
import com.sybase.ddlgen.sql.ASQueryParser;
import com.sybase.ddlgen.sql.ASResultSet;

/**
 * NOTE: This class is just to override the one in DDLGen.jar, which has a bug:<br>
 * And the bug is: <b>Name</b> should be <b>IndexName</b> in SELECT after UNION, receiving error:<br> 
 *         <pre>
 *         Msg 7348, Level 15, State 1:
 *         Server 'someServerName', Line ...:
 *         Select expression results in more than one column having same name. Column name 'Name' is specified more than once
 *         </pre>
 *     <pre>
 *     FROM: localASResultSet.open(ASQueryParser.parseQuery("SELECT ''%'', ''%'', IndexName= case when valid_name(I.name)=0 then ''\"'' + I.name + ''\"'' else I.name end, I.indid, I.status, ''%'', I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..syssegments S WHERE I.id ={1} AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND (I.status & 16 = 16 OR I.status2 & 512 = 512) UNION SELECT ''%'', ''%'', Name      = case when valid_name(I.name)=0 then ''\"'' + I.name + ''\"'' else I.name end, I.indid, I.status, ''%'', I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..syssegments S WHERE I.id ={1} AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND I.status & 16 = 0 AND I.status & 512 = 0", arrayOfString2), "INDEXES_OF_TABLE_QUERY_PRE150");
 *       TO: localASResultSet.open(ASQueryParser.parseQuery("SELECT ''%'', ''%'', IndexName= case when valid_name(I.name)=0 then ''\"'' + I.name + ''\"'' else I.name end, I.indid, I.status, ''%'', I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..syssegments S WHERE I.id ={1} AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND (I.status & 16 = 16 OR I.status2 & 512 = 512) UNION SELECT ''%'', ''%'', IndexName = case when valid_name(I.name)=0 then ''\"'' + I.name + ''\"'' else I.name end, I.indid, I.status, ''%'', I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..syssegments S WHERE I.id ={1} AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND I.status & 16 = 0 AND I.status & 512 = 0", arrayOfString2), "INDEXES_OF_TABLE_QUERY_PRE150");
 *     </pre>
 *       
 * @author gorans
 *
 */
public class IndexContainer
  extends DDLBaseContainer
{
  private String _partitionStrategy = null;
  private Boolean _includeSysDefinePartition = null;
  
  public IndexContainer(DDLBaseContainer paramDDLBaseContainer, DDLGlobalParameters paramDDLGlobalParameters)
  {
    this._parent = paramDDLBaseContainer;
    setDDLGlobalParameters(paramDDLGlobalParameters);
  }
  
  public IndexContainer() {}
  
  public UserTableContainer getParentContainer()
  {
    return (UserTableContainer)this._parent;
  }
  
  public void open(ASConnection paramASConnection)
  {
    ASResultSet localASResultSet = null;
    
    String str1 = "";
    Object localObject1 = "";
    String str2 = null;
    String str3 = "";
    String str4 = "";
    Vector localVector = new Vector();
    Object localObject2;
    try
    {
      localASResultSet = new ASResultSet(paramASConnection);
      if (this._parent != null)
      {
        str4 = ((UserTableContainer)this._parent).getTableID();
        str1 = ((UserTableContainer)this._parent).getTableName();
        str3 = ((UserTableContainer)this._parent).getOwnerName();
        localObject1 = ((UserTableContainer)this._parent).getDBName();
        str2 = "%";
      }
      else
      {
        String str5 = this._ddlGlobalParameters.getObjectName();
        while (str5.length() > 0) {
          if (str5.indexOf('.') != -1)
          {
            localObject2 = str5.substring(0, str5.indexOf('.'));
            str5 = str5.substring(((String)localObject2).length() + 1);
            localVector.addElement(localObject2);
          }
          else
          {
            localVector.addElement(str5);
          }
        }
        if (localVector.size() == 4)
        {
          localObject1 = (String)localVector.elementAt(0);
          str3 = (String)localVector.elementAt(1);
        }
        else if (localVector.size() == 3)
        {
          localObject1 = this._ddlGlobalParameters.getDatabaseName();
          localObject2 = (String)localVector.elementAt(0);
          boolean bool;
          if (localObject1 == null)
          {
            localObject1 = getDefaultDatabaseName(paramASConnection);
            bool = checkValidity((String)localObject1, (String)localObject2, localASResultSet);
            if (bool) {
              str3 = (String)localVector.elementAt(0);
            } else {
              localObject1 = (String)localVector.elementAt(0);
            }
          }
          else if (((String)localObject2).equals(localObject1))
          {
            bool = checkValidity((String)localObject1, (String)localObject2, localASResultSet);
            if (bool) {
              new DDLBaseException("I1", "DATABASE_TABLE_NAME_ERROR", 2);
            } else {
              localObject1 = localObject2;
            }
          }
          else
          {
            if (((String)localObject2).equals("%")) {
              bool = true;
            } else {
              bool = checkValidity((String)localObject1, (String)localObject2, localASResultSet);
            }
            if (bool) {
              str3 = (String)localVector.elementAt(0);
            } else {
              localObject1 = localObject2;
            }
          }
        }
        else if (localVector.size() == 2)
        {
          localObject1 = this._ddlGlobalParameters.getDatabaseName();
          if (localObject1 == null) {
            localObject1 = super.getDefaultDatabaseName(paramASConnection);
          }
        }
        else if ((localVector.size() == 2) || (localVector.size() > 4))
        {
          new DDLBaseException("I2", "TABLE_INDEX_ERROR", 2);
        }
        if (localVector.size() < 2)
        {
          new DDLBaseException("I3", "INVALID_OBJECTNAME_ERROR", 2, this._ddlGlobalParameters.getObjectName());
        }
        else
        {
          str1 = (String)localVector.elementAt(localVector.size() - 2);
          str2 = ignorePatternCharacters((String)localVector.elementAt(localVector.size() - 1));
        }
        paramASConnection.verifyDBName((String)localObject1);
      }
    }
    catch (Exception localException1)
    {
      new DDLBaseException("I4", localException1, "INTERNAL_ERROR", 1);
    }
    if (str3.equals("")) {
      str3 = "dbo";
    }
    try
    {
      if (localASResultSet != null)
      {
        String[] arrayOfString2;
        if (this._parent != null)
        {
//          arrayOfString2 = new String[] { localObject1, str4 };
          arrayOfString2 = new String[] { (String)localObject1, str4 };
          if (paramASConnection.getServerVersion().compareTo("15") >= 0) {
            localASResultSet.open(ASQueryParser.parseQuery("SELECT ''%'', ''%'', IndexName= case when valid_name(I.name ,255)=0 then ''\"'' + I.name + ''\"'' else I.name end, I.indid, I.status, ''%'', I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..syssegments S WHERE I.id ={1} AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND (I.status & 16 = 16 OR I.status2 & 512 = 512) UNION SELECT ''%'', ''%'', IndexName= case when valid_name(I.name ,255)=0 then ''\"'' + I.name + ''\"'' else I.name end, I.indid, I.status, ''%'', I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..syssegments S WHERE I.id ={1} AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND I.status & 16 = 0 AND I.status2 & 512 = 0", arrayOfString2), "INDEXES_OF_TABLE_QUERY");
          } else {
            localASResultSet.open(ASQueryParser.parseQuery("SELECT ''%'', ''%'', IndexName= case when valid_name(I.name     )=0 then ''\"'' + I.name + ''\"'' else I.name end, I.indid, I.status, ''%'', I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..syssegments S WHERE I.id ={1} AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND (I.status & 16 = 16 OR I.status2 & 512 = 512) UNION SELECT ''%'', ''%'', IndexName= case when valid_name(I.name     )=0 then ''\"'' + I.name + ''\"'' else I.name end, I.indid, I.status, ''%'', I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..syssegments S WHERE I.id ={1} AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND I.status & 16 = 0 AND I.status & 512 = 0", arrayOfString2), "INDEXES_OF_TABLE_QUERY_PRE150");
          }
        }
        else
        {
          if (str2 == null) {
            str2 = "%";
          }
//          arrayOfString2 = new String[] { localObject1, str1.indexOf("\"") == 0 ? str1.substring(1, str1.length() - 1) : str1, str2.indexOf("\"") == 0 ? str2.substring(1, str2.length() - 1) : str2, str3 };
          arrayOfString2 = new String[] { (String)localObject1, str1.indexOf("\"") == 0 ? str1.substring(1, str1.length() - 1) : str1, str2.indexOf("\"") == 0 ? str2.substring(1, str2.length() - 1) : str2, str3 };
          if (paramASConnection.getServerVersion().compareTo("15") >= 0)
          {
            localObject2 = ASQueryParser.parseQuery("SELECT ''{0}'',TableName = case when valid_name(O.name ,255)=0 then ''\"'' + O.name + ''\"'' else O.name end,IndexName= case when valid_name(I.name ,255)=0 then ''\"'' + I.name + ''\"'' else I.name end,I.indid, I.status, U.name, I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..sysobjects O, {0}..sysusers U, {0}..syssegments S WHERE U.uid = O.uid AND I.id = O.id AND O.type = ''U'' and O.uid = U.uid and O.name like ''{1}'' and U.name like ''{3}'' AND I.name like ''{2}'' AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND (I.status & 16 = 16 OR I.status2 & 512 = 512)", arrayOfString2);
            localObject2 = (String)localObject2 + ASQueryParser.parseQuery("UNION SELECT ''{0}'',TableName = case when valid_name(O.name ,255)=0 then ''\"'' + O.name + ''\"'' else O.name end,IndexName= case when valid_name(I.name ,255)=0 then ''\"'' + I.name + ''\"'' else I.name end,I.indid, I.status, U.name, I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..sysobjects O, {0}..sysusers U, {0}..syssegments S WHERE U.uid = O.uid AND I.id = O.id AND  type = ''U'' and O.uid = U.uid and O.name like ''{1}'' and U.name like ''{3}'' AND I.name like ''{2}'' AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND I.status & 16 = 0 AND I.status2 & 512 = 0", arrayOfString2);
            localASResultSet.open((String)localObject2, "INDEXESCTR_OPEN_QUERY_PART1+2");
          }
          else
          {
            localObject2 = ASQueryParser.parseQuery("SELECT ''{0}'',TableName = case when valid_name(O.name)=0 then ''\"'' + O.name + ''\"'' else O.name end,IndexName= case when valid_name(I.name)=0 then ''\"'' + I.name + ''\"'' else I.name end,I.indid, I.status, U.name, I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..sysobjects O, {0}..sysusers U, {0}..syssegments S WHERE U.uid = O.uid AND I.id = O.id AND O.type = ''U'' and O.uid = U.uid and O.name like ''{1}'' and U.name like ''{3}'' AND I.name like ''{2}'' AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND (I.status & 16 = 16 OR I.status2 & 512 = 512)", arrayOfString2);
            localObject2 = (String)localObject2 + ASQueryParser.parseQuery("UNION SELECT ''{0}'',TableName = case when valid_name(O.name)=0 then ''\"'' + O.name + ''\"'' else O.name end,IndexName= case when valid_name(I.name)=0 then ''\"'' + I.name + ''\"'' else I.name end,I.indid, I.status, U.name, I.keycnt, I.maxrowsperpage, I.fill_factor, I.res_page_gap,S.name,I.status2 FROM {0}..sysindexes I, {0}..sysobjects O, {0}..sysusers U, {0}..syssegments S WHERE U.uid = O.uid AND I.id = O.id AND  type = ''U'' and O.uid = U.uid and O.name like ''{1}'' and U.name like ''{3}'' AND I.name like ''{2}'' AND I.segment = S.segment AND I.indid BETWEEN 1 AND 254 AND (I.status2 & 2) != 2 AND I.status & 16 = 0 AND I.status2 & 512 = 0", arrayOfString2);
            localASResultSet.open((String)localObject2, "INDEXESCTR_OPEN_QUERY_PART1+2_PRE150");
          }
        }
      }
    }
    catch (SQLException localSQLException1)
    {
      new DDLBaseException("I5", localSQLException1, 2);
    }
    catch (Exception localException2)
    {
      new DDLBaseException("I6", localException2, "INTERNAL_ERROR", 1);
    }
    int i = 0;
    try
    {
      while (localASResultSet.getNextRow())
      {
        String[] arrayOfString1 = localASResultSet.getColumnsAsArray();
        localObject2 = new IndexItem(arrayOfString1, this);
        i++;
        DDLThread localDDLThread = this._threadPool.getFreeThread();
        if (localDDLThread != null) {
          localDDLThread.setItem((DDLBaseItem)localObject2);
        } else {
          ((IndexItem)localObject2).open(paramASConnection);
        }
        this._children.addElement(localObject2);
      }
      if (localASResultSet != null) {
        localASResultSet.close();
      }
    }
    catch (SQLException localSQLException2)
    {
      new DDLBaseException("I7", localSQLException2, 2);
    }
    catch (Exception localException3)
    {
      new DDLBaseException("I8", localException3, "INTERNAL_ERROR", 1);
    }
    if ((i == 0) && (this._parent == null)) {
      new DDLBaseException("I9", "INDEX_NOT_FOUND_ERROR", 2, (String)localObject1 + "." + str3 + "." + str1 + "." + str2);
    }
    if (localASResultSet != null) {
      localASResultSet = null;
    }
    if (i > 0)
    {
      generateDDL();
    }
    else if ((i == 0) && (this._parent == null))
    {
      this._ddlString = "";
      setThreadPoolStatus();
      this._ddlGlobalParameters.setDDLStatus((byte)1);
    }
    else
    {
      this._ddlString = "";
    }
  }
  
  private boolean checkValidity(String paramString1, String paramString2, ASResultSet paramASResultSet)
  {
    String[] arrayOfString = { paramString1, paramString2 };
    
    int i = 0;
    try
    {
      if (paramASResultSet != null) {
        paramASResultSet.open(ASQueryParser.parseQuery("SELECT name FROM {0}..sysusers WHERE name = ''{1}''", arrayOfString), "VERIFY_USERNAME");
      }
      while (paramASResultSet.getNextRow()) {
        i++;
      }
    }
    catch (SQLException localSQLException)
    {
      new DDLBaseException("I10", localSQLException, 2);
    }
    catch (Exception localException)
    {
      new DDLBaseException("I11", localException, "INTERNAL_ERROR", 1);
    }
    if (i == 1) {
      return true;
    }
    return false;
  }
  
  public String getPartitionStrategy(ASConnection paramASConnection, String[] paramArrayOfString)
  {
    if (this._partitionStrategy != null) {
      return this._partitionStrategy;
    }
    if (getParentContainer() != null) {
      this._partitionStrategy = getParentContainer().getPartitionStrategy();
    } else {
      setPartitionStrategy(paramASConnection, paramArrayOfString);
    }
    return this._partitionStrategy;
  }
  
  private void setPartitionStrategy(ASConnection paramASConnection, String[] paramArrayOfString)
  {
    try
    {
      ASResultSet localASResultSet = new ASResultSet(paramASConnection);
      localASResultSet.open(ASQueryParser.parseQuery("select  partition_type = isnull((select v.name from master.dbo.spt_values v where v.type = ''PN'' and v.number = i.partitiontype), ''roundrobin'') from {0}.dbo.sysobjects o, {0}.dbo.sysindexes i where  o.id = object_id(''{0}.{1}.{2}'') and i.id = o.id and i.indid <1 ", paramArrayOfString), "PARTITION_TYPE_INFO");
      if (localASResultSet.getNextRow()) {
        this._partitionStrategy = localASResultSet.getColumnString("partition_type");
      }
    }
    catch (SQLException localSQLException)
    {
      new DDLBaseException("I12", localSQLException, 2, this._name);
    }
    catch (Exception localException)
    {
      new DDLBaseException("I13", localException, "INTERNAL_ERROR", 1);
    }
  }
  
  public boolean includeSysDefinedPartition()
  {
    if (this._includeSysDefinePartition != null) {
      return this._includeSysDefinePartition.booleanValue();
    }
    if (getParentContainer() != null)
    {
      this._includeSysDefinePartition = Boolean.valueOf(getParentContainer().includeSysDefinedPartition());
    }
    else
    {
      ArrayList localArrayList = this._ddlGlobalParameters.getExtendedObjectTypes();
      this._includeSysDefinePartition = Boolean.valueOf(localArrayList.contains("PN"));
    }
    return this._includeSysDefinePartition.booleanValue();
  }
}
