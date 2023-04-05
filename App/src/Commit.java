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

    /**
     * Returns a commit object representing the parent commit of this commit
     * @param cache: a hash : Commit map, passed in so already created Commit objects don't need to be re-instantiated
     * @return the parent commit object
     */
    public Commit parentCommit(Map<String, Commit> cache) {
        if ("".equals(lastHash)) {
            return null;
        }
        return findCommit(lastHash, this.vcsDirectory, cache);
    }
    public Commit parentCommit() {
        if ("".equals(lastHash)) {
            return null;
        }
        return findCommit(lastHash, this.vcsDirectory);
    }

    /**
     * Returns a tree object of the tree in this commit
     * @return tree object
     */
    public Tree getTree() {
        if (this.treeObject == null) {
            this.treeObject = Tree.findTree(this.tree, this.vcsDirectory);
        }
        return treeObject;
    }

    /**
     * returns a string representation of the Commit object
     * @param global: boolean, if the branch should be included or not
     * @return formatted string of the Commit
     */
    public String toString(boolean global) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("===\ncommit %s\nDate: %s\nAuthor: %s\n", hash, time, author));
        if (global) {
            sb.append(String.format("Branch: %s\n", this.branch));
        }
        sb.append(this.message).append("\n");
        return sb.toString();
    }

    /**
     * Returns a commit object of the commit in the hash
     * @param hash: hash of the desired commit
     * @param vcsDirectory: Path to the .vcs directory
     * @return commit object
     */
    public static Commit findCommit(String hash, Path vcsDirectory) {
        try {
            List<String> commit = Files.readAllLines(findHash(hash, vcsDirectory));
            return new Commit(hash, commit.get(0), commit.get(1), commit.get(2), commit.get(3), commit.get(4), commit.get(5), vcsDirectory);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static Commit findCommit(String hash, Path vcsDirectory, Map<String, Commit> cache) {
        if (cache.containsKey(hash)) {
            return cache.get(hash);
        }
        return findCommit(hash, vcsDirectory);
    }

    /**
     * returns commit object representing the head commit
     * @param vcsDirectory: the Path to the .vcs directory
     * @return commit object
     */
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
    public static Commit getHeadCommit(Path vcsDirectory, String branch) {
        try {
            Path p = vcsDirectory.resolve("Branches").resolve(branch);
            if (!Files.exists(p)) {
                return null;
            }
            List<String> lastCommit = Files.readAllLines(p);
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

    /**
     * Creates a new commit
     * @param user: author of the commit
     * @param message: commit message
     * @param vcsDirectory: path to the .vcs directory
     * @param current: current commit object
     * @param index: a map representing the contents of the index
     * @param branch: the current branch
     * @return returns the newly created commit object
     */
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
