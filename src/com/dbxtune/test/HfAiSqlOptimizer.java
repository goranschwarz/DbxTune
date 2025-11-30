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
package com.dbxtune.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HfAiSqlOptimizer
{
	private static final String HF_API_TOKEN = "<HF_API_TOKEN>";

//	private static final String HF_API_URL   = "https://api-inference.huggingface.co/models/codellama/CodeLlama-13b-Instruct-hf";
//	private static final String HF_API_URL = "https://api-inference.huggingface.co/models/codellama/CodeLlama-7b-Instruct-hf";
//	private static final String HF_API_URL = "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2";
	private static final String HF_API_URL = "https://api-inference.huggingface.co/models/google/flan-t5-base";


	public static String optimizeSql(String sql, String schema, String indexes, String triggers, String dbVendor) throws IOException
	{
		String prompt = buildPrompt(sql, schema, indexes, triggers, dbVendor);

		URL url    = new URL(HF_API_URL);
		HttpURLConnection conn   = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Authorization", "Bearer " + HF_API_TOKEN);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);

//		String body = "{ \"inputs\": \"" + prompt.replace("\"", "\\\"") + "\" }";
	    String body = "{ \"inputs\": \"" + prompt.replace("\"", "\\\"") + "\", \"options\": {\"wait_for_model\": true} }";

		try (OutputStream os = conn.getOutputStream())
		{
			byte[] input = body.getBytes(StandardCharsets.UTF_8);
			os.write(input, 0, input.length);
		}

		StringBuilder response;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)))
		{
			response = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null)
			{
				response.append(line.trim());
			}
		}

		return response.toString();
	}

//	private static String buildPrompt(String sql, String schema, String indexes, String triggers, String dbVendor)
//	{
//		return ""
//				+ "You are an expert database performance tuner for " + dbVendor + " databases.\n" 
//				+ "Given the following inputs:\n" 
//				+ "SQL query:\n" + sql + "\n\n" + "Table/View definitions:\n" + schema + "\n\n" 
//				+ "Indexes:\n" + indexes + "\n\n" 
//				+ "Triggers:\n" + triggers + "\n\n" 
//				+ "Task: Optimize the SQL query for best performance. Output JSON in the format:\n" + "{ \"optimized_sql\": \"...\", \"explanation\": \"...\" }\n" 
//				+ "Do not include anything outside of JSON.";
//	}
	private static String buildPrompt(String sql, String schema, String indexes, String triggers, String dbVendor) 
	{
	    return "Optimize the following SQL query for " + dbVendor + ".\n\n"
	            + "SQL:\n" + sql + "\n\n"
	            + "Schema:\n" + schema + "\n\n"
	            + "Indexes:\n" + indexes + "\n\n"
	            + "Triggers:\n" + triggers + "\n\n"
	            + "Return JSON with fields {optimized_sql, explanation}.";
	}

//	public static void main(String[] args) throws IOException
//	{
//		String sql      = "SELECT * FROM orders o JOIN customers c ON o.customer_id = c.id WHERE c.country = 'USA'";
//		String schema   = "Table orders(id INT, customer_id INT, total DECIMAL, created_at DATE);\n" + "Table customers(id INT, name VARCHAR, country VARCHAR);";
//		String indexes  = "Index on orders(customer_id), Index on customers(country)";
//		String triggers = "None";
//		String dbVendor = "PostgreSQL";
//
//		String result   = optimizeSql(sql, schema, indexes, triggers, dbVendor);
//		System.out.println("LLM Response: " + result);
//	}
//	public static void main(String[] args) throws IOException {
//	    URL url = new URL("https://api-inference.huggingface.co/models/google/flan-t5-base");
//	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//	    conn.setRequestMethod("POST");
//	    conn.setRequestProperty("Authorization", "Bearer " + HF_API_TOKEN);
//	    conn.setRequestProperty("Content-Type", "application/json");
//	    conn.setDoOutput(true);
//
//	    String prompt = "Optimize this SQL query:\nSELECT * FROM users WHERE age > 30;\n\nReturn JSON with {optimized_sql, explanation}.";
//	    String body = "{ \"inputs\": \"" + prompt.replace("\"", "\\\"") + "\" }";
//
//	    try (OutputStream os = conn.getOutputStream()) {
//	        os.write(body.getBytes(StandardCharsets.UTF_8));
//	    }
//
//	    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
//	    StringBuilder response = new StringBuilder();
//	    String line;
//	    while ((line = br.readLine()) != null) {
//	        response.append(line);
//	    }
//
//	    System.out.println("Response: " + response);
//	}
//    public static void main(String[] args) throws IOException {
//        String apiUrl = "https://api-inference.huggingface.co/pipeline/text2text-generation/google/flan-t5-base";
//
//        URL url = new URL(apiUrl);
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setRequestMethod("POST");
//        conn.setRequestProperty("Authorization", "Bearer " + HF_API_TOKEN);
//        conn.setRequestProperty("Content-Type", "application/json");
//        conn.setDoOutput(true);
//
//        String prompt = "Optimize this SQL query:\nSELECT * FROM users WHERE age > 30;\n\nReturn JSON with {optimized_sql, explanation}.";
//        String body = "{ \"inputs\": \"" + prompt.replace("\"", "\\\"") + "\" }";
//
//        try (OutputStream os = conn.getOutputStream()) {
//            os.write(body.getBytes(StandardCharsets.UTF_8));
//        }
//
//        StringBuilder response;
//        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
//            response = new StringBuilder();
//            String line;
//            while ((line = br.readLine()) != null) {
//                response.append(line);
//            }
//        }
//
//        System.out.println("Response: " + response);
//    }
    public static void main(String[] args) throws IOException {
        String apiUrl = "https://api-inference.huggingface.co/models/google/flan-t5-small";

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + HF_API_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String prompt = "Translate to French: 'Hello, how are you?'";
        String body = "{ \"inputs\": \"" + prompt.replace("\"", "\\\"") + "\" }";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        StringBuilder response;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        System.out.println("Response: " + response);
    }
}
