package asemon.gui;

public interface ConnectionProgressCallback
{
	public static final int TASK_STATUS_CURRENT     = 0;
	public static final int TASK_STATUS_SUCCEEDED   = 1;
	public static final int TASK_STATUS_SKIPPED     = 2;
	public static final int TASK_STATUS_FAILED      = 3;
	public static final int TASK_STATUS_FAILED_LAST = 4;

	public static final int FINAL_STATUS_SUCCEEDED   = 1;
	public static final int FINAL_STATUS_FAILED      = 2;
	
	public void setTaskStatus(String taskName, int status);
	public void setTaskStatus(String taskName, int status, Object infoObj);

	public void setFinalStatus(int status);
	public void setFinalStatus(int status, Object infoObj);
}
