import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class VCSUtils {
    public static Path findHash(String hash, Path vcsDirectory) {
        return vcsDirectory.resolve("Objects").resolve(hash.substring(0, 2)).resolve(hash.substring(2));
    }
    /**
     * Returns the SHA-1 hash for a file
     * @param path: path to the file
     * @return returns the string hash for the file
     */
    public static String hash(File path) {
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
    public static String hash(String input) {
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
            System.out.println("Hash failed due to:\n" + e.getMessage());
            return null;
        }
    }

    /**
     * Creates the bin of the first two characters of the hash if that doesn't exist, and copies the file to the bin
     * @param path: path to the file to be copied
     * @param hash: the hashcode of the file, for naming and bin assignment purposes
     * @return boolean whether the creation was successful or not
     */
    public static boolean createFile(File path, String hash, Path vcsDirectory) {
        if (hashExists(hash, vcsDirectory)) {
            return true;
        } else {
            Path source = path.toPath();
            Path target = findHash(hash, vcsDirectory);
            File bin = target.getParent().toFile();
            if (!bin.exists() && !bin.mkdir()) {
                return false;
            }
            try {
                Files.copy(source, target);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
    public static boolean createFile(String contents, String hash, Path vcsDirectory) {
        if (hashExists(hash, vcsDirectory)) {
            return true;
        } else {
            File target = findHash(hash, vcsDirectory).toFile();
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
     * Checks if a file with the hash as the name is already saved
     * @param hash: the hash
     * @return boolean if the file is already saved.
     */
    public static boolean hashExists(String hash, Path vcsDirectory) {
        return Files.exists(findHash(hash, vcsDirectory));
    }
}
