import Entites.Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.h2.tools.DeleteDbFiles;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TestDb {
    static Connection connection;
    public static void main(String[] a) throws Exception {
        connection = createAndGetConnectionToDB();
        startServer();
    }

    public static Connection createAndGetConnectionToDB() throws SQLException {
        DeleteDbFiles.execute("~", "test", true);
        Connection conn = DriverManager.
                getConnection("jdbc:h2:~/test", "sa", "");
        Statement st = conn.createStatement();
        String createTable = "CREATE TABLE client" +
                "(id INTEGER not NULL, " +
                "name VARCHAR(255), " +
                "age SMALLINT, " +
                "PRIMARY KEY (id))";
        String insert = "INSERT INTO client VALUES (1, 'Bob', 25 )";
        st.executeUpdate(createTable);
        st.executeUpdate(insert);
        st.close();
        return conn;
    }

    public static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/test", new MyHandler());
        server.createContext("/testPost", new MyPostHandler());
        server.start();
    }

    static class MyHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Statement st;
            String response = "This is the response";
            try {
                st = connection.createStatement();
                ResultSet resultSet = st.executeQuery("SELECT * FROM client");
                ObjectMapper objectMapper = new ObjectMapper();
                List<Client> clients = new ArrayList<>();
                while (resultSet.next()) {
                    Client client = new Client(resultSet.getInt("id"),
                            resultSet.getString("name"), resultSet.getInt("age"));
                    clients.add(client);
                }
                response = objectMapper.writeValueAsString(clients);
            } catch (SQLException throwable) {
                throwable.printStackTrace();
            }
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
    static class MyPostHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Statement st;
            String request = new BufferedReader(new InputStreamReader(t.getRequestBody()))
                    .lines().collect(Collectors.joining("\n"));
            System.out.println(request);
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Client client = objectMapper.readValue(request, Client.class);
                System.out.println(client);
                st = connection.createStatement();
                String insertQuery = String.format("INSERT into client VALUES (%d, '%s', %d)", client.getId(), client.getName(), client.getAge());
                System.out.println(insertQuery);
                st.executeUpdate(insertQuery);

            } catch (Exception e) {
                e.printStackTrace();
            }
            t.sendResponseHeaders(200, -1);
            t.close();
        }
    }
}
