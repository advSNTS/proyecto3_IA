package com.example;

import java.io.*;
import java.util.*;
import javax.swing.*;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

// Clase para representar un nodo en la red bayesiana (sin cambios)
class Node {
    String name;
    List<String> values;
    List<Node> parents;
    Map<String, Double> probabilities;
    
    public Node(String name) {
        this.name = name;
        this.values = new ArrayList<>();
        this.parents = new ArrayList<>();
        this.probabilities = new HashMap<>();
    }
    
    public void addValue(String value) {
        if (!values.contains(value)) {
            values.add(value);
        }
    }
    
    public void addParent(Node parent) {
        this.parents.add(parent);
    }
    
    public String getKey(List<String> parentValues) {
        StringBuilder key = new StringBuilder();
        for (String value : parentValues) {
            if (value != null) {
                key.append(value).append(":");
            }
        }
        return key.toString();
    }
    
    public void setProbability(List<String> parentValues, String value, double prob) {
        String key = getKey(parentValues) + value;
        probabilities.put(key, prob);
    }
    
    public double getProbability(List<String> parentValues, String value) {
        String key = getKey(parentValues) + value;
        return probabilities.getOrDefault(key, 0.0);
    }
    
    @Override
    public String toString() {
        return "Node{" + "name='" + name + '\'' + ", values=" + values + 
               ", parents=" + parents.size() + '}';
    }
}

// Clase principal para la red bayesiana CON VISUALIZACIÓN JGraphX
public class RedBayesiana {
    private Map<String, Node> nodes;
    private List<String[]> dependencies;
    
    public RedBayesiana() {
        this.nodes = new HashMap<>();
        this.dependencies = new ArrayList<>();
    }
    
