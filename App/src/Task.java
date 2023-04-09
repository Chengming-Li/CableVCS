import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class Task extends VCSUtils {
    private Path opened;
    private Path closed;

    public Task(Path vcs) {
        vcs = vcs.resolve("Tasks");
        this.opened = vcs.resolve("Opened");
        this.closed = vcs.resolve("Closed");
    }

    /**
     * Creates a new file in the opened folder
     * @param name: name of the new file
     * @param description: contents of the new file
     */
    public void createTask(String name, String description) throws Exception {
        FileWriter writer = new FileWriter(this.opened.resolve(name).toFile(), false);
        writer.write(description);
        writer.close();
    }

    /**
     * Reopens a task
     * @param name: name of closed task
     */
    public void reopenTask(String name) throws Exception {
        if (taskExists(name, true)) {
            throw new Exception("Closed task does not exist");
        } else {
            Path destination = this.opened.resolve(name);
            Path source = this.closed.resolve(name);
            moveTask(source, destination);
        }
    }

    /**
     * Closes a task
     * @param name: task to close
     */
    public void closeTask(String name) throws Exception {
        if (taskExists(name, false)) {
            throw new Exception("Task does not exist");
        } else {
            Path destination = this.closed.resolve(name);
            Path source = this.opened.resolve(name);
            moveTask(source, destination);
        }
    }

    /**
     * Checks if the task exists
     * @param name: name of the task
     * @param closed: if the task is closed or not
     * @return true if task exists, false otherwise
     */
    public boolean taskExists(String name, boolean closed) {
        File f;
        if (closed) {
            f = this.closed.resolve(name).toFile();
        } else {
            f = this.opened.resolve(name).toFile();
        }
        return f.exists();
    }

    /**
     * Moves a task from the source to the destination
     * @param source: current path of file
     * @param destination: new path of file
     */
    private void moveTask(Path source, Path destination) throws Exception {
        Files.move(source, destination);
    }
}
