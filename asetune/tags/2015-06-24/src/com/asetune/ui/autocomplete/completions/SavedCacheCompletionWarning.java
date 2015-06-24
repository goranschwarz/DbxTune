package com.asetune.ui.autocomplete.completions;

import java.io.File;
import java.util.Date;

import com.asetune.ui.autocomplete.CompletionProviderAbstract;
import com.asetune.utils.TimeUtils;

public class SavedCacheCompletionWarning
extends ShorthandCompletionX
{
	private static final long serialVersionUID = 1L;

	private File _file;
	private int  _restoredSize;

	public SavedCacheCompletionWarning(CompletionProviderAbstract provider, File file, int restoredSize)
	{
		super(provider, "warning", "warning", "WARNING - <font color=\"red\">The Completion list has been restored, it may not reflect what's in the database.</font>"); 

		_file         = file;
		_restoredSize = restoredSize;
	}

	@Override
	public String getSummary()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<B>WARNING</B> - <font color=\"red\">The Completion list has been restored, it may not reflect what's in the database</font>");
		sb.append("<HR>"); // add Horizontal Ruler: ------------------
		sb.append("<BR>");
		sb.append("<B>Restored entries:</B> ")  .append(_restoredSize).append("<BR>");
		sb.append("<B>Restored from file:</B> ").append(_file).append("<BR>");
		sb.append("<BR>");
		sb.append("<B>File date:</B> ")         .append(new Date(_file.lastModified())).append("<BR>");
		sb.append("<B>Age in days:</B> ")       .append( TimeUtils.datediff(TimeUtils.DD_DAY,  _file.lastModified(),System.currentTimeMillis()) ).append("<BR>");
		sb.append("<B>Age in hours:</B> ")      .append( TimeUtils.datediff(TimeUtils.DD_HOUR, _file.lastModified(),System.currentTimeMillis()) ).append("<BR>");
		sb.append("<BR>");
		sb.append("<BR>");
		sb.append("<B>Note 1</B>: You can clear the saved cache by: Hit the '<i>Code Completion</i>' button, and press '<i>Clear</i>'.<BR>");
		sb.append("<B>Note 2</B>: You can also configure how this should work under: The '<i>Code Completion</i>' button, and press '<i>Configure</i>'.<BR>");
		sb.append("<HR>");
		sb.append("-end-<BR><BR>");
		
		return sb.toString();
	}

	/**
	 * Make it HTML aware
	 */
	@Override
	public String toString()
	{
		return "<html><body>" + super.toString() + "</body></html>";
	}
}
