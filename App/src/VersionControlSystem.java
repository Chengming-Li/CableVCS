import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Stream;

public class VersionControlSystem extends VCSUtils {
    private final Path currentDirectory;
    private final Path vcsDirectory;
    private final File head;
    private final File index;
    private Commit lastCommit;
    private final File AllCommits;
    private final Path branches;
    private String branch;
    private Map<String, String> indexMap;
    private static final String[] SUBDIRECTORIES = {"Objects", "Branches", "Tasks", "Tasks\\Opened", "Tasks\\Closed"};
    private static final String[] FILES = {"HEAD", "Index", "AllCommits"};
    private static final int LENGTHOFHASHANDSTATUS = 43;
    private final Map<String, Commit> commitCache;
    private Set<Path> branchSet;
    private Task task;
    public VersionControlSystem(String currentDirectory) throws Exception {
        this.currentDirectory = Paths.get(currentDirectory);
        this.vcsDirectory = this.currentDirectory.resolve(".vcs");
        this.head = this.vcsDirectory.resolve("HEAD").toFile();
        this.index = this.vcsDirectory.resolve("Index").toFile();
        this.branches = this.vcsDirectory.resolve("Branches");
        this.AllCommits = this.vcsDirectory.resolve("AllCommits").toFile();
        this.commitCache = new HashMap<>();
        this.lastCommit = Commit.getHeadCommit(this.vcsDirectory);
        assert lastCommit != null;
        this.commitCache.put(lastCommit.hash, lastCommit);
        this.branch = lastCommit.branch;
        this.task = new Task(this.vcsDirectory);
    }
    public VersionControlSystem(String currentDirectory, String vcsDirectory, String head, String index,
                                String AllCommits) throws Exception {
        this.currentDirectory = Paths.get(currentDirectory);
        this.vcsDirectory = Paths.get(vcsDirectory);
        this.head = new File(head);
        this.index = new File(index);
        this.branches = this.vcsDirectory.resolve("Branches");
        this.AllCommits = new File(AllCommits);
        this.commitCache = new HashMap<>();
        this.lastCommit = Commit.getHeadCommit(this.vcsDirectory);
        assert lastCommit != null;
        this.commitCache.put(lastCommit.hash, lastCommit);
        this.branch = lastCommit.branch;
        this.task = new Task(this.vcsDirectory);
    }

    /**
     * The init command, creates a .vcs folder and subfolders to initialize version control system.
     * If a .vcs folder already exists, do nothing
     */
    public static VersionControlSystem init(String dir) throws Exception {
        Path start = Paths.get(dir);
        File vcs = start.resolve(".vcs").toFile();
        if (!Files.exists(start)) {
            System.out.println("Directory doesn't exist");
        } else if (vcs.exists()) {
            System.out.println("Version Control System already exists");
        } else {
            if (vcs.mkdir()) {
                Map<String, String> sub = new HashMap<>();
                Path path = vcs.toPath();
                Files.setAttribute(path, "dos:hidden", true);
                for (String subDirectory : SUBDIRECTORIES) {
                    File subfolder = new File(path.toFile(), subDirectory);
                    if (!subfolder.mkdir()) {
                        vcs.delete();
                        System.out.println("Failed to create " + path.resolve(subDirectory));
                        return null;
                    } else {
                        sub.put(subDirectory, subfolder.toPath().toAbsolutePath().toString());
                    }
                }
                for (String f : FILES) {
                    File file = new File(path.toFile(), f);
                    if (!file.createNewFile()) {
                        vcs.delete();
                        System.out.println("Failed to create " + path.resolve(f));
                        return null;
                    } else {
                        sub.put(f, file.toPath().toAbsolutePath().toString());
                    }
                }
                Commit c = Commit.writeInitialCommit(path);
                FileWriter writer = new FileWriter(path.resolve("Branches").resolve("master").toFile());
                writer.write(c.hash);
                writer.close();
                writer = new FileWriter(sub.get("HEAD"));
                writer.write(path.resolve("Branches").resolve("master").toString());
                writer.close();
                return new VersionControlSystem(dir, path.toString(), sub.get("HEAD"),
                        sub.get("Index"), sub.get("AllCommits"));
            } else {
                System.out.println("Unable to create " + vcs.toPath());
            }
        }
        return null;
    }

