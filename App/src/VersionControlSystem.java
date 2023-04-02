import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class VersionControlSystem {
    private final Path currentDirectory;
    private final Path vcsDirectory;
    private final File head;
    private final File index;
    private final File AllCommits;
    private final Path objects;
    private final Path branches;
    private final String separator;
    private String branch;
    private Map<String, String> indexMap;
    private static final String[] subDirectories = {"Objects", "Branches"};
    private static final String[] files = {"HEAD", "Index", "AllCommits"};
    public VersionControlSystem(String currentDirectory) {
        this.currentDirectory = Paths.get(currentDirectory);
        this.separator = System.getProperty("file.separator");
        this.vcsDirectory = pathBuilder(new String[]{".vcs"}, this.currentDirectory);
        this.head = pathBuilder(new String[]{"HEAD"}, vcsDirectory).toFile();
        this.index = pathBuilder(new String[]{"Index"}, vcsDirectory).toFile();
        this.objects = pathBuilder(new String[]{"Objects"}, vcsDirectory);
        this.branches = pathBuilder(new String[]{"Branches"}, vcsDirectory);
        this.AllCommits = pathBuilder(new String[]{"AllCommits"}, vcsDirectory).toFile();
        this.branch = Objects.requireNonNull(getHeadPath()).getName();
    }
    public VersionControlSystem(String currentDirectory, String vcsDirectory, String head, String index, String objects, String AllCommits) {
        this.currentDirectory = Paths.get(currentDirectory);
        this.vcsDirectory = Paths.get(vcsDirectory);
        this.head = new File(head);
        this.index = new File(index);
        this.objects = Paths.get(objects);
        this.separator = System.getProperty("file.separator");
        this.branches = pathBuilder(new String[]{"Branches"}, this.vcsDirectory);
        this.AllCommits = new File(AllCommits);
        this.branch = Objects.requireNonNull(getHeadPath()).getName();
    }

    /**
     * The init command, creates a .vcs folder and subfolders to initialize version control system.
     * If a .vcs folder already exists, do nothing
     */
    public static VersionControlSystem init(String dir) {
        Path start = Paths.get(dir);
        File vcs = pathBuilder(new String[] {".vcs"}, start).toFile();
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
                        System.out.println("Failed to create " + pathBuilder(new String[] {subDirectory}, path));
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
                            System.out.println("Failed to create " + pathBuilder(new String[] {f}, path));
                            return null;
                        } else {
                            sub.put(f, file.toPath().toAbsolutePath().toString());
                        }
                    } catch (Exception e) {
                        vcs.delete();
                        System.out.println("Failed to create " + pathBuilder(new String[] {f}, path));
                        System.out.println(e.getMessage());
                        return null;
                    }
                }
                File file = new File(path.resolve("Branches").toFile(), "Master");
                try {
                    if (!file.createNewFile()) {
                        System.out.println("Failed to create " + file.getAbsolutePath());
                    } else {
                        FileWriter writer = new FileWriter(sub.get("HEAD"));
                        writer.write(path.resolve("Branches").resolve("Master").toString());
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
            String lastHash = lastCommitHash(name);
            if (!file.exists()) {
                if (lastHash != null) {
                    this.indexMap.put(name, String.format("%s %d", hash, 2));
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
                createFile(file, hash);
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
            File headPath = getHeadPath();
            if (headPath == null) {
                System.out.println("Unable to get head pointer");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(makeTree()).append("\n");
            List<String> path = Files.readAllLines(headPath.toPath());
            if (path.size() != 0) {
                sb.append(path.get(0));
            }
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"));
            sb.append("\n").append(time).append("\n").append(user).append("\n").append(this.branch).append("\n").append(message);
            String hash = hash(sb.toString());
            createFile(sb.toString(), hash);
            FileWriter fw = new FileWriter(getHeadPath(), false);
            fw.write(hash);
            fw.close();
            this.indexMap = null;
            fw = new FileWriter(this.index, false);
            fw.write("");
            fw.close();
            fw = new FileWriter(this.AllCommits, true);
            fw.write(hash + "\n");
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
            String lastHash = lastCommitHash(name);
            if (lastHash == null) {
                if (this.indexMap.containsKey(name)) {
                    this.indexMap.remove(name);
                } else {
                    System.out.println("No reason to remove the file");
                }
            } else {
                this.indexMap.put(name, String.format("%s %d", lastHash, 2));
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
            File lastCommit = lastCommit();
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
     * Returns the File of the last commit, based on what is stored in the head file
     * @return null if there is no last commit, the File of the last commit otherwise
     */
    private File lastCommit() {
        try {
            File headPath = getHeadPath();
            if (headPath == null) {
                return null;
            }
            List<String> path = Files.readAllLines(headPath.toPath());
            if (path.size() == 0) {
                return null;
            }
            return objects.resolve(path.get(0).substring(0, 2)).resolve(path.get(0).substring(2)).toFile();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the File of the tree in the last commit
     * @return null if there was no previous commit, the File object otherwise
     */
    private File lastTree() {
        try {
            File commit = lastCommit();
            if (commit == null) {
                return null;
            }
            String path = Files.readAllLines(Paths.get(commit.getAbsolutePath())).get(0);
            return objects.resolve(path.substring(0, 2)).resolve(path.substring(2)).toFile();
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
     * Returns the hash of the file at the last commit
     * @param path: path to the file
     * @return string of the hash of the file in the last commit, or null if the file isn't in the last commit
     */
    private String lastCommitHash(String path) {
        try {
            File lt = lastTree();
            if (lt != null) {
                BufferedReader br = new BufferedReader(new FileReader(lt));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith(path)) {
                        return line.substring(path.length()+1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Creates a tree file, which is the contents of the last tree but with changes based on the files staged
     * @return hash of the new tree file
     */
    public String makeTree() {
        try {
            readIndex();
            StringBuilder sb = new StringBuilder();
            File tree = lastTree();
            if (tree == null) {
                for (String path : this.indexMap.keySet()) {
                    sb.append(String.format("%s %s\n", path, this.indexMap.get(path).substring(0, this.indexMap.get(path).length()-2)));
                }
            } else {
                BufferedReader br = new BufferedReader(new FileReader(tree));
                String line;
                String path;
                while ((line = br.readLine()) != null) {
                    path = line.split(" ")[0];
                    if (this.indexMap.containsKey(path)) {
                        if (!this.indexMap.get(path).substring(this.indexMap.get(path).length() - 1).equals("2")) {
                            sb.append(String.format("%s %s\n", path, this.indexMap.get(path).substring(0, this.indexMap.get(path).length()-2)));
                        }
                    } else {
                        sb.append(line).append("\n");
                    }
                }
            }
            String hash = hash(sb.toString());
            createFile(sb.toString(), hash);
            return hash;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a path to a file or directory given how to get there from currentDirectory
     * @param paths: how to get to the desired destination, in the form of an array of strings
     * @return the desired path
     */
    private static Path pathBuilder(String[] paths, Path start) {
        Path output = start;
        for (String path : paths) {
            output = output.resolve(path);
        }
        return output;
    }

    /**
     * Returns the SHA-1 hash for a file
     * @param path: path to the file
     * @return returns the string hash for the file
     */
    public String hash(File path) {
        try (FileInputStream fis = new FileInputStream(path)) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] dataBytes = new byte[1024];
            int bytesRead;

            while ((bytesRead = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, bytesRead);
            }
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte hashByte : hashBytes) {
                sb.append(Integer.toString((hashByte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (Exception e) {
            System.out.println("Hash failed for " + path + "due to:\n" + e.getMessage());
            return null;
        }
    }
    public String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(input.getBytes());
            byte[] hashBytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if a file with the hash as the name is already saved
     * @param hash: the hash
     * @return boolean if the file is already saved.
     */
    public boolean hashExists(String hash) {
        return Files.exists(getHashedFile(hash).toPath());
    }

    /**
     * Creates the bin of the first two characters of the hash if that doesn't exist, and copies the file to the bin
     * @param path: path to the file to be copied
     * @param hash: the hashcode of the file, for naming and bin assignment purposes
     * @return boolean whether the creation was successful or not
     */
    public boolean createFile(File path, String hash) {
        if (hashExists(hash)) {
            return true;
        } else {
            Path source = path.toPath();
            Path target = getHashedFile(hash).toPath();
            File bin = target.getParent().toFile();
            if (!bin.exists() && !bin.mkdir()) {
                return false;
            }
            try {
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
    public boolean createFile(String contents, String hash) {
        if (hashExists(hash)) {
            return true;
        } else {
            File target = getHashedFile(hash);
            File bin = target.getParentFile();
            if (!bin.exists() && !bin.mkdir()) {
                return false;
            }
            try {
                FileWriter writer = new FileWriter(target);
                writer.write(contents);
                writer.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Returns the file for the hash
     * @param hash: hash of the file
     * @return file object
     */
    public File getHashedFile(String hash) {
        return pathBuilder(new String[] {subDirectories[0], hash.substring(0, 2), hash.substring(2)}, vcsDirectory).toFile();
    }

    /**
     * Adds the formatted commit logs to the string builder and returns the hash of the next commit
     * @param hash: hash of the commit
     * @param sb: string builder object
     * @return hash of the next commit
     */
    private String printCommit(String hash, StringBuilder sb, boolean global) {
        try {
            List<String> path = Files.readAllLines(getHashedFile(hash).toPath());
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

