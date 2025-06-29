Let me create a complete implementation starting with the Java backend, then the Android client.
// =============================================================================
// 1. INTERFACES
// =============================================================================

// Compute.java - RMI Remote Interface
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Compute extends Remote {
    <T> T executeTask(Task<T> t) throws RemoteException;
}

// Task.java - Task Interface
import java.io.Serializable;

public interface Task<T> extends Serializable {
    T execute();
}

// =============================================================================
// 2. DATA MODELS
// =============================================================================

// FruitPrice.java - Data Model
import java.io.Serializable;

public class FruitPrice implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String fruitName;
    private double pricePerKg;
    
    public FruitPrice() {}
    
    public FruitPrice(String fruitName, double pricePerKg) {
        this.fruitName = fruitName;
        this.pricePerKg = pricePerKg;
    }
    
    // Getters and Setters
    public String getFruitName() { return fruitName; }
    public void setFruitName(String fruitName) { this.fruitName = fruitName; }
    public double getPricePerKg() { return pricePerKg; }
    public void setPricePerKg(double pricePerKg) { this.pricePerKg = pricePerKg; }
    
    @Override
    public String toString() {
        return "FruitPrice{fruitName='" + fruitName + "', pricePerKg=" + pricePerKg + "}";
    }
}

// Receipt.java - Receipt Model
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class Receipt implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<String> items;
    private double totalCost;
    private double amountPaid;
    private double change;
    private String cashier;
    private LocalDateTime timestamp;
    
    public Receipt(List<String> items, double totalCost, double amountPaid, String cashier) {
        this.items = items;
        this.totalCost = totalCost;
        this.amountPaid = amountPaid;
        this.change = amountPaid - totalCost;
        this.cashier = cashier;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters
    public List<String> getItems() { return items; }
    public double getTotalCost() { return totalCost; }
    public double getAmountPaid() { return amountPaid; }
    public double getChange() { return change; }
    public String getCashier() { return cashier; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== RECEIPT ==========\n");
        sb.append("Date: ").append(timestamp).append("\n");
        sb.append("Cashier: ").append(cashier).append("\n");
        sb.append("Items:\n");
        for (String item : items) {
            sb.append("  ").append(item).append("\n");
        }
        sb.append("Total Cost: $").append(String.format("%.2f", totalCost)).append("\n");
        sb.append("Amount Paid: $").append(String.format("%.2f", amountPaid)).append("\n");
        sb.append("Change: $").append(String.format("%.2f", change)).append("\n");
        sb.append("============================");
        return sb.toString();
    }
}

// =============================================================================
// 3. RMI SERVER IMPLEMENTATION
// =============================================================================

