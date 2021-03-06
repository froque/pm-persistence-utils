/**
 * Copyright (C) 2014 Premium Minds.
 *
 * This file is part of pm-persistence-utils.
 *
 * pm-persistence-utils is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * pm-persistence-utils is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with pm-persistence-utils. If not, see <http://www.gnu.org/licenses/>.
 */
package com.premiumminds.persistence.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.tool.EnversSchemaGenerator;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;


public class HibernateEnversDDL  {

	/**
	 * --create unitName [filename]
	 * --create-drop unitName [filename]
	 * --update unitName jdbcUrl jdbcUsername jdbcPassword [filename]
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length==0){
			System.out.println("Usage: ");
			System.out.println("\t--create unitName [filename] - Create table commands");
			System.out.println("\t--create-drop unitName [filename] - Create table and drop commands");
			System.out.println("\t--update unitName jdbcUrl jdbcUsername jdbcPassword [filename] - Alter table commands based on your database");
			System.out.println("\n\t[filename] is the name of the file where to write (it's optional)");
		} else {
			if("--create".equals(args[0].toLowerCase())) createCommand(args);
			if("--create-drop".equals(args[0].toLowerCase())) createDropCommand(args);
			if("--update".equals(args[0].toLowerCase())) updateCommand(args);

		}
	}

	private static void createCommand(String[] args) {
		String unitName;
		String filename=null;
		if(args.length<2) System.out.println("Expected unitName");
		else {
			unitName = args[1];
			if(args.length>2) filename = args[2];

			EnversSchemaGenerator esg = new EnversSchemaGenerator(HibernateDDL.getConfiguration(unitName));
			org.hibernate.tool.hbm2ddl.SchemaExport se = esg.export();
			se.setOutputFile(filename);
			se.setFormat(true);
			se.setDelimiter(";");
			se.execute(false, false, false, true);
		}
	}

	private static void createDropCommand(String[] args) {
		String unitName;
		String filename=null;
		if(args.length<2) System.out.println("Expected unitName");
		else {
			unitName = args[1];
			if(args.length>2) filename = args[2];

			EnversSchemaGenerator esg = new EnversSchemaGenerator(HibernateDDL.getConfiguration(unitName));
			org.hibernate.tool.hbm2ddl.SchemaExport se = esg.export();
			se.setOutputFile(filename);
			se.setFormat(true);
			se.setDelimiter(";");
			se.execute(false, false, true, true);
		}
	}

	private static void updateCommand(String[] args) {
		String unitName, filename=null, url, username, password;
		if(args.length<5) System.out.println("Expected unitName jdbcUrl jdbcUsername jdbcPassword");
		else {
			unitName = args[1];
			url = args[2];
			username = args[3];
			password = args[4];
			if(args.length>5) filename = args[5];
			
			Configuration configuration = HibernateDDL.getConfiguration(unitName);
			configuration.buildMappings();
			AuditConfiguration.getFor(configuration);
			Dialect dialect = Dialect.getDialect(configuration.getProperties());
	
			Connection conn = null;
			DatabaseMetadata meta = null;
			try {
				conn = DriverManager.getConnection(url, username, password);
				meta = new DatabaseMetadata(conn, dialect, true);
				String[] updateSQL = configuration.generateSchemaUpdateScript(dialect, meta);
				
				HibernateDDL.stringToStream(updateSQL, filename);

				configuration.buildMappings();
				AuditConfiguration.getFor(configuration);
				SchemaUpdate su = new SchemaUpdate(configuration);
				su.setOutputFile(filename);
				su.setFormat(true);
				su.setDelimiter(";");
				su.execute(true, false);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

}
