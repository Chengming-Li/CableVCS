const buttonOne = document.getElementById('buttonID');
const { spawn } = require('child_process');
buttonOne.onclick = getVS;
const javaProcess = spawn('java', ['-jar', 'path/to/your/java.jar']);

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