// FruitComputeEngine.java - RMI Server
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class FruitComputeEngine extends UnicastRemoteObject implements Compute {
    private static final long serialVersionUID = 1L;
    
    // In-memory fruit price database
    private static final Map<String, Double> fruitPriceTable = new ConcurrentHashMap<>();
    
    // Initialize with some default fruits
    static {
        fruitPriceTable.put("apple", 2.50);
        fruitPriceTable.put("banana", 1.20);
        fruitPriceTable.put("orange", 3.00);
        fruitPriceTable.put("mango", 4.50);
    }
    
    public FruitComputeEngine() throws RemoteException {
        super();
    }
    
    @Override
    public <T> T executeTask(Task<T> task) throws RemoteException {
        System.out.println("Executing task: " + task.getClass().getSimpleName());
        return task.execute();
    }
    
    // Static methods for database operations
    public static synchronized boolean addFruit(String name, double price) {
        if (fruitPriceTable.containsKey(name.toLowerCase())) {
            return false; // Fruit already exists
        }
        fruitPriceTable.put(name.toLowerCase(), price);
        return true;
    }
    
    public static synchronized boolean updateFruit(String name, double price) {
        if (!fruitPriceTable.containsKey(name.toLowerCase())) {
            return false; // Fruit doesn't exist
        }
        fruitPriceTable.put(name.toLowerCase(), price);
        return true;
    }
    
    public static synchronized boolean deleteFruit(String name) {
        return fruitPriceTable.remove(name.toLowerCase()) != null;
    }
    
    public static synchronized Double getFruitPrice(String name) {
        return fruitPriceTable.get(name.toLowerCase());
    }
    
    public static synchronized Map<String, Double> getAllFruits() {
        return new ConcurrentHashMap<>(fruitPriceTable);
    }
    
    // Main method to start RMI server
    public static void main(String[] args) {
        try {
            // Create RMI registry on port 1099
            Registry registry = LocateRegistry.createRegistry(1099);
            
            // Create compute engine instance
            FruitComputeEngine engine = new FruitComputeEngine();
            
            // Bind the remote object in the registry
            registry.rebind("FruitComputeEngine", engine);
            
            System.out.println("FruitComputeEngine server ready on port 1099");
            System.out.println("Fruit database initialized with: " + fruitPriceTable);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

// =============================================================================
// 4. TASK IMPLEMENTATIONS
// =============================================================================

// AddFruitPrice.java
public class AddFruitPrice implements Task<Boolean> {
    private static final long serialVersionUID = 1L;
    
    private String fruitName;
    private double pricePerKg;
    
    public AddFruitPrice(String fruitName, double pricePerKg) {
        this.fruitName = fruitName;
        this.pricePerKg = pricePerKg;
    }
    
    @Override
    public Boolean execute() {
        System.out.println("Adding fruit: " + fruitName + " at $" + pricePerKg + "/kg");
        return FruitComputeEngine.addFruit(fruitName, pricePerKg);
    }
}

// UpdateFruitPrice.java
public class UpdateFruitPrice implements Task<Boolean> {
    private static final long serialVersionUID = 1L;
    
    private String fruitName;
    private double pricePerKg;
    
    public UpdateFruitPrice(String fruitName, double pricePerKg) {
        this.fruitName = fruitName;
        this.pricePerKg = pricePerKg;
    }
    
    @Override
    public Boolean execute() {
        System.out.println("Updating fruit: " + fruitName + " to $" + pricePerKg + "/kg");
        return FruitComputeEngine.updateFruit(fruitName, pricePerKg);
    }
}

// DeleteFruitPrice.java
public class DeleteFruitPrice implements Task<Boolean> {
    private static final long serialVersionUID = 1L;
    
    private String fruitName;
    
    public DeleteFruitPrice(String fruitName) {
        this.fruitName = fruitName;
    }
    
    @Override
    public Boolean execute() {
        System.out.println("Deleting fruit: " + fruitName);
        return FruitComputeEngine.deleteFruit(fruitName);
    }
}

// CalFruitCost.java
public class CalFruitCost implements Task<Double> {
    private static final long serialVersionUID = 1L;
    
    private String fruitName;
    private double quantity;
    
    public CalFruitCost(String fruitName, double quantity) {
        this.fruitName = fruitName;
        this.quantity = quantity;
    }
    
    @Override
    public Double execute() {
        System.out.println("Calculating cost for " + quantity + "kg of " + fruitName);
        Double price = FruitComputeEngine.getFruitPrice(fruitName);
        if (price == null) {
            return -1.0; // Fruit not found
        }
        return price * quantity;
    }
}

// CalculateCost.java - Generate Receipt
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CalculateCost implements Task<Receipt> {
    private static final long serialVersionUID = 1L;
    
    private Map<String, Double> fruitQuantities; // fruit name -> quantity
    private double amountPaid;
    private String cashier;
    
    public CalculateCost(Map<String, Double> fruitQuantities, double amountPaid, String cashier) {
        this.fruitQuantities = fruitQuantities;
        this.amountPaid = amountPaid;
        this.cashier = cashier;
    }
    
    @Override
    public Receipt execute() {
        System.out.println("Generating receipt for cashier: " + cashier);
        
        List<String> items = new ArrayList<>();
        double totalCost = 0.0;
        
        for (Map.Entry<String, Double> entry : fruitQuantities.entrySet()) {
            String fruitName = entry.getKey();
            double quantity = entry.getValue();
            
            Double price = FruitComputeEngine.getFruitPrice(fruitName);
            if (price != null) {
                double cost = price * quantity;
                totalCost += cost;
                items.add(String.format("%s: %.2fkg @ $%.2f/kg = $%.2f", 
                    fruitName, quantity, price, cost));
            }
        }
        
        return new Receipt(items, totalCost, amountPaid, cashier);
    }
}

// =============================================================================
// 5. RMI CLIENT REGISTRY
// =============================================================================

// FruitComputeTaskRegistry.java - RMI Client
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class FruitComputeTaskRegistry {
    private Compute compute;
    
    public FruitComputeTaskRegistry() throws Exception {
        // Look up the remote object
        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        compute = (Compute) registry.lookup("FruitComputeEngine");
        System.out.println("Connected to FruitComputeEngine");
    }
    
    public <T> T executeTask(Task<T> task) throws Exception {
        return compute.executeTask(task);
    }
    
    // Test client
    public static void main(String[] args) {
        try {
            FruitComputeTaskRegistry client = new FruitComputeTaskRegistry();
            
            // Test adding a fruit
            Boolean added = client.executeTask(new AddFruitPrice("grape", 5.00));
            System.out.println("Grape added: " + added);
            
            // Test calculating cost
            Double cost = client.executeTask(new CalFruitCost("apple", 2.5));
            System.out.println("Cost of 2.5kg apples: $" + cost);
            
            // Test updating price
            Boolean updated = client.executeTask(new UpdateFruitPrice("banana", 1.50));
            System.out.println("Banana price updated: " + updated);
            
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

Now let me create the servlet layer that will handle HTTP requests from the Android client:
// =============================================================================
// SERVLET IMPLEMENTATIONS
// =============================================================================

// BaseFruitServlet.java - Base servlet with common functionality
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import com.google.gson.Gson;

public abstract class BaseFruitServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected FruitComputeTaskRegistry taskRegistry;
    protected Gson gson;
    
    @Override
    public void init() throws ServletException {
        super.init();
        try {
            taskRegistry = new FruitComputeTaskRegistry();
            gson = new Gson();
            System.out.println("Servlet initialized with RMI connection");
        } catch (Exception e) {
            throw new ServletException("Failed to connect to RMI server", e);
        }
    }
    
    protected void sendJsonResponse(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        
        PrintWriter out = response.getWriter();
        out.print(gson.toJson(data));
        out.flush();
    }
    
    protected void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        ErrorResponse error = new ErrorResponse(message);
        sendJsonResponse(response, error);
    }
    
    // Error response class
    public static class ErrorResponse {
        private String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
        
        public String getError() { return error; }
    }
}

// AddFruitServlet.java
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;

@WebServlet("/api/fruits/add")
public class AddFruitServlet extends BaseFruitServlet {
    private static final long serialVersionUID = 1L;
    
    public static class AddFruitRequest {
        private String fruitName;
        private double pricePerKg;
        
        // Getters and setters
        public String getFruitName() { return fruitName; }
        public void setFruitName(String fruitName) { this.fruitName = fruitName; }
        public double getPricePerKg() { return pricePerKg; }
        public void setPricePerKg(double pricePerKg) { this.pricePerKg = pricePerKg; }
    }
    
    public static class AddFruitResponse {
        private boolean success;
        private String message;
        
        public AddFruitResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            // Read JSON from request body
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            AddFruitRequest req = gson.fromJson(sb.toString(), AddFruitRequest.class);
            
            if (req.getFruitName() == null || req.getFruitName().trim().isEmpty()) {
                sendErrorResponse(response, "Fruit name is required", 400);
                return;
            }
            
            if (req.getPricePerKg() <= 0) {
                sendErrorResponse(response, "Price must be greater than 0", 400);
                return;
            }
            
            // Execute RMI task
            Boolean result = taskRegistry.executeTask(new AddFruitPrice(req.getFruitName(), req.getPricePerKg()));
            
            if (result) {
                sendJsonResponse(response, new AddFruitResponse(true, "Fruit added successfully"));
            } else {
                sendJsonResponse(response, new AddFruitResponse(false, "Fruit already exists"));
            }
            
        } catch (Exception e) {
            System.err.println("Error in AddFruitServlet: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(200);
    }
}

// UpdateFruitServlet.java
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;

@WebServlet("/api/fruits/update")
public class UpdateFruitServlet extends BaseFruitServlet {
    private static final long serialVersionUID = 1L;
    
    public static class UpdateFruitRequest {
        private String fruitName;
        private double pricePerKg;
        
        public String getFruitName() { return fruitName; }
        public void setFruitName(String fruitName) { this.fruitName = fruitName; }
        public double getPricePerKg() { return pricePerKg; }
        public void setPricePerKg(double pricePerKg) { this.pricePerKg = pricePerKg; }
    }
    
    public static class UpdateFruitResponse {
        private boolean success;
        private String message;
        
        public UpdateFruitResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            UpdateFruitRequest req = gson.fromJson(sb.toString(), UpdateFruitRequest.class);
            
            if (req.getFruitName() == null || req.getFruitName().trim().isEmpty()) {
                sendErrorResponse(response, "Fruit name is required", 400);
                return;
            }
            
            if (req.getPricePerKg() <= 0) {
                sendErrorResponse(response, "Price must be greater than 0", 400);
                return;
            }
            
            Boolean result = taskRegistry.executeTask(new UpdateFruitPrice(req.getFruitName(), req.getPricePerKg()));
            
            if (result) {
                sendJsonResponse(response, new UpdateFruitResponse(true, "Fruit updated successfully"));
            } else {
                sendJsonResponse(response, new UpdateFruitResponse(false, "Fruit not found"));
            }
            
        } catch (Exception e) {
            System.err.println("Error in UpdateFruitServlet: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "PUT");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(200);
    }
}

// DeleteFruitServlet.java
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/fruits/delete")
public class DeleteFruitServlet extends BaseFruitServlet {
    private static final long serialVersionUID = 1L;
    
    public static class DeleteFruitResponse {
        private boolean success;
        private String message;
        
        public DeleteFruitResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String fruitName = request.getParameter("name");
            
            if (fruitName == null || fruitName.trim().isEmpty()) {
                sendErrorResponse(response, "Fruit name parameter is required", 400);
                return;
            }
            
            Boolean result = taskRegistry.executeTask(new DeleteFruitPrice(fruitName));
            
            if (result) {
                sendJsonResponse(response, new DeleteFruitResponse(true, "Fruit deleted successfully"));
            } else {
                sendJsonResponse(response, new DeleteFruitResponse(false, "Fruit not found"));
            }
            
        } catch (Exception e) {
            System.err.println("Error in DeleteFruitServlet: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(200);
    }
}

// CalculateCostServlet.java
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;

@WebServlet("/api/fruits/calculate")
public class CalculateCostServlet extends BaseFruitServlet {
    private static final long serialVersionUID = 1L;
    
    public static class CalculateCostRequest {
        private String fruitName;
        private double quantity;
        
        public String getFruitName() { return fruitName; }
        public void setFruitName(String fruitName) { this.fruitName = fruitName; }
        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }
    }
    
    public static class CalculateCostResponse {
        private boolean success;
        private String fruitName;
        private double quantity;
        private double pricePerKg;
        private double totalCost;
        private String message;
        
        public CalculateCostResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public CalculateCostResponse(String fruitName, double quantity, double pricePerKg, double totalCost) {
            this.success = true;
            this.fruitName = fruitName;
            this.quantity = quantity;
            this.pricePerKg = pricePerKg;
            this.totalCost = totalCost;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getFruitName() { return fruitName; }
        public double getQuantity() { return quantity; }
        public double getPricePerKg() { return pricePerKg; }
        public double getTotalCost() { return totalCost; }
        public String getMessage() { return message; }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            CalculateCostRequest req = gson.fromJson(sb.toString(), CalculateCostRequest.class);
            
            if (req.getFruitName() == null || req.getFruitName().trim().isEmpty()) {
                sendErrorResponse(response, "Fruit name is required", 400);
                return;
            }
            
            if (req.getQuantity() <= 0) {
                sendErrorResponse(response, "Quantity must be greater than 0", 400);
                return;
            }
            
            Double totalCost = taskRegistry.executeTask(new CalFruitCost(req.getFruitName(), req.getQuantity()));
            
            if (totalCost == -1.0) {
                sendJsonResponse(response, new CalculateCostResponse(false, "Fruit not found"));
            } else {
                double pricePerKg = totalCost / req.getQuantity();
                sendJsonResponse(response, new CalculateCostResponse(req.getFruitName(), req.getQuantity(), pricePerKg, totalCost));
            }
            
        } catch (Exception e) {
            System.err.println("Error in CalculateCostServlet: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(200);
    }
}

// GenerateReceiptServlet.java
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.Map;

@WebServlet("/api/receipt/generate")
public class GenerateReceiptServlet extends BaseFruitServlet {
    private static final long serialVersionUID = 1L;
    
    public static class GenerateReceiptRequest {
        private Map<String, Double> fruitQuantities;
        private double amountPaid;
        private String cashier;
        
        public Map<String, Double> getFruitQuantities() { return fruitQuantities; }
        public void setFruitQuantities(Map<String, Double> fruitQuantities) { this.fruitQuantities = fruitQuantities; }
        public double getAmountPaid() { return amountPaid; }
        public void setAmountPaid(double amountPaid) { this.amountPaid = amountPaid; }
        public String getCashier() { return cashier; }
        public void setCashier(String cashier) { this.cashier = cashier; }
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            GenerateReceiptRequest req = gson.fromJson(sb.toString(), GenerateReceiptRequest.class);
            
            if (req.getFruitQuantities() == null || req.getFruitQuantities().isEmpty()) {
                sendErrorResponse(response, "Fruit quantities are required", 400);
                return;
            }
            
            if (req.getCashier() == null || req.getCashier().trim().isEmpty()) {
                sendErrorResponse(response, "Cashier name is required", 400);
                return;
            }
            
            if (req.getAmountPaid() < 0) {
                sendErrorResponse(response, "Amount paid must be non-negative", 400);
                return;
            }
            
            Receipt receipt = taskRegistry.executeTask(new CalculateCost(req.getFruitQuantities(), req.getAmountPaid(), req.getCashier()));
            sendJsonResponse(response, receipt);
            
        } catch (Exception e) {
            System.err.println("Error in GenerateReceiptServlet: " + e.getMessage());
            sendErrorResponse(response, "Internal server error", 500);
        }
    }
    
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setStatus(200);
    }
}

// =============================================================================
// WEB.XML CONFIGURATION
// =============================================================================

/*
Create a web.xml file in your WEB-INF directory with this content:

<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee 
         http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
    
    <display-name>Fruit Service Engine</display-name>
    
    <!-- Add GSON library dependency -->
    <context-param>
        <param-name>gson-version</param-name>
        <param-value>2.8.9</param-value>
    </context-param>
    
    <!-- CORS Filter for handling preflight requests -->
    <filter>
        <filter-name>CorsFilter</filter-name>
        <filter-class>CorsFilter</filter-class>
    </filter>
    
    <filter-mapping>
        <filter-name>CorsFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
    
    <welcome-file-list>
        <welcome-file>index.html</welcome-file>
    </welcome-file-list>
    
</web-app>
*/

// =============================================================================
// CORS FILTER
// =============================================================================

// CorsFilter.java - Handle CORS for Android client
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public class CorsFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Set CORS headers
        httpResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        httpResponse.setHeader("Access-Control-Max-Age", "3600");
        
        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        // No cleanup needed
    }
}

// =============================================================================
// MAVEN POM.XML DEPENDENCIES
// =============================================================================

/*
Add these dependencies to your pom.xml:

<dependencies>
    <!-- Servlet API -->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>4.0.1</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- JSON processing -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.8.9</version>
    </dependency>
    
    <!-- For logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.32</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.1</version>
            <configuration>
                <source>8</source>
                <target>8</target>
            </configuration>
        </plugin>
        
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-war-plugin</artifactId>
            <version>3.2.3</version>
        </plugin>
    </plugins>
</build>
*/

Now let me create the Android client application using Kotlin and Jetpack Compose:
// =============================================================================
// ANDROID MANIFEST (app/src/main/AndroidManifest.xml)
// =============================================================================

/*
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.FruitService"
        android:usesCleartextTraffic="true"
        tools:targetApi="31">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.FruitService">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
*/

// =============================================================================
// GRADLE DEPENDENCIES (app/build.gradle.kts)
// =============================================================================

/*
dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-graphics:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Network
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // JSON
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}
*/

// =============================================================================
// DATA MODELS
// =============================================================================

// FruitModels.kt
package com.example.fruitservice.model

import com.google.gson.annotations.SerializedName

data class FruitPrice(
    val fruitName: String,
    val pricePerKg: Double
)

data class AddFruitRequest(
    val fruitName: String,
    val pricePerKg: Double
)

data class AddFruitResponse(
    val success: Boolean,
    val message: String
)

data class UpdateFruitRequest(
    val fruitName: String,
    val pricePerKg: Double
)

data class UpdateFruitResponse(
    val success: Boolean,
    val message: String
)

data class DeleteFruitResponse(
    val success: Boolean,
    val message: String
)

data class CalculateCostRequest(
    val fruitName: String,
    val quantity: Double
)

data class CalculateCostResponse(
    val success: Boolean,
    val fruitName: String? = null,
    val quantity: Double? = null,
    val pricePerKg: Double? = null,
    val totalCost: Double? = null,
    val message: String? = null
)

data class GenerateReceiptRequest(
    val fruitQuantities: Map<String, Double>,
    val amountPaid: Double,
    val cashier: String
)

data class Receipt(
    val items: List<String>,
    val totalCost: Double,
    val amountPaid: Double,
    val change: Double,
    val cashier: String,
    val timestamp: String
)

data class ErrorResponse(
    val error: String
)

// =============================================================================
// NETWORK SERVICE
// =============================================================================

// FruitApiService.kt
package com.example.fruitservice.network

import com.example.fruitservice.model.*
import retrofit2.Response
import retrofit2.http.*

interface FruitApiService {
    
    @POST("api/fruits/add")
    suspend fun addFruit(@Body request: AddFruitRequest): Response<AddFruitResponse>
    
    @PUT("api/fruits/update")
    suspend fun updateFruit(@Body request: UpdateFruitRequest): Response<UpdateFruitResponse>
    
    @DELETE("api/fruits/delete")
    suspend fun deleteFruit(@Query("name") fruitName: String): Response<DeleteFruitResponse>
    
    @POST("api/fruits/calculate")
    suspend fun calculateCost(@Body request: CalculateCostRequest): Response<CalculateCostResponse>
    
    @POST("api/receipt/generate")
    suspend fun generateReceipt(@Body request: GenerateReceiptRequest): Response<Receipt>
}

// NetworkModule.kt
package com.example.fruitservice.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    
    // Change this to your server IP address
    private const val BASE_URL = "http://10.0.2.2:8080/"  // For Android emulator
    // private const val BASE_URL = "http://192.168.1.100:8080/"  // For real device
    
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val fruitApiService: FruitApiService = retrofit.create(FruitApiService::class.java)
}

// =============================================================================
// REPOSITORY
// =============================================================================

// FruitRepository.kt
package com.example.fruitservice.repository

import com.example.fruitservice.model.*
import com.example.fruitservice.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FruitRepository {
    
    private val apiService = NetworkModule.fruitApiService
    
    suspend fun addFruit(fruitName: String, pricePerKg: Double): Result<AddFruitResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.addFruit(AddFruitRequest(fruitName, pricePerKg))
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to add fruit: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun updateFruit(fruitName: String, pricePerKg: Double): Result<UpdateFruitResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateFruit(UpdateFruitRequest(fruitName, pricePerKg))
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to update fruit: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun deleteFruit(fruitName: String): Result<DeleteFruitResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteFruit(fruitName)
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to delete fruit: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun calculateCost(fruitName: String, quantity: Double): Result<CalculateCostResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.calculateCost(CalculateCostRequest(fruitName, quantity))
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to calculate cost: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    suspend fun generateReceipt(
        fruitQuantities: Map<String, Double>,
        amountPaid: Double,
        cashier: String
    ): Result<Receipt> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.generateReceipt(
                GenerateReceiptRequest(fruitQuantities, amountPaid, cashier)
            )
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to generate receipt: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// =============================================================================
// VIEW MODEL
// =============================================================================

// FruitViewModel.kt
package com.example.fruitservice.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fruitservice.model.*
import com.example.fruitservice.repository.FruitRepository
import kotlinx.coroutines.launch

class FruitViewModel : ViewModel() {
    
    private val repository = FruitRepository()
    
    var uiState by mutableStateOf(FruitUiState())
        private set
    
    fun addFruit(fruitName: String, pricePerKg: Double) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            
            repository.addFruit(fruitName, pricePerKg)
                .onSuccess { response ->
                    uiState = uiState.copy(
                        isLoading = false,
                        successMessage = response.message,
                        lastOperationSuccess = response.success
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unknown error",
                        lastOperationSuccess = false
                    )
                }
        }
    }
    
    fun updateFruit(fruitName: String, pricePerKg: Double) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            
            repository.updateFruit(fruitName, pricePerKg)
                .onSuccess { response ->
                    uiState = uiState.copy(
                        isLoading = false,
                        successMessage = response.message,
                        lastOperationSuccess = response.success
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unknown error",
                        lastOperationSuccess = false
                    )
                }
        }
    }
    
    fun deleteFruit(fruitName: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            
            repository.deleteFruit(fruitName)
                .onSuccess { response ->
                    uiState = uiState.copy(
                        isLoading = false,
                        successMessage = response.message,
                        lastOperationSuccess = response.success
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unknown error",
                        lastOperationSuccess = false
                    )
                }
        }
    }
    
    fun calculateCost(fruitName: String, quantity: Double) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            
            repository.calculateCost(fruitName, quantity)
                .onSuccess { response ->
                    if (response.success && response.totalCost != null) {
                        uiState = uiState.copy(
                            isLoading = false,
                            calculatedCost = response,
                            lastOperationSuccess = true
                        )
                    } else {
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage = response.message ?: "Calculation failed",
                            lastOperationSuccess = false
                        )
                    }
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unknown error",
                        lastOperationSuccess = false
                    )
                }
        }
    }
    
    fun generateReceipt(fruitQuantities: Map<String, Double>, amountPaid: Double, cashier: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            
            repository.generateReceipt(fruitQuantities, amountPaid, cashier)
                .onSuccess { receipt ->
                    uiState = uiState.copy(
                        isLoading = false,
                        generatedReceipt = receipt,
                        lastOperationSuccess = true
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unknown error",
                        lastOperationSuccess = false
                    )
                }
        }
    }
    
    fun clearMessages() {
        uiState = uiState.copy(
            successMessage = null,
            errorMessage = null,
            calculatedCost = null,
            generatedReceipt = null
        )
    }
}