    /**
     * Stages the changes made to a file and updates the index file accordingly
     * @param path: the path to the file to be added
     */
    public void add(String path) throws Exception {
        readIndex();
        File file = new File(path);
        String name = this.currentDirectory.relativize(file.toPath()).toString();
        String hash = hash(file);
        String lastHash = null;
        if (lastCommit != null) {
            lastHash = lastCommit.getTree().map.getOrDefault(name, null);
        }
        if (!file.exists()) {
            if (lastHash != null) {
                this.indexMap.put(name, "________________________________________ 2");
            } else {
                System.out.println(name + " does not exist");
                return;
            }
        } else {
            if (lastHash == null) {
                this.indexMap.put(name, String.format("%s %d", hash, 1));
            } else if (lastHash.equals(hash)) {
                this.indexMap.remove(name);
            } else {
                this.indexMap.put(name, String.format("%s %d", hash, 0));
            }
            createFile(file, hash, vcsDirectory);
        }
        StringBuilder sb = new StringBuilder();
        for (String key : this.indexMap.keySet()) {
            sb.append(String.format("%s %s\n", key, indexMap.get(key)));
        }
        FileWriter writer = new FileWriter(index);
        writer.write(sb.toString());
        writer.close();
    }

    /**
     * Creates a commit, which includes the following lines:
     *      *  [tree]
     *      *  [previous commit]
     *      *  [time]
     *      *  [author]
     *      *  ===
     *      *  [open commits]
     *      *  ===
     *      *  [closed commits]
     *      *  ===
     *      *  [message]
     * @param message: string, message for the commit
     * @param user: string, the author of the commit
     * @return true if commit was a success, false if otherwise
     */
    public void commit(String message, String user) throws Exception {
        commit(message, user, new String[0], new String[0][0]);
    }

    /**
     * Creates a commit, which includes the following lines:
     *      *  [tree]
     *      *  [previous commit]
     *      *  [time]
     *      *  [author]
     *      *  [branch]
     *      *  ===
     *      *  [closed task]
     *      *  ===
     *      *  [opened tasks]
     *      *  ===
     *      *  [message]
     * Also opens and closes tasks
     * @param message: string, message for the commit
     * @param user: string, the author of the commit
     * @param closeTasks: array of strings corresponding to task name
     * @param openTasks: array of arrays of strings, the first element is the task name, second is task description
     */
    public void commit(String message, String user, String[] closeTasks, String[][] openTasks) throws Exception {
        if (this.indexMap == null || this.indexMap.keySet().size() == 0) {
            throw new Exception("No changes added to commit");
        } else if (message.length() == 0) {
            throw new Exception("Please enter a commit message");
        }
        for (String task : closeTasks) {
            // do smth
        }
        for (String[] task : openTasks) {
            // do smth
        }
        readIndex();
        this.lastCommit = Commit.writeCommit(user, message, vcsDirectory, lastCommit, indexMap, this.branch, closeTasks, openTasks);
        commitCache.put(lastCommit.hash, lastCommit);
        FileWriter fw = new FileWriter(Objects.requireNonNull(getHeadPath()), false);
        fw.write(lastCommit.hash);
        fw.close();
        this.indexMap = null;
        fw = new FileWriter(this.index, false);
        fw.write("");
        fw.close();
        fw = new FileWriter(this.AllCommits, true);
        fw.write(lastCommit.hash + "\n");
        fw.close();
    }

