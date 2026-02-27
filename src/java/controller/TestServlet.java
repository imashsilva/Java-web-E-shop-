package controller;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;
import java.io.*;
import java.sql.*;

@WebServlet("/test-db")
public class TestServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        out.println("<h1>Database Connection Test</h1>");
        
        // Test direct MySQL connection
        Connection conn = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/smart_tech?useSSL=false&serverTimezone=UTC";
            String user = "root";
            String password = "Imash@331"; // replace with your password
            
            conn = DriverManager.getConnection(url, user, password);
            out.println("<p style='color: green;'>✅ MySQL Direct Connection SUCCESSFUL!</p>");
            
        } catch (Exception e) {
            out.println("<p style='color: red;'>❌ MySQL Direct Connection FAILED: " + e.getMessage() + "</p>");
            e.printStackTrace(out);
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
        
        out.println("<hr>");
        
        // Test Hibernate connection
        try {
            hibernate.HibernateUtil.getSessionFactory();
            out.println("<p style='color: green;'>✅ Hibernate Connection SUCCESSFUL!</p>");
            System.out.println("Done");
        } catch (Exception e) {
            out.println("<p style='color: red;'>❌ Hibernate Connection FAILED: " + e.getMessage() + "</p>");
            e.printStackTrace(out);
        }
    }
}