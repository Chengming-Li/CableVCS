import versioncontrolsystem.VCSUtils;
import versioncontrolsystem.VersionControlSystem;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

class VersionControlSystemTest {
    private static final String TESTDIR = "C:\\Users\\malic\\Downloads\\Project\\VersionControlSystem\\Test";
    private static final String VCSDIR = TESTDIR + "\\.vcs";

    @org.junit.jupiter.api.Test
    void init() {
        VersionControlSystem vcs = cleanUp();
        assertTrue(new File(VCSDIR).exists());
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
    void commitTest2() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer;
            for (int i = 0; i < 5; i++) {
                writer = new FileWriter(TESTDIR +"\\testText" + i +".txt");
                writer.write("This is some nice text, yada" + i);
                writer.close();
                vcs.add(TESTDIR +"\\testText" + i +".txt");
            }
            vcs.commit("commit", "user", new String[0], new String[] {"Close this task"});
            vcs = new VersionControlSystem(TESTDIR);
            writer = new FileWriter(TESTDIR +"\\testText.txt");
            writer.write("This is some nice text, yada");
            writer.close();
            vcs.add(TESTDIR +"\\testText.txt");
            VersionControlSystem finalVcs = vcs;
            Exception e = assertThrows(Exception.class, () -> finalVcs.commit("Closed", "user", null, new String[] {"Close this task"}));
            assertEquals(e.getMessage(), "Task \"Close this task\" already exists");
            finalVcs.commit("Closed", "user", new String[] {"Close this task"}, new String[] {"New Task"});
            assertTrue((!new File(VCSDIR + "\\Tasks\\Close this task").exists() && new File(VCSDIR + "\\Tasks\\New Task").exists()));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void removeTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            FileWriter writer = new FileWriter(TESTDIR +"\\testText.txt");
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(TESTDIR +"\\testText.txt");
            vcs.remove(TESTDIR +"\\testText.txt");
            Exception exception = assertThrows(Exception.class,
                    () -> vcs.commit("Test", "User"));
            assertEquals(exception.getMessage(), "No changes added to commit");
            vcs.add(TESTDIR +"\\testText.txt");
            vcs.commit("Test", "User");
            writer = new FileWriter(TESTDIR +"\\testText.txt");
            writer.write("This is some more nice text");
            writer.close();
            vcs.add(TESTDIR +"\\testText.txt");
            vcs.remove(TESTDIR +"\\testText.txt");
            vcs.commit("Test", "User");
            assertFalse(new File(TESTDIR +"\\testText.txt").exists());
            Exception e = assertThrows(Exception.class,
                    () -> vcs.remove(TESTDIR +"\\testText.txt"));
            assertEquals(e.getMessage(), "No reason to remove file");
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
            vcs.branch("Branch");
            vcs.checkout("Branch", true);
            vcs.remove(TESTDIR +"\\testText0.txt");
            vcs.commit("Branch", "User");
            System.out.println(vcs.log());
            vcs.checkout("master", true);
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
            vcs.branch("Branch");
            vcs.checkout("Branch", true);
            vcs.remove(TESTDIR +"\\testText0.txt");
            vcs.commit("Branch", "User");
            System.out.println(vcs.globalLog());
            vcs.checkout("master", true);
            assertEquals(6, vcs.globalLog().split("===").length-1);
            vcs = new VersionControlSystem(TESTDIR);
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
            vcs.branch("branch");
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
            assertEquals(vcs.status(), "=== Branches ===\n*master\nbranch\n=== Staged Files ===\ntestText3.txt\nTester\\testText.txt\n=== Removed Files ===\ntestText0.txt\n=== Modified Files ===\ntestText1.txt (deleted) \ntestText2.txt (modified)\ntestText4.txt (modified)\n=== Untracked Files ===\ntestText.txt\n");
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
    void checkoutTest4() {
        VersionControlSystem vcs = cleanUp();
        try {
            Path path = Path.of(TESTDIR + "\\testText.txt");
            FileWriter writer = new FileWriter(path.toFile());
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(path.toString());
            Exception e = assertThrows(Exception.class, () -> vcs.checkout(path.toString(), false));
            assertEquals(e.getMessage(), "File does not exist in that commit");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void checkoutTest5() {
        VersionControlSystem vcs = cleanUp();
        try {
            Path path = Path.of(TESTDIR + "\\testText.txt");
            FileWriter writer = new FileWriter(path.toFile());
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("First Commit", "User");
            vcs.branch("Branch");
            writer = new FileWriter(path.toFile());
            writer.write("This is still the master branch");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("Second Commit", "User");
            vcs.checkout("Branch", true);
            String pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is some nice text, yada yada");
            writer = new FileWriter(path.toFile());
            writer.write("This is the new branch");
            writer.close();
            Path otherPath = Path.of(TESTDIR + "\\testText1.txt");
            writer = new FileWriter(otherPath.toFile());
            writer.write("This is not the burner file");
            writer.close();
            vcs.add(path.toString());
            vcs.add(otherPath.toString());
            vcs.commit("Third Commit", "User");
            vcs.checkout("master", true);
            pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is still the master branch");
            assertFalse(otherPath.toFile().exists());
            writer = new FileWriter(otherPath.toFile());
            writer.write("This is THE burner file");
            writer.close();
            Exception e = assertThrows(Exception.class, () -> vcs.checkout("Branch", true));
            assertEquals(e.getMessage(), "There are untracked files in the way; delete it or add it first.\ntestText1.txt\n");
            otherPath.toFile().delete();
            vcs.checkout("Branch", true);
            pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is the new branch");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void checkoutTest6() {
        VersionControlSystem vcs = cleanUp();
        try {
            Path path = Path.of(TESTDIR + "\\testText.txt");
            FileWriter writer = new FileWriter(path.toFile());
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("First Commit", "User", null, new String[]{"One", "Two"});
            vcs.branch("Branch");
            writer = new FileWriter(path.toFile());
            writer.write("This is still the master branch");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("Second Commit", "User", new String[]{"One"}, new String[] {"Four"});
            assertTrue(!new File(VCSDIR+"\\Tasks\\One").exists() && new File(VCSDIR + "\\Tasks\\Two").exists() && new File(VCSDIR + "\\Tasks\\Four").exists());
            vcs.checkout("Branch", true);
            assertTrue(new File(VCSDIR+"\\Tasks\\One").exists() && new File(VCSDIR + "\\Tasks\\Two").exists());
            writer = new FileWriter(path.toFile());
            writer.write("This is the new branch");
            writer.close();
            Path otherPath = Path.of(TESTDIR + "\\testText1.txt");
            writer = new FileWriter(otherPath.toFile());
            writer.write("This is not the burner file");
            writer.close();
            vcs.add(path.toString());
            vcs.add(otherPath.toString());
            vcs.commit("Third Commit", "User", new String[]{"One", "Two"}, null);
            assertTrue(!new File(VCSDIR+"\\Tasks\\One").exists() && !new File(VCSDIR + "\\Tasks\\Two").exists());
            vcs.checkout("master", true);
            System.out.println(vcs.globalLog());
            assertTrue(!new File(VCSDIR+"\\Tasks\\One").exists() && new File(VCSDIR + "\\Tasks\\Two").exists() && new File(VCSDIR + "\\Tasks\\Four").exists());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void branchTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            Path path = Path.of(TESTDIR + "\\testText.txt");
            FileWriter writer = new FileWriter(path.toFile());
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("First Commit", "User");
            vcs.branch("Branch");
            writer = new FileWriter(path.toFile());
            writer.write("This is still the master branch");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("Second Commit", "User");
            vcs.checkout("Branch", true);
            String pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is some nice text, yada yada");
            writer = new FileWriter(path.toFile());
            writer.write("This is the new branch");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("Third Commit", "User");
            vcs.checkout("master", true);
            pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is still the master branch");
            vcs.checkout("Branch", true);
            pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is the new branch");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    @org.junit.jupiter.api.Test
    void removeBranchTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            Path path = Path.of(TESTDIR + "\\testText.txt");
            FileWriter writer = new FileWriter(path.toFile());
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("First Commit", "User");
            vcs.branch("Branch");
            vcs.checkout("Branch", true);
            vcs.removeBranch("master");
            assertFalse((new File(path.resolve(".vcs").resolve("Branches").resolve("master").toString())).exists());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @org.junit.jupiter.api.Test
    void resetTest() {
        VersionControlSystem vcs = cleanUp();
        try {
            Path path = Path.of(TESTDIR + "\\testText.txt");
            FileWriter writer = new FileWriter(path.toFile());
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("First Commit", "User");
            writer = new FileWriter(path.toFile());
            writer.write("This is still the master branch");
            writer.close();
            Path otherPath = Path.of(TESTDIR + "\\testText1.txt");
            writer = new FileWriter(otherPath.toFile());
            writer.write("This is not the burner file");
            writer.close();
            vcs.add(path.toString());
            vcs.add(otherPath.toString());
            vcs.commit("Second Commit", "User");
            writer = new FileWriter(path.toFile());
            writer.write("This is stilll the master branch");
            writer.close();
            vcs.add(path.toString());
            vcs.remove(otherPath.toString());
            vcs.commit("Third Commit", "User");
            assertFalse(otherPath.toFile().exists());
            writer = new FileWriter(otherPath.toFile());
            writer.write("This is THE burner file");
            writer.close();
            String hash = vcs.getLastCommit().parentCommit().hash;
            Exception e = assertThrows(Exception.class, () -> vcs.reset(hash));
            assertEquals(e.getMessage(), "There are untracked files in the way; delete it or add it first.\ntestText1.txt\n");
            otherPath.toFile().delete();
            vcs.reset(hash);
            String pathContents = Files.readAllLines(path).get(0);
            assertEquals(pathContents, "This is still the master branch");
            writer = new FileWriter(path.toFile());
            writer.write("This is stilll the master branch");
            writer.close();
            vcs.reset();
            assertEquals(pathContents, "This is still the master branch");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @org.junit.jupiter.api.Test
    void UITest() {
        try {
            VersionControlSystem vcs = cleanUp();
            Path path = Path.of(TESTDIR + "\\test Text.txt");
            FileWriter writer = new FileWriter(path.toFile());
            writer.write("This is some nice text, yada yada");
            writer.close();
            vcs.add(path.toString());
            vcs.commit("First Commit", "User", null, new String[] {"Hello", "There"});
            vcs = new VersionControlSystem(TESTDIR);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
    private static VersionControlSystem cleanUp() {
        try {
            File[] files = new File(TESTDIR).listFiles();
            if (files == null) {
                return null;
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
            VersionControlSystem vcs = VersionControlSystem.init(TESTDIR);
            return vcs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
