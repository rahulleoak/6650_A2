package org.ClientSide;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class RDSManager {

  public static Connection connectToDB(String dbURL, String username, String password){
    Connection connection = null;
    try {
      // Register the JDBC driver
      Class.forName("com.mysql.cj.jdbc.Driver");

      // Establish the database connection
      connection = DriverManager.getConnection(dbURL, username, password);
    } catch (ClassNotFoundException | SQLException e) {
      e.printStackTrace();
    }
    return connection;
  }

}
