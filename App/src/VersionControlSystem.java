import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class VersionControlSystem extends VCSUtils {
    private final Path currentDirectory;
    private final Path vcsDirectory;
    private final File head;
    private final File index;
    private Commit lastCommit;
    private final File AllCommits;
    private final Path objects;
    private final Path branches;
    private String branch;
    private Map<String, String> indexMap;
    private static final String[] subDirectories = {"Objects", "Branches"};
    private static final String[] files = {"HEAD", "Index", "AllCommits"};
    public VersionControlSystem(String currentDirectory) {
        this.currentDirectory = Paths.get(currentDirectory);
        this.vcsDirectory = this.currentDirectory.resolve(".vcs");
        this.head = this.vcsDirectory.resolve("HEAD").toFile();
        this.index = this.vcsDirectory.resolve("Index").toFile();
        this.objects = this.vcsDirectory.resolve("Objects");
        this.branches = this.vcsDirectory.resolve("Branches");
        this.AllCommits = this.vcsDirectory.resolve("AllCommits").toFile();;
        this.branch = Objects.requireNonNull(getHeadPath()).getName();
        this.lastCommit = Commit.getHeadCommit(this.vcsDirectory);
    }
    public VersionControlSystem(String currentDirectory, String vcsDirectory, String head, String index, String objects, String AllCommits) {
        this.currentDirectory = Paths.get(currentDirectory);
        this.vcsDirectory = Paths.get(vcsDirectory);
        this.head = new File(head);
        this.index = new File(index);
        this.objects = Paths.get(objects);
        this.branches = this.vcsDirectory.resolve("Branches");
        this.AllCommits = new File(AllCommits);
        this.branch = Objects.requireNonNull(getHeadPath()).getName();
        this.lastCommit = Commit.getHeadCommit(this.vcsDirectory);
    }

    /**
     * The init command, creates a .vcs folder and subfolders to initialize version control system.
     * If a .vcs folder already exists, do nothing
     */
    public static VersionControlSystem init(String dir) {
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
                try {
                    Files.setAttribute(path, "dos:hidden", true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (String subDirectory : subDirectories) {
                    File subfolder = new File(path.toFile(), subDirectory);
                    if (!subfolder.mkdir()) {
                        vcs.delete();
                        System.out.println("Failed to create " + path.resolve(subDirectory));
                        return null;
                    } else {
                        sub.put(subDirectory, subfolder.toPath().toAbsolutePath().toString());
                    }
                }
                for (String f : files) {
                    File file = new File(path.toFile(), f);
                    try {
                        if (!file.createNewFile()) {
                            vcs.delete();
                            System.out.println("Failed to create " + path.resolve(f));
                            return null;
                        } else {
                            sub.put(f, file.toPath().toAbsolutePath().toString());
                        }
                    } catch (Exception e) {
                        vcs.delete();
                        System.out.println("Failed to create " + path.resolve(f));
                        System.out.println(e.getMessage());
                        return null;
                    }
                }
                File file = new File(path.resolve("Branches").toFile(), "master");
                try {
                    if (!file.createNewFile()) {
                        System.out.println("Failed to create " + file.getAbsolutePath());
                    } else {
                        FileWriter writer = new FileWriter(sub.get("HEAD"));
                        writer.write(path.resolve("Branches").resolve("master").toString());
                        writer.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new VersionControlSystem(dir, path.toString(), sub.get("HEAD"), sub.get("Index"), sub.get("Objects"), sub.get("AllCommits"));
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
    public void add(String path) {
        try {
            readIndex();
            File file = new File(path);
            String name = this.currentDirectory.relativize(file.toPath()).toString();
            String hash = hash(file);
            String lastHash = lastCommit.getTree().map.getOrDefault(name, null);
            if (!file.exists()) {
                if (lastHash != null) {
                    this.indexMap.put(name, String.format("________________________________________ %d", hash, 2));
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a commit, which includes 5 lines:
     *  [tree]
     *  [previous commit]
     *  [time]
     *  [author]
     *  [message]
     * @param message: string, message for the commit
     * @param user: string, the author of the commit
     */
    public void commit(String message, String user) {
        if (this.indexMap == null || this.indexMap.keySet().size() == 0) {
            System.out.println("No changes added to the commit");
            return;
        } else if (message.length() == 0) {
            System.out.println("Please enter a commit message.");
            return;
        }
        try {
            this.lastCommit = Commit.writeCommit(user, message, vcsDirectory, lastCommit);
            FileWriter fw = new FileWriter(getHeadPath(), false);
            fw.write(lastCommit.hash);
            fw.close();
            this.indexMap = null;
            fw = new FileWriter(this.index, false);
            fw.write("");
            fw.close();
            fw = new FileWriter(this.AllCommits, true);
            fw.write(lastCommit.hash + "\n");
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Unstages the file if the file is untracked, otherwise mark the file to be removed, and delete the file if it hasn't been deleted already.
     * @param path: path to the file
     */
    public void remove(String path) {
        try {
            readIndex();
            File file = new File(path);
            String name = this.currentDirectory.relativize(file.toPath()).toString();
            if (lastCommit.getTree().map.getOrDefault(name, null) == null) {
                if (this.indexMap.containsKey(name)) {
                    this.indexMap.remove(name);
                } else {
                    System.out.println("No reason to remove the file");
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
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(index))) {
                bw.write(sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public String log() {
        try {
            File headPath = getHeadPath();
            if (lastCommit == null || headPath == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            String hash = Files.readAllLines(headPath.toPath()).get(0);
            while (hash.length() != 0) {
                hash = printCommit(hash, sb, false);
            }
            return sb.toString();
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    public String globalLog() {
        try {
            StringBuilder sb = new StringBuilder();
            List<String> hashes = Files.readAllLines(this.AllCommits.toPath());
            for (int i = hashes.size()-1; i >= 0; i--) {
                printCommit(hashes.get(i), sb, true);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String status() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Branches ===\n");
        try (Stream<Path> walk = Files.walk(branches).filter(path -> !Files.isDirectory(path))) {
            for (Path path : walk.toList()) {
                if (path.getFileName().toString().equals(branch)) {
                    sb.append("*").append(path.getFileName()).append("\n");
                } else {
                    sb.append(path.getFileName()).append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        readIndex();
        try (Stream<Path> walk = Files.walk(currentDirectory).filter(path -> !Files.isDirectory(path)).filter(path -> !path.startsWith(vcsDirectory))){
            String p;
            Set<String> staged = new HashSet<>();
            Set<String> modified = new HashSet<>();
            Set<String> untracked = new HashSet<>();
            Set<String> removed = new HashSet<>();
            Set<String> indexFiles = new HashSet<>(indexMap.keySet());
            Set<String> commitFiles = new HashSet<>(lastCommit.getTree().map.keySet());
            String line;
            for (Path path : walk.toList()) {
                p = this.currentDirectory.relativize(path).toString();
                if (indexFiles.contains(p)) {
                    line = indexMap.get(p);
                    if (line.endsWith("2")) {
                        untracked.add(p);
                    } else if (line.startsWith(hash(path.toFile()))){
                        staged.add(p);
                    } else {
                        modified.add(p + " (modified)");
                    }
                } else if (commitFiles.contains(p) && !lastCommit.getTree().map.get(p).equals(hash(path.toFile()))) {
                    modified.add(p + " (modified)");
                } else if (!commitFiles.contains(p)){
                    untracked.add(p);
                }
                indexFiles.remove(p);
                commitFiles.remove(p);
            }
            for (String i : indexFiles) {
                if (indexMap.get(i).endsWith("2")) {
                    removed.add(i);
                } else {
                    modified.add(i + " (deleted)");
                }
                commitFiles.remove(i);
            }
            for (String i : commitFiles) {
                modified.add(i + " (deleted)");
            }
            if (!staged.isEmpty()) {
                sb.append("\n=== Staged Files ===\n");
                for (String f : staged) {
                    sb.append(f).append("\n");
                }
            }
            if (!removed.isEmpty()) {
                sb.append("\n=== Removed Files ===\n");
                for (String f : removed) {
                    sb.append(f).append("\n");
                }
            }
            if (!modified.isEmpty()) {
                sb.append("\n=== Modified Files ===\n");
                for (String f : modified) {
                    sb.append(f).append("\n");
                }
            }
            if (!untracked.isEmpty()) {
                sb.append("\n=== Untracked Files ===\n");
                for (String f : untracked) {
                    sb.append(f).append("\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public void checkout(String path, boolean branch) {
        // only if it's a branch:
    }

    /**
     * Returns the file of the head branch pointer
     * @return file object pointing to head commit
     */
    private File getHeadPath() {
        try {
            return new File(Files.readAllLines((this.head.toPath())).get(0));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads the contents of Index file and converts it into a hashmap
     * key: the relative path to the file
     * value: [hash] [state]
     */
    private void readIndex() {
        if (this.indexMap != null) {
            return;
        }
        try {
            this.indexMap = new HashMap<>();
            BufferedReader br = new BufferedReader(new FileReader(index));
            String line;
            while ((line = br.readLine()) != null) {
                this.indexMap.put(line.substring(0, line.length()-43), line.substring(line.length()-43));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a tree file, which is the contents of the last tree but with changes based on the files staged
     * @return hash of the new tree file
     */
    public String makeTree() {
        try {
            readIndex();
            StringBuilder sb = new StringBuilder();
            if (lastCommit == null) {
                for (String path : this.indexMap.keySet()) {
                    sb.append(String.format("%s %s\n", path, this.indexMap.get(path).substring(0, this.indexMap.get(path).length()-2)));
                }
            } else {
                Map<String, String> map = new HashMap<>(lastCommit.getTree().map);
                for (String key : indexMap.keySet()) {
                    if (indexMap.get(key).endsWith("2")) {
                        map.remove(key);
                    } else {
                        map.put(key, indexMap.get(key).substring(0, indexMap.get(key).length()-2));
                    }
                }
                for (String key : map.keySet()) {
                    sb.append(String.format("%s %s\n", key, map.get(key)));
                }
            }
            String hash = hash(sb.toString());
            createFile(sb.toString(), hash, vcsDirectory);
            return hash;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Adds the formatted commit logs to the string builder and returns the hash of the next commit
     * @param hash: hash of the commit
     * @param sb: string builder object
     * @return hash of the next commit
     */
    private String printCommit(String hash, StringBuilder sb, boolean global) {
        try {
            List<String> path = Files.readAllLines(findHash(hash, vcsDirectory));
            sb.append(String.format("===\ncommit %s\nDate: %s\nAuthor: %s\n", hash, path.get(2), path.get(3)));
            if (global){
                sb.append("Branch: ").append(path.get(4)).append("\n");
            }
            sb.append(path.get(5)).append("\n");
            return path.get(1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
