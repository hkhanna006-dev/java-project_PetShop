import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal REST server for the PetShop application.
 *
 * - Uses the JDK HttpServer (no external dependencies).
 * - Uses `DBConnection.getConnection()` to access MySQL.
 * - Exposes endpoints under /api for pets, customers and sales.
 *
 * NOTE: This is a small convenience server for local development only.
 */
public class MainServer {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/pets", new PetsHandler());
        server.createContext("/api/customers", new CustomersHandler());
        server.createContext("/api/sales", new SalesHandler());

        server.setExecutor(null);
        System.out.println("Starting PetShop REST server on http://localhost:" + port);
        server.start();
    }

    // ---------- Helpers ----------
    private static String readBody(HttpExchange ex) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(ex.getRequestBody(), "utf-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // Very small JSON parser for flat objects with string/number values (development only)
    private static Map<String, String> parseJson(String json) {
        Map<String, String> m = new HashMap<>();
        if (json == null) return m;
        String s = json.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);
        // naive parser: split by commas while respecting double quotes
        List<String> partsList = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"') {
                inQuotes = !inQuotes;
                cur.append(ch);
            } else if (ch == ',' && !inQuotes) {
                partsList.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        if (cur.length() > 0) partsList.add(cur.toString());

        for (String part : partsList) {
            String[] kv = part.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String val = kv[1].trim();
            if (key.startsWith("\"") && key.endsWith("\"")) key = key.substring(1, key.length() - 1);
            if (val.startsWith("\"") && val.endsWith("\"")) val = val.substring(1, val.length() - 1);
            m.put(key, val);
        }
        return m;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static void writeJson(HttpExchange ex, int status, String body) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        // Allow CORS from dev frontends
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        byte[] bytes = body.getBytes("utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ---------- Pets Handler ----------
    static class PetsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            // Handle CORS preflight
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
                ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
                ex.sendResponseHeaders(204, -1);
                return;
            }
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            // path: /api/pets or /api/pets/{id}
            String[] parts = path.split("/");
            try {
                if ("GET".equalsIgnoreCase(method) && parts.length == 3) {
                    handleList(ex);
                } else if ("POST".equalsIgnoreCase(method) && parts.length == 3) {
                    handleCreate(ex);
                } else if ("PUT".equalsIgnoreCase(method) && parts.length == 4) {
                    handleUpdate(ex, parts[3]);
                } else if ("DELETE".equalsIgnoreCase(method) && parts.length == 4) {
                    handleDelete(ex, parts[3]);
                } else {
                    writeJson(ex, 404, "{\"error\": \"Not found\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                writeJson(ex, 500, "{\"error\": \"" + jsonEscape(e.getMessage()) + "\"}");
            }
        }

        private void handleList(HttpExchange ex) throws SQLException, IOException {
            List<String> items = new ArrayList<>();
            try (Connection c = DBConnection.getConnection()) {
                PreparedStatement ps = c.prepareStatement("SELECT id, name, species, breed, age, price, quantity FROM pets ORDER BY created_at DESC");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String species = rs.getString("species");
                    String breed = rs.getString("breed");
                    int age = rs.getInt("age");
                    double price = rs.getDouble("price");
                    int quantity = rs.getInt("quantity");
                    String json = String.format("{\"id\":%d,\"name\":\"%s\",\"species\":\"%s\",\"breed\":\"%s\",\"age\":%d,\"price\":%.2f,\"stock\":%d}",
                            id, jsonEscape(name), jsonEscape(species), jsonEscape(breed), age, price, quantity);
                    items.add(json);
                }
            }
            String body = "[" + String.join(",", items) + "]";
            writeJson(ex, 200, body);
        }

        private void handleCreate(HttpExchange ex) throws IOException, SQLException {
            String body = readBody(ex);
            Map<String, String> m = parseJson(body);
            String name = m.getOrDefault("name", "");
            String species = m.getOrDefault("species", "");
            String breed = m.getOrDefault("breed", "");
            int age = parseInt(m.get("age"));
            double price = parseDouble(m.get("price"));
            int stock = parseInt(m.get("stock"));

            try (Connection c = DBConnection.getConnection()) {
                PreparedStatement ps = c.prepareStatement("INSERT INTO pets (name, species, breed, age, price, quantity) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setString(1, name);
                ps.setString(2, species);
                ps.setString(3, breed);
                if (age > 0) ps.setInt(4, age); else ps.setNull(4, java.sql.Types.INTEGER);
                ps.setDouble(5, price);
                ps.setInt(6, stock);
                ps.executeUpdate();
            }
            writeJson(ex, 201, "{\"status\":\"created\"}");
        }

        private void handleUpdate(HttpExchange ex, String idStr) throws IOException, SQLException {
            int id = Integer.parseInt(idStr);
            String body = readBody(ex);
            Map<String, String> m = parseJson(body);

            StringBuilder sql = new StringBuilder("UPDATE pets SET ");
            List<Object> params = new ArrayList<>();
            if (m.containsKey("name")) { sql.append("name=?,"); params.add(m.get("name")); }
            if (m.containsKey("species")) { sql.append("species=?,"); params.add(m.get("species")); }
            if (m.containsKey("breed")) { sql.append("breed=?,"); params.add(m.get("breed")); }
            if (m.containsKey("age")) { sql.append("age=?,"); params.add(parseInt(m.get("age"))); }
            if (m.containsKey("price")) { sql.append("price=?,"); params.add(parseDouble(m.get("price"))); }
            if (m.containsKey("stock")) { sql.append("quantity=?,"); params.add(parseInt(m.get("stock"))); }

            if (params.isEmpty()) {
                writeJson(ex, 400, "{\"error\": \"No updatable fields provided\"}");
                return;
            }
            // remove trailing comma
            sql.setLength(sql.length() - 1);
            sql.append(" WHERE id=?");
            try (Connection c = DBConnection.getConnection()) {
                PreparedStatement ps = c.prepareStatement(sql.toString());
                for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
                ps.setInt(params.size() + 1, id);
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    writeJson(ex, 404, "{\"error\": \"Not found\"}");
                } else {
                    writeJson(ex, 200, "{\"status\":\"updated\"}");
                }
            }
        }

        private void handleDelete(HttpExchange ex, String idStr) throws IOException, SQLException {
            int id = Integer.parseInt(idStr);
            try (Connection c = DBConnection.getConnection()) {
                PreparedStatement ps = c.prepareStatement("DELETE FROM pets WHERE id = ?");
                ps.setInt(1, id);
                int deleted = ps.executeUpdate();
                if (deleted == 0) writeJson(ex, 404, "{\"error\": \"Not found\"}");
                else writeJson(ex, 200, "{\"status\":\"deleted\"}");
            }
        }

        private int parseInt(String s) {
            try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
        }

        private double parseDouble(String s) {
            try { return Double.parseDouble(s); } catch (Exception e) { return 0.0; }
        }
    }

    // ---------- Customers Handler ----------
    static class CustomersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String[] parts = path.split("/");
            try {
                if ("GET".equalsIgnoreCase(method) && parts.length == 3) {
                    handleList(ex);
                } else if ("POST".equalsIgnoreCase(method) && parts.length == 3) {
                    handleCreate(ex);
                } else if ("PUT".equalsIgnoreCase(method) && parts.length == 4) {
                    handleUpdate(ex, parts[3]);
                } else if ("DELETE".equalsIgnoreCase(method) && parts.length == 4) {
                    handleDelete(ex, parts[3]);
                } else {
                    writeJson(ex, 404, "{\"error\": \"Not found\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                writeJson(ex, 500, "{\"error\": \"" + jsonEscape(e.getMessage()) + "\"}");
            }
        }

        private void handleList(HttpExchange ex) throws SQLException, IOException {
            List<String> items = new ArrayList<>();
            try (Connection c = DBConnection.getConnection()) {
                PreparedStatement ps = c.prepareStatement("SELECT id, name, email, phone, address FROM customers ORDER BY created_at DESC");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    String phone = rs.getString("phone");
                    String address = rs.getString("address");
                    String json = String.format("{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\",\"address\":\"%s\"}",
                            id, jsonEscape(name), jsonEscape(email), jsonEscape(phone), jsonEscape(address));
                    items.add(json);
                }
            }
            String body = "[" + String.join(",", items) + "]";
            writeJson(ex, 200, body);
        }

        private void handleCreate(HttpExchange ex) throws IOException, SQLException {
            String body = readBody(ex);
            Map<String, String> m = parseJson(body);
            String name = m.getOrDefault("name", "");
            String email = m.getOrDefault("email", null);
            String phone = m.getOrDefault("phone", "");
            String address = m.getOrDefault("address", null);

            try (Connection c = DBConnection.getConnection()) {
                PreparedStatement ps = c.prepareStatement("INSERT INTO customers (name, email, phone, address) VALUES (?, ?, ?, ?)");
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, phone);
                ps.setString(4, address);
                ps.executeUpdate();
            }
            writeJson(ex, 201, "{\"status\":\"created\"}");
        }

        private void handleUpdate(HttpExchange ex, String idStr) throws IOException, SQLException {
            int id = Integer.parseInt(idStr);
            String body = readBody(ex);
            Map<String, String> m = parseJson(body);

            StringBuilder sql = new StringBuilder("UPDATE customers SET ");
            List<Object> params = new ArrayList<>();
            if (m.containsKey("name")) { sql.append("name=?,"); params.add(m.get("name")); }
            if (m.containsKey("email")) { sql.append("email=?,"); params.add(m.get("email")); }
            if (m.containsKey("phone")) { sql.append("phone=?,"); params.add(m.get("phone")); }
            if (m.containsKey("address")) { sql.append("address=?,"); params.add(m.get("address")); }

            if (params.isEmpty()) {
                writeJson(ex, 400, "{\"error\": \"No updatable fields provided\"}");
                return;
            }
            sql.setLength(sql.length() - 1);
            sql.append(" WHERE id=?");
            try (Connection c = DBConnection.getConnection()) {
                PreparedStatement ps = c.prepareStatement(sql.toString());
                for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
                ps.setInt(params.size() + 1, id);
                int updated = ps.executeUpdate();
                if (updated == 0) writeJson(ex, 404, "{\"error\": \"Not found\"}");
                else writeJson(ex, 200, "{\"status\":\"updated\"}");
            }
        }

        private void handleDelete(HttpExchange ex, String idStr) throws IOException, SQLException {
            int id = Integer.parseInt(idStr);
            try (Connection c = DBConnection.getConnection()) {
                PreparedStatement ps = c.prepareStatement("DELETE FROM customers WHERE id = ?");
                ps.setInt(1, id);
                int deleted = ps.executeUpdate();
                if (deleted == 0) writeJson(ex, 404, "{\"error\": \"Not found\"}");
                else writeJson(ex, 200, "{\"status\":\"deleted\"}");
            }
        }
    }

    // ---------- Sales Handler ----------
    static class SalesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            String[] parts = path.split("/");
            try {
                if ("GET".equalsIgnoreCase(method) && parts.length == 3) {
                    handleList(ex);
                } else if ("POST".equalsIgnoreCase(method) && parts.length == 3) {
                    handleCreate(ex);
                } else {
                    writeJson(ex, 404, "{\"error\": \"Not found\"}");
                }
            } catch (Exception e) {
                e.printStackTrace();
                writeJson(ex, 500, "{\"error\": \"" + jsonEscape(e.getMessage()) + "\"}");
            }
        }

        private void handleList(HttpExchange ex) throws SQLException, IOException {
            List<String> items = new ArrayList<>();
            try (Connection c = DBConnection.getConnection()) {
                PreparedStatement ps = c.prepareStatement(
                        "SELECT s.id, s.quantity, s.total_price, s.sale_date, p.name AS pet_name, p.species AS pet_species, p.breed AS pet_breed, c.name AS customer_name, c.phone AS customer_phone " +
                                "FROM sales s JOIN pets p ON s.pet_id = p.id JOIN customers c ON s.customer_id = c.id ORDER BY s.sale_date DESC");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int qty = rs.getInt("quantity");
                    double total = rs.getDouble("total_price");
                    String saleDate = rs.getString("sale_date");
                    String petName = rs.getString("pet_name");
                    String petSpecies = rs.getString("pet_species");
                    String petBreed = rs.getString("pet_breed");
                    String custName = rs.getString("customer_name");
                    String custPhone = rs.getString("customer_phone");

                    String json = String.format("{\"id\":%d,\"quantity\":%d,\"total_price\":%.2f,\"sale_date\":\"%s\",\"pet\":{\"name\":\"%s\",\"species\":\"%s\",\"breed\":\"%s\"},\"customer\":{\"name\":\"%s\",\"phone\":\"%s\"}}",
                            id, qty, total, jsonEscape(saleDate), jsonEscape(petName), jsonEscape(petSpecies), jsonEscape(petBreed), jsonEscape(custName), jsonEscape(custPhone));
                    items.add(json);
                }
            }
            String body = "[" + String.join(",", items) + "]";
            writeJson(ex, 200, body);
        }

        private void handleCreate(HttpExchange ex) throws IOException, SQLException {
            String body = readBody(ex);
            Map<String, String> m = parseJson(body);
            int petId = Integer.parseInt(m.getOrDefault("pet_id", "0"));
            int customerId = Integer.parseInt(m.getOrDefault("customer_id", "0"));
            int qty = Integer.parseInt(m.getOrDefault("quantity", "0"));
            double total = Double.parseDouble(m.getOrDefault("total_price", "0"));

            try (Connection c = DBConnection.getConnection()) {
                try {
                    c.setAutoCommit(false);
                    // check stock
                    PreparedStatement ps1 = c.prepareStatement("SELECT quantity FROM pets WHERE id = ? FOR UPDATE");
                    ps1.setInt(1, petId);
                    ResultSet rs = ps1.executeQuery();
                    if (!rs.next()) throw new SQLException("Pet not found");
                    int stock = rs.getInt("quantity");
                    if (stock < qty) throw new SQLException("Not enough stock");

                    // update stock
                    PreparedStatement ps2 = c.prepareStatement("UPDATE pets SET quantity = ? WHERE id = ?");
                    ps2.setInt(1, stock - qty);
                    ps2.setInt(2, petId);
                    ps2.executeUpdate();

                    // insert sale
                    PreparedStatement ps3 = c.prepareStatement("INSERT INTO sales (pet_id, customer_id, quantity, total_price) VALUES (?, ?, ?, ?)");
                    ps3.setInt(1, petId);
                    ps3.setInt(2, customerId);
                    ps3.setInt(3, qty);
                    ps3.setDouble(4, total);
                    ps3.executeUpdate();

                    c.commit();
                    writeJson(ex, 201, "{\"status\":\"created\"}");
                } catch (Exception err) {
                    c.rollback();
                    throw err;
                } finally {
                    c.setAutoCommit(true);
                }
            }
        }
    }
}
