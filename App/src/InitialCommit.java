import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class InitialCommit extends Commit{
    public InitialCommit(String hash) {
        super(hash, "", "", "", "", "master", "Initial Commit", null, null, null, new HashSet<>());
    }

    @Override
    public String toString(boolean global) {
        return "";
    }
    @Override
    public Tree getTree() {
        return new Tree(null, new HashMap<>());
    }
    @Override
    public Commit parentCommit(Map<String, Commit> cache) {
        return null;
    }
    public Commit parentCommit() {
        return null;
    }
}
