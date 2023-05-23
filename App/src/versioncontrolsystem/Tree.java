package versioncontrolsystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Tree extends VCSUtils {
    public final String hash;
    public final Map<String, String> map;

    public Tree(String hash, Map<String, String> map) {
        this.hash = hash;
        this.map = map;
    }

    /**
     * If the file is tracked in the commit
     * @param name: name of file
     * @return boolean if file is tracked or not
     */
    public boolean contains(String name) {
        return this.map.containsKey(name);
    }

    /**
     * returns the versioncontrolsystem.Tree object under the hash
     * @param hash: hash of the desired tree
     * @param vcsDirectory: path to the .vcs directory
     * @return tree object
     */
    public static Tree findTree(String hash, Path vcsDirectory) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(findHash(hash, vcsDirectory).toFile()))) {
            Map<String, String> map = new HashMap<>();
            String line;
            String[] split;
            while ((line = br.readLine()) != null) {
                split = line.split(" ");
                map.put(split[0].trim(), split[1].trim());
            }
            return new Tree(hash, map);
        }
    }

    /**
     * Creates and returns a new tree object
     * @param vcsDirectory: path to the .vcs directory
     * @param index: the map representing the contents of the index file
     * @param commit: the current commit
     * @return a tree object
     */
    public static Tree makeTree(Path vcsDirectory, Map<String, String> index, Commit commit) throws Exception {
            StringBuilder sb = new StringBuilder();
            Map<String, String> map;
            if (commit == null) {
                map = new HashMap<>();
                for (String path : index.keySet()) {
                    map.put(path, index.get(path).substring(0, index.get(path).length() - 2));
                }
            } else {
                map = new HashMap<>(commit.getTree().map);
                for (String key : index.keySet()) {
                    if (index.get(key).endsWith("2")) {
                        map.remove(key);
                    } else {
                        map.put(key, index.get(key).substring(0, index.get(key).length() - 2));
                    }
                }
            }
            for (String key : map.keySet()) {
                sb.append(String.format("%s %s\n", key, map.get(key)));
            }
            String hash = hash(sb.toString());
            createFile(sb.toString(), hash, vcsDirectory);
            return new Tree(hash, map);
    }
}