data class FruitUiState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val lastOperationSuccess: Boolean = false,
    val calculatedCost: CalculateCostResponse? = null,
    val generatedReceipt: Receipt? = null
)

Now let me create the UI components for the Android app:
// =============================================================================
// MAIN ACTIVITY
// =============================================================================

// MainActivity.kt
package com.example.fruitservice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fruitservice.ui.theme.FruitServiceTheme
import com.example.fruitservice.viewmodel.FruitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FruitServiceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: FruitViewModel = viewModel()
                    FruitServiceApp(viewModel = viewModel)
                }
            }
        }
    }
}

// =============================================================================
// MAIN APP COMPOSABLE
// =============================================================================

// FruitServiceApp.kt
package com.example.fruitservice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fruitservice.viewmodel.FruitViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FruitServiceApp(
    viewModel: FruitViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Add Fruit", "Update Fruit", "Delete Fruit", "Calculate Cost", "Generate Receipt")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Fruit Service Engine",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { 
                            selectedTab = index
                            viewModel.clearMessages()
                        },
                        text = { 
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            ) 
                        }
                    )
                }
            }
            
            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> AddFruitScreen(viewModel)
                    1 -> UpdateFruitScreen(viewModel)
                    2 -> DeleteFruitScreen(viewModel)
                    3 -> CalculateCostScreen(viewModel)
                    4 -> GenerateReceiptScreen(viewModel)
                }
            }
        }
    }
}

