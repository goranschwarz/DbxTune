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
package com.dbxtune.gui;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.apache.commons.io.FileUtils;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.dbxtune.Version;
import com.dbxtune.sql.showplan.ShowplanHtmlView;
import com.dbxtune.ui.rsyntaxtextarea.AsetuneSyntaxConstants;
import com.dbxtune.ui.rsyntaxtextarea.RSyntaxTextAreaX;
import com.dbxtune.utils.SqlServerUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

/**
 * SQL Server Showplan Viewer
 * <p>
 * A GUI to paste or load SQL Server XML Showplans and view them in a graphical way.
 */
public class SqlServerPlanViewer
extends JFrame
implements ActionListener
{
    private static final long serialVersionUID = 1L;

    private JButton           _viewPlan_but      = new JButton("View Plan");
    private JButton           _loadClipboard_but = new JButton("Load Clipboard");
    private JButton           _loadFile_but      = new JButton("Load File");
    private JButton           _close_but         = new JButton("Close");

    private RSyntaxTextAreaX  _xmlText           = new RSyntaxTextAreaX(15, 80);
    private RTextScrollPane   _xmlScroll         = new RTextScrollPane(_xmlText);

    private RSyntaxTextAreaX  _sqlText           = new RSyntaxTextAreaX(5, 80);
    private RTextScrollPane   _sqlScroll         = new RTextScrollPane(_sqlText);

    private static String     _lastFileLoaded    = null;
    private final static String TITLE            = "SQL Server Showplan Viewer";

    public SqlServerPlanViewer()
    {
        this(null);
    }

    public SqlServerPlanViewer(String xmlPlan)
    {
        super(TITLE);
        
        ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/sqlserver_plan_viewer_16.png");
        ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/sqlserver_plan_viewer_32.png");
        if (icon16 != null || icon32 != null)
        {
            ArrayList<Image> iconList = new ArrayList<Image>();
            if (icon16 != null) iconList.add(icon16.getImage());
            if (icon32 != null) iconList.add(icon32.getImage());
            setIconImages(iconList);
        }

        init();
        
        if (StringUtil.hasValue(xmlPlan))
        {
            _xmlText.setText(xmlPlan);
            updateSqlFromXml();
        }
    }

    private void init()
    {
        setLayout(new MigLayout("insets 5 5 5 5, fill"));

        _xmlText.setText("--NOT-YET-IMPLEMENTED--");
        _sqlText.setText("--NOT-YET-IMPLEMENTED--");

        _xmlText.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        _sqlText.setSyntaxEditingStyle(AsetuneSyntaxConstants.SYNTAX_STYLE_MSSQL_TSQL);
        _sqlText.setEditable(false);

        _xmlText.addCaretListener(e -> updateSqlFromXml());

        JPanel butPanel = new JPanel(new MigLayout("insets 0 0 0 0"));
        butPanel.add(_viewPlan_but);
        butPanel.add(_loadClipboard_but);
        butPanel.add(_loadFile_but);
        butPanel.add(_close_but, "gapleft push");

        _viewPlan_but.addActionListener(this);
        _loadClipboard_but.addActionListener(this);
        _loadFile_but.addActionListener(this);
        _close_but.addActionListener(this);

        _viewPlan_but.setToolTipText("View the graphical execution plan in your browser");
        _loadClipboard_but.setToolTipText("Load XML from the clipboard");
        _loadFile_but.setToolTipText("Load XML from a file (.sqlplan)");

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, _xmlScroll, _sqlScroll);
        split.setDividerLocation(400);
        split.setResizeWeight(0.8);

        add(split, "grow, push, wrap");
        add(butPanel, "growx");

        pack();
        setSize(SwingUtils.hiDpiScale(900), SwingUtils.hiDpiScale(700));
        SwingUtils.centerWindow(this);
    }

    private void updateSqlFromXml()
    {
        String xml = _xmlText.getText();
        String sql = SqlServerUtils.getSqlTextFromXmlPlan(xml);
        if (StringUtil.hasValue(sql))
        {
            _sqlText.setText(sql);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        if (_viewPlan_but.equals(source))
        {
            String xml = _xmlText.getText();
            if (StringUtil.hasValue(xml))
            {
                ShowplanHtmlView.show(ShowplanHtmlView.Type.SQLSERVER, xml);
            }
            else
            {
                SwingUtils.showInfoMessage(this, "No XML", "Please paste or load a SQL Server XML Showplan first.");
            }
        }
        else if (_loadClipboard_but.equals(source))
        {
            String clip = SwingUtils.getClipboardContents();
            if (StringUtil.hasValue(clip))
            {
                _xmlText.setText(clip);
                updateSqlFromXml();
            }
        }
        else if (_loadFile_but.equals(source))
        {
            JFileChooser chooser = new JFileChooser(_lastFileLoaded);
            chooser.setDialogTitle("Select a SQL Server XML Showplan file");
            int returnVal = chooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION)
            {
                File file = chooser.getSelectedFile();
                _lastFileLoaded = file.getAbsolutePath();
                try
                {
                    String content = FileUtils.readFileToString(file, "UTF-8");
                    _xmlText.setText(content);
                    updateSqlFromXml();
                }
                catch (IOException ex)
                {
                    SwingUtils.showErrorMessage(this, "Error loading file", "Could not load file: " + file.getAbsolutePath(), ex);
                }
            }
        }
        else if (_close_but.equals(source))
        {
            setVisible(false);
            dispose();
        }
    }
}
