/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package du.parser;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author Fernando
 */
public class DuParser {

    public static void main(String[] args) throws FileNotFoundException, IOException, InterruptedException, InvocationTargetException {
        Executor pool = Executors.newFixedThreadPool(4);
        for(String s : args)  {
            pool.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        parseFile(new File(s));
                    } catch (InvocationTargetException | IOException | InterruptedException | NumberFormatException ex) {
                        Logger.getLogger(DuParser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            
        }

    }

    private static void parseFile(File f) throws InvocationTargetException, IOException, InterruptedException, NumberFormatException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(f))))) {
            String line;
            Tree root = new Tree();
            root.name = f.getAbsolutePath();
            System.out.println("Stage 1: creating lists");
            while ((line = in.readLine()) != null) {
                String[] parts = line.split("\t", 2);
                int size = Integer.parseInt(parts[0]);
                String[] tree = parts[1].split("/");
                Tree working = root;
                for (String filePart : tree) {
                    if (filePart.isEmpty() || ".".equals(filePart)) {
                        continue;
                    }
                    if (working.children == null) {
                        working.children = new HashSet<>();
                    }
                    Tree tmp = new Tree();
                    tmp.name = filePart;
                    if(working.children.contains(tmp)) {
                        for(Tree t : working.children) {
                            if(t.equals(tmp)) {
                                tmp = t;
                            }
                        }
                    } else working.children.add(tmp);
                    working = tmp;
                }
                working.size = size;
            }
            System.out.println("Stage 2: sorting lists");
            root = sortTree(root);
            System.out.println("Stage 3: Building gui");
            final DefaultMutableTreeNode jnodes = makeNode(root);
            System.out.println("Stage 4: Rendering gui");
            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    JFrame frame = new JFrame("File system usage");
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    frame.setSize(600, 460);
                    frame.setResizable(true);
                    frame.getContentPane().setLayout(new GridBagLayout());
                    frame.getContentPane().add(new JScrollPane(new JTree(jnodes)), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, 1, new Insets(5, 5, 5, 5), 0, 0));
                    frame.setVisible(true);
                }
            });
        }
    }

    private static DefaultMutableTreeNode makeNode(Tree tree) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(tree.name + " (" + tree.size + ")");
        if (tree.children != null) {
            for (Tree t : tree.children) {
                node.add(makeNode(t));
            }
        }
        return node;
    }

    private static Tree sortTree(Tree in) {
        Tree out = new Tree();
        out.size = in.size;
        out.name = in.name;
        if (in.children instanceof SortedSet || in.children == null) {
            out.children = in.children;
        } else {
            out.children = in.children.stream().parallel().map((Tree t) -> sortTree(t)).collect(Collectors.toCollection(TreeSet::new));
        }
        return out;
    }

    private static class Tree implements Comparable<Tree> {

        public int size;

        public String name;

        public Set<Tree> children;

        @Override
        public int compareTo(Tree o) {
            int c = Integer.compare(o.size, size);
            if (c != 0) {
                return c;
            }
            return name.compareTo(o.name);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.name);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Tree other = (Tree) obj;
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return true;
        }

    }

}
