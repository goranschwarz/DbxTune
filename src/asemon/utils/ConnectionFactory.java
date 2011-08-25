/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.utils;

import java.sql.Connection;

public interface ConnectionFactory
{
	public Connection getConnection(String appname);
}
