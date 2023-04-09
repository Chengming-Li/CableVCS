import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
    public final Set<String> closed;
    public final Set<String> opened;

    public Commit(String hash, String tree, String lastCommit, String time,
                  String author, String branch, String message, Path vcsDirectory, Set<String> closed, Set<String> opened) {
        this.hash = hash;
        this.tree = tree;
        this.lastHash = lastCommit;
        this.time = time;
        this.author = author;
        this.branch = branch;
        this.message = message;
        this.vcsDirectory = vcsDirectory;
        this.closed = closed;
        this.opened = opened;
    }
    public Commit(String hash, String tree, String lastCommit, String time,
                  String author, String branch, String message, Path vcsDirectory, Tree treeObject, Set<String> closed, Set<String> opened) {
        this.hash = hash;
        this.tree = tree;
        this.lastHash = lastCommit;
        this.time = time;
        this.author = author;
        this.branch = branch;
        this.message = message;
        this.vcsDirectory = vcsDirectory;
        this.treeObject = treeObject;
        this.closed = closed;
        this.opened = opened;
    }

    /**
     * Returns a commit object representing the parent commit of this commit
     * @param cache: a hash : Commit map, passed in so already created Commit objects don't need to be re-instantiated
     * @return the parent commit object
     */
    public Commit parentCommit(Map<String, Commit> cache) throws Exception {
        if ("".equals(lastHash)) {
            return null;
        }
        return findCommit(lastHash, this.vcsDirectory, cache);
    }
    public Commit parentCommit() throws Exception {
        if ("".equals(lastHash)) {
            return null;
        }
        return findCommit(lastHash, this.vcsDirectory);
    }

    /**
     * Returns a tree object of the tree in this commit
     * @return tree object
     */
    public Tree getTree() throws Exception {
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
    public static Commit findCommit(String hash, Path vcsDirectory) throws Exception {
            List<String> commit = Files.readAllLines(findHash(hash, vcsDirectory));
            if (commit.size() == 0) {
                return new InitialCommit(hash);
            }
            int i = 6;
            Set<String> opened = new HashSet<>();
            while (!"===".equals(commit.get(i))) {
                opened.add(commit.get(i));
                i++;
            }
            i++;
            Set<String> closed = new HashSet<>();
            while (!"===".equals(commit.get(i))) {
                closed.add(commit.get(i));
                i++;
            }
            i++;
            StringBuilder sb = new StringBuilder();
            while (i < commit.size()) {
                sb.append(commit.get(i)).append("\n");
                i++;
            }
            return new Commit(hash, commit.get(0), commit.get(1),
                    commit.get(2), commit.get(3), commit.get(4), sb.toString(), vcsDirectory, opened, closed);
    }
    public static Commit findCommit(String hash, Path vcsDirectory, Map<String, Commit> cache) throws Exception {
        if (cache.containsKey(hash)) {
            return cache.get(hash);
        }
        return findCommit(hash, vcsDirectory);
    }

    /**
     * returns commit object representing the head commit of a branch
     * @param vcsDirectory: the Path to the .vcs directory
     * @param branch: the branch the returned head commit belongs to. If no branch is entered, use current branch
     * @return commit object
     */
    public static Commit getHeadCommit(Path vcsDirectory, String branch) throws Exception {
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
    }
    public static Commit getHeadCommit(Path vcsDirectory) throws Exception {
            String headBranch = Files.readAllLines(vcsDirectory.resolve("HEAD")).get(0);
            List<String> lastCommit = Files.readAllLines(Path.of(headBranch));
            if (lastCommit.size() == 0) {
                return null;
            } else {
                return findCommit(lastCommit.get(0), vcsDirectory);
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
    public static Commit writeCommit(String user,
                                     String message, Path vcsDirectory, Commit current, Map<String, String> index, String branch, String[] closed, String[][] opened) throws Exception {
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
        sb.append(time).append("\n").append(user).append("\n").append(branch).append("\n===\n");
        for (String s : closed) {
            sb.append(s).append("\n");
        }
        Set<String> o = new HashSet<>();
        sb.append("===\n");
        for (String[] s : opened) {
            sb.append(s[0]).append("\n");
            o.add(s[0]);
        }
        sb.append("===\n");
        sb.append(message);
        String hash = hash(sb.toString());
        createFile(sb.toString(), hash, vcsDirectory);
        return new Commit(hash, tree.hash, lastHash, time, user, branch, message, vcsDirectory, tree, new HashSet<>(Arrays.asList(closed)), o);
    }

    /**
     * Creates an initial commit object
     * @param vcsDirectory: path to the .vcs directory
     * @return returns the newly created commit object
     */
    public static Commit writeInitialCommit(Path vcsDirectory) throws Exception {
        String hash = hash("");
        createFile("", hash, vcsDirectory);
        return new InitialCommit(hash);
    }
}