// =============================================================================
// ADD FRUIT SCREEN
// =============================================================================

@Composable
fun AddFruitScreen(
    viewModel: FruitViewModel,
    modifier: Modifier = Modifier
) {
    var fruitName by remember { mutableStateOf("") }
    var pricePerKg by remember { mutableStateOf("") }
    val uiState = viewModel.uiState
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Add New Fruit",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedTextField(
            value = fruitName,
            onValueChange = { fruitName = it },
            label = { Text("Fruit Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = pricePerKg,
            onValueChange = { pricePerKg = it },
            label = { Text("Price per KG ($)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Button(
            onClick = {
                val price = pricePerKg.toDoubleOrNull()
                if (fruitName.isNotBlank() && price != null && price > 0) {
                    viewModel.addFruit(fruitName.trim(), price)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && fruitName.isNotBlank() && 
                     pricePerKg.toDoubleOrNull()?.let { it > 0 } == true
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Add Fruit")
            }
        }
        
        // Status Messages
        StatusMessages(uiState)
    }
}

// =============================================================================
// UPDATE FRUIT SCREEN
// =============================================================================

@Composable
fun UpdateFruitScreen(
    viewModel: FruitViewModel,
    modifier: Modifier = Modifier
) {
    var fruitName by remember { mutableStateOf("") }
    var pricePerKg by remember { mutableStateOf("") }
    val uiState = viewModel.uiState
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Update Fruit Price",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedTextField(
            value = fruitName,
            onValueChange = { fruitName = it },
            label = { Text("Fruit Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = pricePerKg,
            onValueChange = { pricePerKg = it },
            label = { Text("New Price per KG ($)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Button(
            onClick = {
                val price = pricePerKg.toDoubleOrNull()
                if (fruitName.isNotBlank() && price != null && price > 0) {
                    viewModel.updateFruit(fruitName.trim(), price)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && fruitName.isNotBlank() && 
                     pricePerKg.toDoubleOrNull()?.let { it > 0 } == true
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Update Fruit")
            }
        }
        
        StatusMessages(uiState)
    }
}

// =============================================================================
// COMPLETE DELETE FRUIT SCREEN
// =============================================================================

@Composable
fun DeleteFruitScreen(
    viewModel: FruitViewModel,
    modifier: Modifier = Modifier
) {
    var fruitName by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val uiState = viewModel.uiState
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Delete Fruit",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedTextField(
            value = fruitName,
            onValueChange = { fruitName = it },
            label = { Text("Fruit Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Button(
            onClick = { showConfirmDialog = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && fruitName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onError
                )
            } else {
                Text("Delete Fruit")
            }
        }
        
        StatusMessages(uiState)
        
        // Confirmation Dialog
        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Deletion") },
                text = { Text("Are you sure you want to delete '$fruitName'? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteFruit(fruitName.trim())
                            showConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showConfirmDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// =============================================================================
// CALCULATE COST SCREEN
// =============================================================================

@Composable
fun CalculateCostScreen(
    viewModel: FruitViewModel,
    modifier: Modifier = Modifier
) {
    var fruitName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    val uiState = viewModel.uiState
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Calculate Cost",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedTextField(
            value = fruitName,
            onValueChange = { fruitName = it },
            label = { Text("Fruit Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = quantity,
            onValueChange = { quantity = it },
            label = { Text("Quantity (KG)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Button(
            onClick = {
                val qty = quantity.toDoubleOrNull()
                if (fruitName.isNotBlank() && qty != null && qty > 0) {
                    viewModel.calculateCost(fruitName.trim(), qty)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && fruitName.isNotBlank() && 
                     quantity.toDoubleOrNull()?.let { it > 0 } == true
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Calculate Cost")
            }
        }
        
        // Display calculation result
        uiState.calculatedCost?.let { cost ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Calculation Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Fruit: ${cost.fruitName}")
                    Text("Quantity: ${cost.quantity} kg")
                    Text("Price per KG: $${String.format("%.2f", cost.pricePerKg)}")
                    Divider()
                    Text(
                        text = "Total Cost: $${String.format("%.2f", cost.totalCost)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        StatusMessages(uiState)
    }
}

// =============================================================================
// GENERATE RECEIPT SCREEN
// =============================================================================

@Composable
fun GenerateReceiptScreen(
    viewModel: FruitViewModel,
    modifier: Modifier = Modifier
) {
    var cashier by remember { mutableStateOf("") }
    var amountPaid by remember { mutableStateOf("") }
    var fruitItems by remember { mutableStateOf(listOf<FruitItem>()) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    val uiState = viewModel.uiState
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Generate Receipt",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        OutlinedTextField(
            value = cashier,
            onValueChange = { cashier = it },
            label = { Text("Cashier Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        // Fruit Items Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fruit Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showAddItemDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Item"
                        )
                    }
                }
                
                if (fruitItems.isEmpty()) {
                    Text(
                        text = "No items added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    fruitItems.forEachIndexed { index, item ->
                        FruitItemRow(
                            item = item,
                            onRemove = {
                                fruitItems = fruitItems.toMutableList().apply {
                                    removeAt(index)
                                }
                            }
                        )
                    }
                }
            }
        }
        
        OutlinedTextField(
            value = amountPaid,
            onValueChange = { amountPaid = it },
            label = { Text("Amount Paid ($)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Button(
            onClick = {
                val amount = amountPaid.toDoubleOrNull()
                if (cashier.isNotBlank() && amount != null && amount >= 0 && fruitItems.isNotEmpty()) {
                    val fruitQuantities = fruitItems.associate { it.name to it.quantity }
                    viewModel.generateReceipt(fruitQuantities, amount, cashier.trim())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading && cashier.isNotBlank() && 
                     amountPaid.toDoubleOrNull()?.let { it >= 0 } == true &&
                     fruitItems.isNotEmpty()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Generate Receipt")
            }
        }
        
        // Display receipt
        uiState.generatedReceipt?.let { receipt ->
            ReceiptCard(receipt)
        }
        
        StatusMessages(uiState)
        
        // Add Item Dialog
        if (showAddItemDialog) {
            AddFruitItemDialog(
                onDismiss = { showAddItemDialog = false },
                onAdd = { item ->
                    fruitItems = fruitItems + item
                    showAddItemDialog = false
                }
            )
        }
    }
}

// =============================================================================
// SUPPORTING COMPOSABLES
// =============================================================================

@Composable
fun StatusMessages(uiState: FruitUiState) {
    uiState.successMessage?.let { message ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
    
    uiState.errorMessage?.let { message ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun FruitItemRow(
    item: FruitItem,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${item.quantity} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRemove
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove Item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddFruitItemDialog(
    onDismiss: () -> Unit,
    onAdd: (FruitItem) -> Unit
) {
    var fruitName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Fruit Item") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fruitName,
                    onValueChange = { fruitName = it },
                    label = { Text("Fruit Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (KG)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull()
                    if (fruitName.isNotBlank() && qty != null && qty > 0) {
                        onAdd(FruitItem(fruitName.trim(), qty))
                    }
                },
                enabled = fruitName.isNotBlank() && quantity.toDoubleOrNull()?.let { it > 0 } == true
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReceiptCard(receipt: Receipt) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "RECEIPT",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Divider()
            
            Text("Date: ${receipt.timestamp}")
            Text("Cashier: ${receipt.cashier}")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Items:",
                fontWeight = FontWeight.Bold
            )
            
            receipt.items.forEach { item ->
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Divider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Cost:")
                Text(
                    text = "$${String.format("%.2f", receipt.totalCost)}",
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Amount Paid:")
                Text("$${String.format("%.2f", receipt.amountPaid)}")
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Change:")
                Text(
                    text = "$${String.format("%.2f", receipt.change)}",
                    fontWeight = FontWeight.Bold,
                    color = if (receipt.change >= 0) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// =============================================================================
// DATA CLASSES FOR UI
// =============================================================================

data class FruitItem(
    val name: String,
    val quantity: Double
)

// =============================================================================
// MISSING IMPORTS FOR COMPOSE
// =============================================================================

/*
Add these imports to your MainActivity.kt and other Compose files:

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
*/

// =============================================================================
// THEME CONFIGURATION
// =============================================================================

// ui/theme/Theme.kt
package com.example.fruitservice.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun FruitServiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// ui/theme/Color.kt
package com.example.fruitservice.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ui/theme/Type.kt
package com.example.fruitservice.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

// =============================================================================
// DEPLOYMENT INSTRUCTIONS
// =============================================================================

/*
DEPLOYMENT STEPS:

1. START RMI SERVER:
   - Compile all Java files: javac *.java
   - Start the server: java FruitComputeEngine
   - Server will run on port 1099

2. DEPLOY WEB APPLICATION:
   - Package into WAR file using Maven: mvn clean package
   - Deploy to Tomcat server (port 8080)
   - Ensure GSON library is in classpath

3. CONFIGURE ANDROID APP:
   - Update BASE_URL in NetworkModule.kt:
     * For emulator: "http://10.0.2.2:8080/"
     * For real device: "http://YOUR_SERVER_IP:8080/"
   - Add internet permission in AndroidManifest.xml

4. BUILD AND RUN ANDROID APP:
   - Sync project with Gradle files
   - Build and install on device/emulator
   - Ensure device can reach the server

5. TESTING:
   - Test RMI server independently using FruitComputeTaskRegistry
   - Test servlets using browser or Postman
   - Test Android app end-to-end functionality

SERVER REQUIREMENTS:
- Java 8+
- Apache Tomcat 9+
- Network connectivity between server and Android device

ANDROID REQUIREMENTS:
- Android API 24+ (Android 7.0)
- Internet permission
- Network access to server
*/
