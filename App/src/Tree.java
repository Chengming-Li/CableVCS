import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Tree extends VCSUtils{
    public final String hash;
    public final Map<String, String> map;

    public Tree(String hash, Map<String, String> map) {
        this.hash = hash;
        this.map = map;
    }
    public static Tree findTree(String hash, Path vcsDirectory) {
        try (BufferedReader br = new BufferedReader(new FileReader(findHash(hash, vcsDirectory).toFile()))){
            Map<String, String> map = new HashMap<>();
            String line;
            String[] split;
            while ((line = br.readLine()) != null) {
                split = line.split(" ");
                map.put(split[0], split[1]);
            }
            return new Tree(hash, map);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static Tree makeTree(Path vcsDirectory) {
        return null;
    }
}
