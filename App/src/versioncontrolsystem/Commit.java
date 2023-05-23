package versioncontrolsystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
    public final List<Commit> next;
    public final Set<String> tasks;

    public Commit(String hash, String tree, String lastCommit, String time,
                  String author, String branch, String message, Path vcsDirectory, Set<String> closed,
                  Set<String> opened, Set<String> tasks) {
        this.hash = hash;
        this.tree = tree;
        this.lastHash = lastCommit;
        this.time = time;
        this.author = author;
        this.branch = branch;
        this.message = message;
        this.vcsDirectory = vcsDirectory;
        if (closed == null) {
            this.closed = new HashSet<>();
        } else {
            this.closed = closed;
        }
        if (opened == null) {
            this.opened = new HashSet<>();
        } else {
            this.opened = opened;
        }
        this.tasks = tasks;
        this.next = new LinkedList<>();
    }
    public Commit(String hash, String tree, String lastCommit, String time,
                  String author, String branch, String message, Path vcsDirectory, Tree treeObject,
                  Set<String> closed, Set<String> opened, Set<String> tasks) {
        this(hash, tree, lastCommit, time, author, branch, message, vcsDirectory, closed, opened, tasks);
        this.treeObject = treeObject;
    }

    /**
     * Returns a commit object representing the parent commit of this commit
     * @param cache: a hash : versioncontrolsystem.Commit map, passed in so already created versioncontrolsystem.Commit objects don't need to be re-instantiated
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
     * returns a string representation of the versioncontrolsystem.Commit object
     * @param global: boolean, if the branch should be included or not
     * @return formatted string of the versioncontrolsystem.Commit
     */
    public String toString(boolean global) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("===\ncommit %s\nDate: %s\nAuthor: %s\n", hash, time, author));
        if (global) {
            sb.append(String.format("Branch: %s\n", this.branch));
        }
        if (this.opened.size() > 0) {
            sb.append("Opened Tasks:\n");
            for (String s : this.opened) {
                sb.append(s).append("\n");
            }
        }
        if (this.closed.size() > 0) {
            sb.append("Closed Tasks:\n");
            for (String s : this.closed) {
                sb.append(s).append("\n");
            }
        }
        sb.append(this.message).append("\n");
        return sb.toString();
    }
    public String toOutputString(boolean global) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s\nDate: %s\nAuthor: %s\n", hash, time, author));
        if (global) {
            sb.append(String.format("Branch: %s\n", this.branch));
        }
        if (this.opened.size() > 0) {
            sb.append("\nOpened Tasks:\n");
            for (String s : this.opened) {
                sb.append(s).append("\n");
            }
        }
        if (this.closed.size() > 0) {
            sb.append("\nClosed Tasks:\n");
            for (String s : this.closed) {
                sb.append(s).append("\n");
            }
        }
        sb.append("\n");
        sb.append(this.message);
        return sb.toString();
    }

    /**
     * Returns a commit object of the commit in the hash
     * @param hash: hash of the desired commit
     * @param vcsDirectory: Path to the .vcs directory
     * @return commit object
     */
    public static Commit findCommit(String hash, Path vcsDirectory) throws Exception {
        File file = findHash(hash, vcsDirectory).toFile();
        if (!file.exists()) {
            throw new FailCaseException(String.format("Commit with hash %s does not exist", hash));
        }
        BufferedReader reader = new BufferedReader(new FileReader(file));
        if (!reader.ready()) {
            return new InitialCommit(hash);
        }
        String line;
        int i = 0;
        List<String> args = new ArrayList<>();
        while ((line = reader.readLine()) != null && !line.equals("===")) {
            args.add(line);
        }
        Set<String> opened = new HashSet<>();
        while ((line = reader.readLine()) != null && !line.equals("===")) {
            opened.add(line);
        }
        Set<String> closed = new HashSet<>();
        while ((line = reader.readLine()) != null && !line.equals("===")) {
            closed.add(line);
        }
        Set<String> tasks = new HashSet<>();
        while ((line = reader.readLine()) != null && !line.equals("===")) {
            tasks.add(line);
        }
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null && !line.equals("===")) {
            sb.append(line).append("\n");
        }
        sb.delete(sb.length()-1, sb.length());
        reader.close();
        return new Commit(hash, args.get(0), args.get(1),
                args.get(2), args.get(3), args.get(4), sb.toString(), vcsDirectory, closed, opened, tasks);
    }
    public static Commit findCommit(String hash, Path vcsDirectory, Map<String, Commit> cache) throws Exception {
        if (cache.containsKey(hash)) {
            return cache.get(hash);
        }
        Commit output = findCommit(hash, vcsDirectory);
        cache.put(output.hash, output);
        return output;
    }

    /**
     * returns commit object representing the head commit of a branch
     * @param vcsDirectory: the Path to the .vcs directory
     * @param branch: the branch the returned head commit belongs to. If no branch is entered, use current branch
     * @return commit object
     */
    public static Commit getHeadCommit(Path vcsDirectory, String branch, Map<String, Commit> cache) throws Exception {
        vcsDirectory.resolve("Branches").toFile().setReadable(true, false);
        Path p = vcsDirectory.resolve("Branches").resolve(branch);
        p.toFile().setReadable(true, false);
        if (!Files.exists(p)) {
            throw new Exception(String.format("Branch \"%s\" does not exist", branch));
        }
        BufferedReader br = new BufferedReader(new FileReader(p.toFile()));
        return findCommit(br.readLine(), vcsDirectory, cache);
    }
    public static Commit getHeadCommit(Path vcsDirectory, Map<String, Commit> cache) throws Exception {
        String headBranch = Files.readAllLines(vcsDirectory.resolve("HEAD")).get(0);
        List<String> lastCommit = Files.readAllLines(Path.of(headBranch));
        if (lastCommit.size() == 0) {
            throw new Exception(String.format("Branch \"%s\" not formatted correctly", headBranch));
        } else {
            return findCommit(lastCommit.get(0), vcsDirectory, cache);
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
    public static Commit writeCommit(String user, String message, Path vcsDirectory, Commit current,
                                     Map<String, String> index, String branch, String[] closed,
                                     String[] opened, Set<String> tasks) throws Exception {
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
        tasks = new HashSet<>(tasks);
        for (String s : opened) {
            new File(vcsDirectory.resolve("Tasks").resolve(s).toString()).createNewFile();
            sb.append(s).append("\n");
        }
        File p;
        sb.append("===\n");
        for (String s : closed) {
            sb.append(s).append("\n");
            p = new File(vcsDirectory.resolve("Tasks").resolve(s).toString());
            if (p.exists()) {
                p.delete();
            }
        }
        sb.append("===\n");
        for (String s : tasks) {
            sb.append(s).append("\n");
            p = new File(vcsDirectory.resolve("Tasks").resolve(s).toString());
            if (!p.exists()) {
                p.createNewFile();
            }
        }
        sb.append("===\n").append(message).append("\n===");
        String hash = hash(sb.toString());
        createFile(sb.toString(), hash, vcsDirectory);
        return new Commit(hash, tree.hash, lastHash, time, user, branch, message,
                vcsDirectory, tree, new HashSet<>(Arrays.asList(closed)), new HashSet<>(Arrays.asList(opened)), tasks);
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
