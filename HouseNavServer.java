// Save as HouseNavServer.java
import com.sun.net.httpserver.*;
import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.*;

public class HouseNavServer {

    // JDBC settings - change to match your SQL Server instance
    private static final String JDBC_URL = "jdbc:sqlserver://localhost:1433;databaseName=HouseNav;encrypt=false;";
    private static final String JDBC_USER = "your_db_user";
    private static final String JDBC_PASS = "your_db_password";

    // In-memory graph loaded from DB
    static class Node { int id; int x, y; String label; Node(int id,int x,int y,String l){this.id=id;this.x=x;this.y=y;this.label=l;} }
    static Map<Integer, Node> nodes = new HashMap<>();
    static Map<Integer, List<Integer>> adj = new HashMap<>(); // adjacency
    static Map<String, Integer> roomToNode = new HashMap<>(); // room_name -> node_id

    public static void main(String[] args) throws Exception {
        // 1) Load graph from DB
        loadGraphFromDB();

        // 2) Start HTTP server
        HttpServer http = HttpServer.create(new InetSocketAddress(8000), 0);
        http.createContext("/route", new RouteHandler());
        http.setExecutor(null);
        System.out.println("Server started at http://localhost:8000");
        http.start();
    }

    static void loadGraphFromDB() throws Exception {
        // Load JDBC driver
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)) {
            // Load nodes
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id,x,y,label FROM NODES")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    String label = rs.getString("label");
                    Node n = new Node(id, x, y, label);
                    nodes.put(id, n);
                    adj.put(id, new ArrayList<>());
                }
            }

            // Load edges (treat as undirected)
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT from_node,to_node FROM EDGES")) {
                while (rs.next()) {
                    int a = rs.getInt("from_node");
                    int b = rs.getInt("to_node");
                    if (!adj.containsKey(a)) adj.put(a, new ArrayList<>());
                    if (!adj.containsKey(b)) adj.put(b, new ArrayList<>());
                    adj.get(a).add(b);
                    adj.get(b).add(a);
                }
            }

            // Load rooms
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT name,node_id FROM ROOMS")) {
                while (rs.next()) {
                    roomToNode.put(rs.getString("name"), rs.getInt("node_id"));
                }
            }
        }
        System.out.println("Loaded nodes: " + nodes.size() + ", rooms: " + roomToNode.size());
    }

    // Handler for /route?start=roomName&end=roomName
    static class RouteHandler implements HttpHandler {
        @Override public void handle(HttpExchange exch) throws IOException {
            try {
                Map<String,String> q = queryToMap(exch.getRequestURI().getRawQuery());
                String startRoom = q.getOrDefault("start", "");
                String endRoom = q.getOrDefault("end", "");

                if (startRoom.isEmpty() || endRoom.isEmpty()) {
                    sendJson(exch, 400, "{\"error\":\"missing start or end parameter\"}");
                    return;
                }

                Integer startNode = roomToNode.get(startRoom);
                Integer endNode = roomToNode.get(endRoom);
                if (startNode == null || endNode == null) {
                    sendJson(exch, 400, "{\"error\":\"unknown room name\"}");
                    return;
                }

                // Compute shortest path (BFS)
                List<Integer> pathNodeIds = bfsShortestPath(startNode, endNode);
                if (pathNodeIds == null || pathNodeIds.isEmpty()) {
                    sendJson(exch, 500, "{\"error\":\"no path found\"}");
                    return;
                }

                // Build coordinates list and instructions
                StringBuilder coordsJson = new StringBuilder();
                coordsJson.append("[");
                for (int i=0;i<pathNodeIds.size();i++){
                    Node n = nodes.get(pathNodeIds.get(i));
                    coordsJson.append("{\"x\":").append(n.x).append(",\"y\":").append(n.y).append("}");
                    if (i<pathNodeIds.size()-1) coordsJson.append(",");
                }
                coordsJson.append("]");

                // Build simple instructions (naive: step-by-step between nodes)
                List<String> instr = buildInstructions(pathNodeIds, startRoom, endRoom);

                StringBuilder instrJson = new StringBuilder();
                instrJson.append("[");
                for (int i=0;i<instr.size();i++){
                    instrJson.append("\"").append(escapeJson(instr.get(i))).append("\"");
                    if (i<instr.size()-1) instrJson.append(",");
                }
                instrJson.append("]");

                // Combine into final JSON
                String json = "{"
                    + "\"path\":" + coordsJson.toString() + ","
                    + "\"instructions\":" + instrJson.toString()
                    + "}";

                sendJson(exch, 200, json);

            } catch (Exception ex) {
                ex.printStackTrace();
                sendJson(exch, 500, "{\"error\":\"internal server error\"}");
            }
        }

        void sendJson(HttpExchange exch, int code, String body) throws IOException {
            byte[] bytes = body.getBytes("UTF-8");
            exch.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exch.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exch.getResponseBody()) {
                os.write(bytes);
            }
        }

        Map<String,String> queryToMap(String query){
            Map<String,String> map = new HashMap<>();
            if (query == null) return map;
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                try {
                    String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String val = URLDecoder.decode(pair.substring(idx+1), "UTF-8");
                    map.put(key, val);
                } catch (Exception e) { /* ignore malformed */ }
            }
            return map;
        }

        // BFS shortest path returning list of node ids (inclusive)
        List<Integer> bfsShortestPath(int start, int goal) {
            Queue<Integer> q = new LinkedList<>();
            Map<Integer,Integer> parent = new HashMap<>();
            q.add(start);
            parent.put(start, null);

            while(!q.isEmpty()){
                int cur = q.poll();
                if (cur == goal) break;
                List<Integer> neighbors = adj.getOrDefault(cur, Collections.emptyList());
                for (int nb : neighbors) {
                    if (!parent.containsKey(nb)) {
                        parent.put(nb, cur);
                        q.add(nb);
                    }
                }
            }

            if (!parent.containsKey(goal)) return null;
            LinkedList<Integer> path = new LinkedList<>();
            Integer cur = goal;
            while(cur != null){
                path.addFirst(cur);
                cur = parent.get(cur);
            }
            return path;
        }

        // Simple instruction builder (very basic natural language)
        List<String> buildInstructions(List<Integer> pathNodes, String startRoom, String endRoom) {
            List<String> out = new ArrayList<>();
            out.add("Start at " + startRoom + ".");
            for (int i=1;i<pathNodes.size();i++){
                Node from = nodes.get(pathNodes.get(i-1));
                Node to = nodes.get(pathNodes.get(i));
                // compute simple delta
                int dx = to.x - from.x;
                int dy = to.y - from.y;
                String dir = directionFromDelta(dx, dy);
                out.add("Walk " + dir + " to the next point.");
            }
            out.add("You have arrived at " + endRoom + ".");
            return out;
        }

        String directionFromDelta(int dx, int dy){
            // very naive direction
            if (Math.abs(dx) > Math.abs(dy)) {
                return dx > 0 ? "right" : "left";
            } else {
                return dy > 0 ? "down" : "up";
            }
        }

        String escapeJson(String s){
            return s.replace("\\","\\\\").replace("\"","\\\"");
        }
    }
}
