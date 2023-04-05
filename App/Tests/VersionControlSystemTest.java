import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.util.List;

class VersionControlSystemTest {
    private static final String TESTDIR = "C:\\Users\\malic\\Downloads\\Project\\VersionControlSystem\\Test";
    private static final String VCSDIR = TESTDIR + "\\.vcs";

    @org.junit.jupiter.api.Test
    void init() {
        VersionControlSystem vcs = cleanUp();
        assertTrue(Files.exists(Paths.get(VCSDIR)));
    }
    @org.junit.jupiter.api.Test
    void addTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer = new FileWriter(TESTDIR +"\\testText.txt");
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(TESTDIR +"\\testText.txt");
            BufferedReader br = new BufferedReader(new FileReader(VCSDIR +"\\Index"));
            String firstLine = String.format("%s %s 1", "testText.txt", vcs.hash(new File(TESTDIR +"\\testText.txt")));
            assertEquals(br.readLine(), firstLine);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void commitTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer;
            for (int i = 0; i < 5; i++) {
                writer = new FileWriter(TESTDIR +"\\testText" + i +".txt");
                writer.write("This is some nice text, yada" + i);
                writer.close();
                vcs.add(TESTDIR +"\\testText" + i +".txt");
            }
            Path indexPath = Paths.get(VCSDIR + "\\Index");
            Path headPath = Paths.get(VCSDIR + "\\Branches\\Master");
            assertEquals(5, Files.readAllLines(indexPath).size());
            vcs.commit("Test Commit", "User");
            assertEquals(0, Files.readAllLines(indexPath).size());
            assertEquals(Files.readAllLines(headPath).size(), 1);
            Path of = Path.of(VCSDIR);
            File firstCommit = VCSUtils.findHash(Files.readAllLines(headPath).get(0), of).toFile();
            String firstCommitHash = Files.readAllLines(headPath).get(0);
            assertTrue(firstCommit.exists());
            List<String> firstCommitContents = Files.readAllLines(firstCommit.toPath());
            writer = new FileWriter(TESTDIR +"\\testText0.txt", false);
            writer.write("This is some nice text, yada dabba doo");
            writer.close();
            vcs.add(TESTDIR +"\\testText0.txt");
            vcs.commit("Second Commit", "User");
            File secondCommit = VCSUtils.findHash(Files.readAllLines(headPath).get(0), of).toFile();
            List<String> secondCommitContents = Files.readAllLines(secondCommit.toPath());
            assertEquals(secondCommitContents.get(1), firstCommitHash);
            assertNotEquals(firstCommitContents.get(0), secondCommitContents.get(0));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void removeTest() {  // should print "No changes added to the commit\nNo reason to remove the file"
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer = new FileWriter(TESTDIR +"\\testText.txt");
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(TESTDIR +"\\testText.txt");
            vcs.remove(TESTDIR +"\\testText.txt");
            vcs.commit("Test", "User");
            vcs.add(TESTDIR +"\\testText.txt");
            vcs.commit("Test", "User");
            writer = new FileWriter(TESTDIR +"\\testText.txt");
            writer.write("This is some more nice text");
            writer.close();
            vcs.add(TESTDIR +"\\testText.txt");
            vcs.remove(TESTDIR +"\\testText.txt");
            vcs.commit("Test", "User");
            assertFalse(new File(TESTDIR +"\\testText.txt").exists());
            vcs.remove(TESTDIR +"\\testText.txt");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void logTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer;
            for (int i = 0; i < 5; i++) {
                writer = new FileWriter(TESTDIR +"\\testText" + i +".txt");
                writer.write("This is some nice text, yada" + i);
                writer.close();
                vcs.add(TESTDIR +"\\testText" + i +".txt");
                vcs.commit(""+i, "User");
            }
            System.out.println(vcs.log());
            assertEquals(5, vcs.log().split("===").length-1);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void globalLogTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer;
            for (int i = 0; i < 5; i++) {
                writer = new FileWriter(TESTDIR +"\\testText" + i +".txt");
                writer.write("This is some nice text, yada" + i);
                writer.close();
                vcs.add(TESTDIR +"\\testText" + i +".txt");
                vcs.commit(""+i, "User");
            }
            System.out.println(vcs.globalLog());
            assertEquals(5, vcs.log().split("===").length-1);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void statusTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer;
            for (int i = 0; i < 5; i++) {
                writer = new FileWriter(TESTDIR +"\\testText" + i +".txt");
                writer.write("This is some nice text, yada" + i);
                writer.close();
                vcs.add(TESTDIR +"\\testText" + i +".txt");
                vcs.commit(""+i, "User");
            }
            new File(TESTDIR + "\\Tester").mkdir();
            writer = new FileWriter(TESTDIR +"\\Tester\\testText.txt");
            writer.write("This is some nice text, yada");
            writer.close();
            vcs.add(TESTDIR +"\\Tester\\testText.txt");
            vcs.remove(TESTDIR +"\\testText0.txt");
            new File(TESTDIR +"\\testText1.txt").delete();
            writer = new FileWriter(TESTDIR +"\\testText2.txt", false);
            writer.write("This is some nice text");
            writer.close();
            writer = new FileWriter(TESTDIR +"\\testText3.txt", false);
            writer.write("This is some nice text");
            writer.close();
            vcs.add(TESTDIR +"\\testText3.txt");
            writer = new FileWriter(TESTDIR +"\\testText.txt");
            writer.write("This is some nice text, yada dabba doo");
            writer.close();
            writer = new FileWriter(TESTDIR +"\\testText4.txt", false);
            writer.write("This is some nice text, yada dabba");
            vcs.add(TESTDIR +"\\testText4.txt");
            writer.write("This is some nice text, yada dabba");
            writer.close();
            System.out.println(vcs.status());
            assertEquals(vcs.status(), "=== Branches ===\n*master\n=== Staged Files ===\ntestText3.txt\nTester\\testText.txt\n=== Removed Files ===\ntestText0.txt\n=== Modified Files ===\ntestText2.txt (modified)\ntestText4.txt (modified)\ntestText1.txt (deleted)\n=== Untracked Files ===\ntestText.txt\n");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void checkoutTest1() {
        VersionControlSystem vcs = cleanUp();
        try {
            Path path = Path.of(TESTDIR +"\\testText.txt");
            FileWriter writer = new FileWriter(path.toFile());
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("Commit One", "User");
            writer = new FileWriter(path.toFile());
            writer.write("This is some nice text");
            writer.close();
            vcs.checkout(path.toString(), false);
            String pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is some nice text, yada yada");
            vcs.remove(path.toString());
            vcs.checkout(path.toString(), false);
            pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is some nice text, yada yada");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void checkoutTest2() {
        VersionControlSystem vcs = cleanUp();
        try {
            Path path = Path.of(TESTDIR +"\\testText.txt");
            FileWriter writer = new FileWriter(path.toFile());
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("Commit One", "User");
            writer = new FileWriter(path.toFile());
            writer.write("This is some nice text");
            writer.close();
            vcs.checkout(path.toString(), false);
            String pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is some nice text, yada yada");
            vcs.remove(path.toString());
            vcs.checkout(path.toString(), false);
            pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is some nice text, yada yada");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void checkoutTest3() {
        VersionControlSystem vcs = cleanUp();
        try {
            Path path = Path.of(TESTDIR +"\\testText.txt");
            FileWriter writer = new FileWriter(path.toFile());
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("Commit One", "User");
            writer = new FileWriter(path.toFile());
            writer.write("This is some nice text");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("Commit Two", "User");
            String hash = vcs.getLastCommit().parentCommit().hash;
            vcs.checkout(hash, path.toString());
            String pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is some nice text, yada yada");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void checkoutTest4() {  // this should print "File does not exist in that commit"
        VersionControlSystem vcs = cleanUp();
        try {
            Path path = Path.of(TESTDIR + "\\testText.txt");
            FileWriter writer = new FileWriter(path.toFile());
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(path.toString());
            vcs.checkout(path.toString(), false);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    private static VersionControlSystem cleanUp() {
        File[] files = new File(TESTDIR).listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        VersionControlSystem vcs = VersionControlSystem.init(TESTDIR);
        return vcs;
    }
    public static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
}
