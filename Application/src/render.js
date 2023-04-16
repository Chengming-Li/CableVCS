const buttonOne = document.getElementById('buttonID');

// adds a listener to title that'll call the code inside
buttonOne.addEventListener('click', async () => {
    const title = await window.electronAPI.testDialogue("HALLO")
    console.log(title)
});

/*import { spawn } from 'child_process';
const javaProcess = spawn('java', ['-jar', "C:\\Users\\malic\\Downloads\\Project\\VersionControlSystem\\App\\out\\artifacts\\App_jar\\App.jar"]);

// Handle data/events from Java process
javaProcess.stdout.on('data', (data) => {
    // Handle data received from Java process
    console.log(`Data from Java: ${data.toString()}`);
  }
);
javaProcess.stderr.on('data', (data) => {
    // Handle error data received from Java process
    console.error(`Error from Java: ${data.toString()}`);
});
javaProcess.on('close', (code) => {
    // Handle Java process exit
    console.log(`Java process exited with code ${code}`);
});
async function getVS() {
    javaProcess.stdin.write('Hello from Electron!\n');
}
*/