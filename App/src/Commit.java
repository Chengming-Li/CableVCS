import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class Commit extends VCSUtils {
    public final String hash;
    public final String tree;
    private Tree treeObject;
    private final String lastHash;
    public final String time;
    public final String author;
    public final String message;
    public final String branch;
    private final Path vcsDirectory;
    public Commit(String hash, String tree, String lastCommit, String time, String author, String branch, String message, Path vcsDirectory) {
        this.hash = hash;
        this.tree = tree;
        this.lastHash = lastCommit;
        this.time = time;
        this.author = author;
        this.branch = branch;
        this.message = message;
        this.vcsDirectory = vcsDirectory;
    }
    public Commit(String hash, String tree, String lastCommit, String time, String author, String branch, String message, Path vcsDirectory, Tree treeObject) {
        this.hash = hash;
        this.tree = tree;
        this.lastHash = lastCommit;
        this.time = time;
        this.author = author;
        this.branch = branch;
        this.message = message;
        this.vcsDirectory = vcsDirectory;
        this.treeObject = treeObject;
    }
    public Commit parentCommit() {
        if ("".equals(lastHash)) {
            return null;
        }
        return findCommit(hash, this.vcsDirectory);
    }
    public Commit parentCommit(Map<String, Commit> cache) {
        if ("".equals(lastHash)) {
            return null;
        } else if (cache.containsKey(lastHash)) {
            return cache.get(lastHash);
        }
        return findCommit(hash, this.vcsDirectory);
    }
    public Tree getTree() {
        if (this.treeObject == null) {
            this.treeObject = Tree.findTree(this.tree, this.vcsDirectory);
        }
        return treeObject;
    }
    public String toString(boolean global) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("===\ncommit %s\nDate: %s\nAuthor: %s\n", hash, time, author));
        if (global) {
            sb.append(String.format("Branch: %s\n", this.branch));
        }
        sb.append(this.message).append("\n");
        return sb.toString();
    }
    public static Commit findCommit(String hash, Path vcsDirectory) {
        try {
            List<String> commit = Files.readAllLines(findHash(hash, vcsDirectory));
            return new Commit(hash, commit.get(0), commit.get(1), commit.get(2), commit.get(3), commit.get(4), commit.get(5), vcsDirectory);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static Commit getHeadCommit(Path vcsDirectory) {
        try {
            String headBranch = Files.readAllLines(vcsDirectory.resolve("HEAD")).get(0);
            List<String> lastCommit = Files.readAllLines(Path.of(headBranch));
            if (lastCommit.size() == 0) {
                return null;
            } else {
                return findCommit(lastCommit.get(0), vcsDirectory);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Commit writeCommit(String user, String message, Path vcsDirectory, Commit current, Map<String, String> index, String branch) {
        StringBuilder sb = new StringBuilder();
        Tree tree = Tree.makeTree(vcsDirectory, index, current);
        sb.append(tree.hash).append("\n");
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
        String lastHash;
        if (current == null) {
            sb.append("").append("\n");
            lastHash = "";
        } else {
            sb.append(current.hash).append("\n");
            lastHash = current.hash;
        }
        sb.append(time).append("\n").append(user).append("\n").append(branch).append("\n").append(message);
        String hash = hash(sb.toString());
        createFile(sb.toString(), hash, vcsDirectory);
        return new Commit(hash, tree.hash, lastHash, time, user, branch, message, vcsDirectory, tree);
    }
}
