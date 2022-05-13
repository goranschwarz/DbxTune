/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.test;

//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.Collections;
//import java.util.Properties;
//import java.util.concurrent.CompletableFuture;

//import com.microsoft.aad.msal4j.ClientCredentialFactory;
//import com.microsoft.aad.msal4j.ClientCredentialParameters;
//import com.microsoft.aad.msal4j.ConfidentialClientApplication;
//import com.microsoft.aad.msal4j.IAuthenticationResult;
//import com.nimbusds.oauth2.sdk.http.HTTPResponse;

public class TeamsMessage
{

	public static void main(String[] args)
	{
		System.out.println("DUMMY");
	}

//	public static void main(String[] args)
//	{
//		IAuthenticationProvider authProvider = null;
//	
//		// GraphServiceClient<nativeRequestType> graphClient = GraphServiceClient.builder().authenticationProvider( authProvider).buildClient();
//		GraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider( authProvider ).buildClient();
//	
//		ChatMessage chatMessage = new ChatMessage();
//		ItemBody body = new ItemBody();
//		body.content = "Test message from Java Code\n/Goran";
//		chatMessage.body = body;
//	
//		// graphClient.teams("fbe2bf47-16c8-47cf-b4a5-4b9b187c508b").channels("19:4a95f7d8db4c4e7fae857bcebe0623e6@thread.tacv2").messages()
//		graphClient.teams("fbe2bf47-16c8-47cf-b4a5-4b9b187c508b").channels("19:4a95f7d8db4c4e7fae857bcebe0623e6@thread.tacv2").messages()
//			.buildRequest()
//			.post(chatMessage);
//	}
//
//	public static class SimpleAuthProvider implements IAuthenticationProvider
//	{
//		private String accessToken = null;
//	
//		public SimpleAuthProvider(String accessToken)
//		{
//			this.accessToken = accessToken;
//		}
//	
//		@Override
//		public void authenticateRequest(IHttpRequest request)
//		{
//			// Add the access token in the Authorization header
//			request.addHeader("Authorization", "Bearer " + accessToken);
//		}
//	
//		@Override
//		public CompletableFuture<String> getAuthorizationTokenAsync(URL arg0)
//		{
//			// TODO Auto-generated method stub
//			return null;
//		}
//	}

}

//	private static String                        authority;
//	private static String                        clientId;
//	private static String                        secret;
//	private static String                        scope;
//	private static ConfidentialClientApplication app;
//
//	public static void main(String args[]) throws Exception
//	{
//
//		setUpSampleData();
//
//		try
//		{
//			BuildConfidentialClientObject();
//			IAuthenticationResult result             = getAccessTokenByClientCredentialGrant();
//			String                usersListFromGraph = getUsersListFromGraph(result.accessToken());
//
//			System.out.println("Users in the Tenant = " + usersListFromGraph);
//			System.out.println("Press any key to exit ...");
//			System.in.read();
//
//		}
//		catch (Exception ex)
//		{
//			System.out.println("Oops! We have an exception of type - " + ex.getClass());
//			System.out.println("Exception message - " + ex.getMessage());
//			throw ex;
//		}
//	}
//
//	private static void BuildConfidentialClientObject() throws Exception
//	{
//
//		// Load properties file and set properties used throughout the sample
//		app = ConfidentialClientApplication.builder(clientId, ClientCredentialFactory.createFromSecret(secret)).authority(authority).build();
//	}
//
//	private static IAuthenticationResult getAccessTokenByClientCredentialGrant() throws Exception
//	{
//
//		// With client credentials flows the scope is ALWAYS of the shape
//		// "resource/.default", as the
//		// application permissions need to be set statically (in the portal),
//		// and then granted by a tenant administrator
//		ClientCredentialParameters               clientCredentialParam = ClientCredentialParameters.builder(Collections.singleton(scope)).build();
//
//		CompletableFuture<IAuthenticationResult> future                = app.acquireToken(clientCredentialParam);
//		return future.get();
//	}
//
//	private static String getUsersListFromGraph(String accessToken) throws IOException
//	{
//		URL               url  = new URL("https://graph.microsoft.com/v1.0/users");
//		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//
//		conn.setRequestMethod("GET");
//		conn.setRequestProperty("Authorization", "Bearer " + accessToken);
//		conn.setRequestProperty("Accept", "application/json");
//
//		int httpResponseCode = conn.getResponseCode();
//		if(httpResponseCode == HTTPResponse.SC_OK)
////		if ( httpResponseCode == 200 )
//		{
//
//			StringBuilder response;
//			try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream())))
//			{
//
//				String inputLine;
//				response = new StringBuilder();
//				while ((inputLine = in.readLine()) != null)
//				{
//					response.append(inputLine);
//				}
//			}
//			return response.toString();
//		}
//		else
//		{
//			return String.format("Connection returned HTTP code: %s with message: %s", httpResponseCode, conn.getResponseMessage());
//		}
//	}
//
//	/**
//	 * Helper function unique to this sample setting. In a real application
//	 * these wouldn't be so hardcoded, for example different users may need
//	 * different authority endpoints or scopes
//	 */
//	private static void setUpSampleData() throws IOException
//	{
//		// // Load properties file and set properties used throughout the sample
//		// Properties properties = new Properties();
//		// properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("application.properties"));
//		// authority = properties.getProperty("AUTHORITY");
//		// clientId = properties.getProperty("CLIENT_ID");
//		// secret = properties.getProperty("SECRET");
//		// scope = properties.getProperty("SCOPE");
//		authority = "https://login.microsoftonline.com/b238764b-05b9-48a7-a2bd-55a357dee4ae/";
//		clientId  = "08448733-bf63-40d9-8762-42f8c3364fc4";
////		secret    = "edc8f820-52a5-4950-9e77-2e51715b130c";
//		secret    = "ruK7Q~kSzsmJ0qCnWnWp9F7WZigrmU4.Pn0PX";
//		scope     = "https://graph.microsoft.com/.default";
//	}
//}