    // 1. Función para leer archivos CSV de dependencias
    public void loadDependencies(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        
        // Saltar header si existe
        br.readLine();
        
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length >= 2) {
                dependencies.add(parts);
                
                String child = parts[0].trim();
                String parent = parts[1].trim();
                
                // Crear nodos si no existen
                if (!nodes.containsKey(child)) {
                    nodes.put(child, new Node(child));
                }
                if (!nodes.containsKey(parent)) {
                    nodes.put(parent, new Node(parent));
                }
                
                // Establecer relación padre-hijo
                nodes.get(child).addParent(nodes.get(parent));
            }
        }
        br.close();
    }
    
    // 2. Función para leer archivos CSV de probabilidades
    public void loadProbabilities(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        
        Node currentNode = null;
        
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue; // Saltar líneas vacías
            }
            
            String[] data = line.split(",");
            for (int i = 0; i < data.length; i++) {
                data[i] = data[i].trim();
            }
            
            // Verificar si es una línea de header de sección
            if (data.length >= 2 && nodes.containsKey(data[0]) && 
                (data[1].equalsIgnoreCase("Value") || nodes.containsKey(data[1]))) {
                currentNode = nodes.get(data[0]);
                System.out.println("Procesando sección para: " + currentNode.name);
                continue;
            }
            
            // Procesar línea de datos si tenemos un nodo actual
            if (currentNode != null && data.length >= 3 && isNumeric(data[data.length - 1])) {
                processDataLine(currentNode, data);
            }
        }
        br.close();
    }
    
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private void processDataLine(Node node, String[] data) {
        try {
            // El último elemento es la probabilidad
            double probability = Double.parseDouble(data[data.length - 1]);
            
            // El penúltimo elemento es el valor del nodo
            String nodeValue = data[data.length - 2];
            
            // Los elementos anteriores son los valores de los padres (si los hay)
            List<String> parentValues = new ArrayList<>();
            for (int i = 1; i < data.length - 2; i++) {
                parentValues.add(data[i]);
            }
            
            node.addValue(nodeValue);
            node.setProbability(parentValues, nodeValue, probability);
            
            //System.out.println("  Cargado: " + node.name + " = " + nodeValue + " | Padres: " + parentValues + " | Prob: " + probability);
            
        } catch (NumberFormatException e) {
            System.err.println("Error parseando número en línea: " + String.join(",", data));
        }
    }
    
    // 3. NUEVA FUNCIÓN: Mostrar grafo visualmente con JGraphX
    public void displayGraph() {
        try {
            // Crear el grafo
            mxGraph graph = new mxGraph();
            Object parent = graph.getDefaultParent();
            
            graph.getModel().beginUpdate();
            try {
                // Crear un mapa para almacenar los vértices
                Map<String, Object> vertexMap = new HashMap<>();
                
                // Crear todos los nodos primero
                int x = 50;
                int y = 50;
                int nodeWidth = 120;
                int nodeHeight = 40;
                int verticalSpacing = 100;
                
                // Agrupar nodos por nivel (raíces, intermedios, hojas)
                List<String> rootNodes = new ArrayList<>();
                List<String> intermediateNodes = new ArrayList<>();
                List<String> leafNodes = new ArrayList<>();
                
                for (String nodeName : nodes.keySet()) {
                    Node node = nodes.get(nodeName);
                    if (node.parents.isEmpty()) {
                        rootNodes.add(nodeName);
                    } else if (isLeafNode(nodeName)) {
                        leafNodes.add(nodeName);
                    } else {
                        intermediateNodes.add(nodeName);
                    }
                }
                
                // Posicionar nodos raíz en la parte superior
                for (int i = 0; i < rootNodes.size(); i++) {
                    String nodeName = rootNodes.get(i);
                    Object vertex = graph.insertVertex(parent, null, nodeName, 
                            x + i * (nodeWidth + 50), y, nodeWidth, nodeHeight, 
                            "fillColor=#FFCCCC;strokeColor=#CC0000;rounded=1");
                    vertexMap.put(nodeName, vertex);
                }
                
                // Posicionar nodos intermedios en el centro
                y += verticalSpacing;
                for (int i = 0; i < intermediateNodes.size(); i++) {
                    String nodeName = intermediateNodes.get(i);
                    Object vertex = graph.insertVertex(parent, null, nodeName, 
                            x + i * (nodeWidth + 50), y, nodeWidth, nodeHeight, 
                            "fillColor=#CCE5FF;strokeColor=#0066CC;rounded=1");
                    vertexMap.put(nodeName, vertex);
                }
                
                // Posicionar nodos hoja en la parte inferior
                y += verticalSpacing;
                for (int i = 0; i < leafNodes.size(); i++) {
                    String nodeName = leafNodes.get(i);
                    Object vertex = graph.insertVertex(parent, null, nodeName, 
                            x + i * (nodeWidth + 50), y, nodeWidth, nodeHeight, 
                            "fillColor=#CCFFCC;strokeColor=#00CC00;rounded=1");
                    vertexMap.put(nodeName, vertex);
                }
                
                // Crear las aristas (dependencias)
                for (String[] dependency : dependencies) {
                    String child = dependency[0].trim();
                    String parentNode = dependency[1].trim();
                    
                    if (vertexMap.containsKey(parentNode) && vertexMap.containsKey(child)) {
                        graph.insertEdge(parent, null, "", 
                                vertexMap.get(parentNode), vertexMap.get(child),
                                "strokeColor=#666666;endArrow=classic;strokeWidth=2");
                    }
                }
                
            } finally {
                graph.getModel().endUpdate();
            }
            
            // Crear y mostrar la ventana
            mxGraphComponent graphComponent = new mxGraphComponent(graph);
            graphComponent.setConnectable(false);
            graphComponent.getGraph().setAllowDanglingEdges(false);
            
            JFrame frame = new JFrame("Red Bayesiana - Visualización");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.getContentPane().add(graphComponent);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null); // Centrar en pantalla
            frame.setVisible(true);
            
            System.out.println("\n=== GRÁFICO VISUAL GENERADO ===");
            System.out.println("• Nodos rojos: Variables raíz (sin padres)");
            System.out.println("• Nodos azules: Variables intermedias");
            System.out.println("• Nodos verdes: Variables hoja");
            System.out.println("• Flechas: Dirección de dependencia probabilística");
            
        } catch (Exception e) {
            System.err.println("Error al mostrar el gráfico: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Mostrando representación en consola como fallback...");
            printGraph(); // Fallback a la versión de consola
        }
    }
    
    private boolean isLeafNode(String nodeName) {
        // Un nodo es hoja si no es padre de ningún otro nodo
        for (String[] dependency : dependencies) {
            if (dependency[1].trim().equals(nodeName)) {
                return false;
            }
        }
        return true;
    }
    