    /**
     * Unstages the file if the file is untracked, otherwise mark the file to be removed,
     * and delete the file if it hasn't been deleted already.
     * @param path: path to the file
     */
    public void remove(String path) throws Exception {
        readIndex();
        File file = new File(path);
        String name = this.currentDirectory.relativize(file.toPath()).toString();
        if (lastCommit == null || lastCommit.getTree().map.getOrDefault(name, null) == null) {
            if (this.indexMap.containsKey(name)) {
                this.indexMap.remove(name);
            } else {
                throw new Exception("No reason to remove file");
            }
        } else {
            this.indexMap.put(name, String.format("________________________________________ %d", 2));
            if (file.exists()) {
                file.delete();
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String key : this.indexMap.keySet()) {
            sb.append(String.format("%s %s\n", key, indexMap.get(key)));
        }
        FileWriter writer = new FileWriter(index);
        writer.write(sb.toString());
        writer.close();
    }

    /**
     * returns the current commit and all its ancestors
     * ===
     * commit [hash]
     * Date: [MM/DD/YYYY] [hh/dd/ss]
     * Author: [author]
     * [message]
     * @return a string of all the commits
     */
    public String log() throws Exception {
        StringBuilder sb = new StringBuilder();
        Commit c = lastCommit;
        while (c != null) {
            if (!commitCache.containsKey(c.hash)) {
                commitCache.put(c.hash, c);
            }
            sb.append(c.toString(false));
            c = c.parentCommit(this.commitCache);
        }
        return sb.toString();
    }

    /**
     * returns the all commits
     * ===
     * commit [hash]
     * Date: [MM/DD/YYYY] [hh/dd/ss]
     * Author: [author]
     * Branch: [branch]
     * [message]
     * @return a string of all the commits
     */
    public String globalLog() throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(AllCommits));
        String line;
        while ((line = reader.readLine()) != null) {
            if (commitCache.containsKey(line)) {
                sb.append(commitCache.get(line).toString(true));
            } else {
                sb.append(Objects.requireNonNull(Commit.findCommit(line, vcsDirectory, commitCache)).
                        toString(true));
            }
        }
        return sb.toString();
    }

    /**
     * Checks all files in the directory, and returns it formatted by status of file
     * === Branches ===
     * [branches]
     * === Staged Files ===
     * [files that have been modified and staged]
     * === Removed Files ===
     * [files that have been removed and staged]
     * === Modified Files ===
     * [modified files that are unstaged, followed by (modified) or (deleted)]
     * === Untracked Files ===
     * [untracked files]
     * @param sets: an array of 5 Sets, representing the branches, staged files, modified files, untracked files, and removed files
     *            If no array is entered, will call statusHelper() and enter the returned array
     * @return string representing the status
     */
    public String status(Set<String>[] sets) throws Exception {
        StringBuilder sb = new StringBuilder();
        Set<String> branches = sets[0];
        Set<String> staged = sets[1];
        Set<String> modified = sets[2];
        Set<String> untracked = sets[3];
        Set<String> removed = sets[4];
        for (String branch : branches) {
            if (branch.equals(this.branch)) {
                sb.insert(0, "*"+branch+"\n");
            } else {
                sb.append(branch).append("\n");
            }
        }
        sb.insert(0, "=== Branches ===\n");
        if (!staged.isEmpty()) {
            sb.append("=== Staged Files ===\n");
            for (String f : staged) {
                sb.append(f).append("\n");
            }
        }
        if (!removed.isEmpty()) {
            sb.append("=== Removed Files ===\n");
            for (String f : removed) {
                sb.append(f).append("\n");
            }
        }
        if (!modified.isEmpty()) {
            sb.append("=== Modified Files ===\n");
            for (String f : modified) {
                sb.append(f).append("\n");
            }
        }
        if (!untracked.isEmpty()) {
            sb.append("=== Untracked Files ===\n");
            for (String f : untracked) {
                sb.append(f).append("\n");
            }
        }
        return sb.toString();
    }
    public String status() throws Exception {
        return status(statusHelper());
    }

    /**
     * If branch == false:
     *      Checks to make sure the last commit tracked the file path given by input,
     *      then overwrites the file's contents with the file contents in
     *      the last commit, creating the file if necessary
     * if branch == true:
     *      Deletes all the files tracked in current commit but not
     *      tracked in the head commit of the branch, given by input
     *      Overwrites each file tracked in the head commit of the branch, creating files if necessary
     *      Switches HEAD to point at branch
     * @param input: file path or branch name
     * @param branch: true if input is a branch name, false if input is file path
     */
    public void checkout(String input, boolean branch) throws Exception {
        if (!branch) {
            Path p = Path.of(input);
            Path shortP = this.currentDirectory.relativize(p);
            if (this.lastCommit == null) {
                throw new Exception("There was no last commit");
            }
            String hash = this.lastCommit.getTree().map.getOrDefault(shortP.toString(), null);
            if (hash == null) {
                throw new Exception("File does not exist in that commit");
            }
            if (shortP.getParent() != null && shortP.getParent().toFile().exists()) {
                shortP.getParent().toFile().mkdirs();
            }
            Files.copy(findHash(hash, vcsDirectory), p, StandardCopyOption.REPLACE_EXISTING);
        } else {
            // check to see if branch exists
            Commit c = Commit.getHeadCommit(vcsDirectory, input);
            if (performCheckout(c)) {
                this.branch = input;
                FileWriter writer = new FileWriter(this.head);
                writer.write(this.branches.resolve(input).toString());
                writer.close();
                this.lastCommit = c;
            }
        }
    }

    /**
     * Overwrites the contents of file at the end of path with its contents in commit with id == commitID
     * @param commitId: ID/Hash of a commit
     * @param path: the path to a file
     */
    public void checkout(String commitId, String path) throws Exception {
        Path p = Path.of(path);
        Path shortP = this.currentDirectory.relativize(p);
        Commit c = Commit.findCommit(commitId, vcsDirectory, commitCache);
        if (c == null) {
            throw new Exception("No commit with that id exists");
        }
        String hash = c.getTree().map.getOrDefault(shortP.toString(), null);
        if (hash == null) {
            throw new Exception("File does not exist in that commit");
        }
        if (shortP.getParent() != null && shortP.getParent().toFile().exists()) {
            shortP.getParent().toFile().mkdirs();
        }
        Files.copy(findHash(hash, vcsDirectory), p, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Creates a new branch, which points to the last commit
     * @param branchName: name of the new branch
     */
    public void branch(String branchName) throws Exception {
        Path branch = branches.resolve(branchName);
        if (Objects.requireNonNull(getBranches()).contains(branch)) {
            throw new Exception("Branch with that name already exists");
        }
        FileWriter writer = new FileWriter(branch.toFile());
        writer.write(lastCommit.hash);
        writer.close();
        this.branchSet.add(branch);
    }

    /**
     * Removes the branch pointer in the Branches folder
     * @param branchName: name of branch to be removed
     */
    public void removeBranch(String branchName) throws Exception {
        Path branch = branches.resolve(branchName);
        if (this.branch.equals(branchName)) {
            throw new Exception("Cannot remove the current branch");
        }
        else if (!Objects.requireNonNull(getBranches()).contains(branch)) {
            throw new Exception("A branch with that name does not exist");
        }
        branch.toFile().delete();
    }

    /**
     * Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch's head to that commit node.
     * @param commitID: hash of commit to be reset to. If no commitID is entered, it'll use the last commit
     */
    public void reset(String commitID) throws Exception {
        Commit c = Commit.findCommit(commitID, vcsDirectory, commitCache);
        if (performCheckout(c)) {
            FileWriter writer = new FileWriter(this.branches.resolve(branch).toFile());
            writer.write(c.hash);
            writer.close();
        }
    }
    public void reset() throws Exception {
        Commit c = lastCommit;
        if (performCheckout(c)) {
            FileWriter writer = new FileWriter(this.branches.resolve(branch).toFile());
            writer.write(c.hash);
            writer.close();
        }
    }

    /**
     * checks to make sure none of the failure cases of checkout occurs
     * @param c: commit object of the commit to check out to
     * @param m: map, which is last commit's tree
     * @return null if failure, map of the target commit if success
     */
    private Map<String, String> checkoutCheck(Commit c, Map<String, String> m) throws Exception {
        Map<String, String> map;
        if (c == null) {
            throw new Exception("No such branch exists");
        } else {
            map = c.getTree().map;
            String name;
            Set<String> names = new HashSet<>();
            for (Path path : Objects.requireNonNull(getWorkingDir())) {
                name = this.currentDirectory.relativize(path).toString();
                if (map.containsKey(name) && !m.containsKey(name)) {
                    names.add(name);
                }
            }
            if (names.size() > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("There are untracked files in the way; delete it or add it first.\n");
                for (String n : names) {
                    sb.append(n).append("\n");
                }
                throw new Exception(sb.toString());
            }
        }
        return map;
    }

    /**
     * performs the check out operations, see checkout()
     * @param c: object representing target commit
     * @return false if failure, true otherwise
     */
    private boolean performCheckout(Commit c) throws Exception {
        Map<String, String> m = this.lastCommit.getTree().map;
        Map<String, String> headMap = checkoutCheck(c, m);
        if (headMap == null) {
            return false;
        }
        for (String name : m.keySet()) {
            if (!headMap.containsKey(name)) {
                this.currentDirectory.resolve(name).toFile().delete();
            }
        }
        Path shortP;
        for (String name : headMap.keySet()) {
            shortP = Path.of(name);
            if (shortP.getParent() != null && !shortP.getParent().toFile().exists()) {
                shortP.getParent().toFile().mkdirs();
            }
            Files.copy(findHash(headMap.get(name), vcsDirectory),
                    this.currentDirectory.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
        return true;
    }

    /**
     * Returns the file of the head branch pointer
     * @return file object pointing to head commit
     */
    private File getHeadPath() throws Exception {
        return new File(Files.readAllLines((this.head.toPath())).get(0));
    }

    /**
     * Reads the contents of Index file and converts it into a hashmap
     * key: the relative path to the file
     * value: [hash] [state]
     */
    private void readIndex() throws Exception {
        if (this.indexMap != null) {
            return;
        }
        this.indexMap = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(index));
        String line;
        while ((line = br.readLine()) != null) {
            this.indexMap.put(line.substring(0, line.length() - LENGTHOFHASHANDSTATUS),
                    line.substring(line.length() - LENGTHOFHASHANDSTATUS));
        }
    }

    /**
     * Returns a Set of paths to each branch file
     * @return set of paths
     */
    private Set<Path> getBranches() throws Exception {
        if (this.branchSet != null) {
            return branchSet;
        }
        try (Stream<Path> walk = Files.walk(branches).filter(path -> !Files.isDirectory(path))) {
            this.branchSet = new HashSet<>(walk.toList());
            return branchSet;
        }
    }

    /**
     * Returns a Set of the paths of all files in the working directory, excluding the .vcs subdirectory
     * @return set of paths
     */
    private Set<Path> getWorkingDir() throws Exception {
        try (Stream<Path> walk = Files.walk(currentDirectory).filter(path -> !Files.isDirectory(path)).
                filter(path -> !path.startsWith(vcsDirectory))) {
            return new HashSet<>(walk.toList());
        }
    }

    /**
     * Returns a commit object representing the last commit
     * @return commit object
     */
    public Commit getLastCommit() {
        return lastCommit;
    }

    /**
     * Returns an array of string sets
     * Array:
     *  [0]: branches
     *  [1]: staged files
     *  [2]: modified files(to get the name, exclude the last 11 characters)
     *  [3]: untracked files
     *  [4]: removed files
     * @return an array of sets
     */
    public Set<String>[] statusHelper() throws Exception {
        Set<String> branches = new HashSet<>();
        for (Path path : Objects.requireNonNull(getBranches())) {
            branches.add(path.getFileName().toString());
        }
        readIndex();
        String p;
        Set<String> staged = new HashSet<>();
        Set<String> modified = new HashSet<>();
        Set<String> untracked = new HashSet<>();
        Set<String> removed = new HashSet<>();
        Set<String> indexFiles = new HashSet<>(indexMap.keySet());
        Set<String> commitFiles = new HashSet<>(lastCommit.getTree().map.keySet());
        String line;
        for (Path path : Objects.requireNonNull(getWorkingDir())) {
            p = this.currentDirectory.relativize(path).toString();
            if (indexFiles.contains(p)) {
                line = indexMap.get(p);
                if (line.endsWith("2")) {
                    untracked.add(p);
                } else if (line.startsWith(Objects.requireNonNull(hash(path.toFile())))) {
                    staged.add(p);
                } else {
                    modified.add(p + " (modified)");
                }
            } else if (commitFiles.contains(p) && !lastCommit.getTree().map.get(p).equals(hash(path.toFile()))) {
                modified.add(p + " (modified)");
            } else if (!commitFiles.contains(p)) {
                untracked.add(p);
            }
            indexFiles.remove(p);
            commitFiles.remove(p);
        }
        for (String i : indexFiles) {
            if (indexMap.get(i).endsWith("2")) {
                removed.add(i);
            } else {
                modified.add(i + " (deleted) ");
            }
            commitFiles.remove(i);
        }
        for (String i : commitFiles) {
            modified.add(i + " (deleted) ");
        }
        Set<String>[] setArray = new HashSet[5];
        setArray[0] = branches;
        setArray[1] = staged;
        setArray[2] = modified;
        setArray[3] = untracked;
        setArray[4] = removed;
        return setArray;
    }
}
