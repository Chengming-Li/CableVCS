const { app, BrowserWindow, ipcMain } = require('electron')
const { spawn } = require('child_process');
const path = require('path')
var mainWindow = null
const vcs = spawn('java', ['-jar', "C:\\Users\\malic\\Downloads\\Project\\VersionControlSystem\\App\\out\\artifacts\\App_jar\\App.jar"]);
var closed = false;

// Handle creating/removing shortcuts on Windows when installing/uninstalling.
if (require('electron-squirrel-startup')) {
    app.quit()
}

function callVCSFunction(event, input) {
    vcs.stdin.write(input + "\n");
}

const createWindow = () => {
    // Create the browser window.
    mainWindow = new BrowserWindow({
        autoHideMenuBar: true,
        width: 800,
        height: 600,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
        },
    })
    Object.freeze(mainWindow);  // prevents anyone from changing mainWindow's value

    // listen to messages in the Messages channel
    ipcMain.on("Messages", callVCSFunction)

    vcs.stdout.on('data', (data) => {
        // Handle data received from Java process by sending a message with the info into the Messages channel to render.js
        const d = data.toString();
        if (!closed) {
            if (d.startsWith("Branches")) {
                mainWindow.webContents.send('Branches', d.substring(8));
            } else if (d.startsWith("Staged")) {
                mainWindow.webContents.send('Staged', d.substring(6));
            } else if (d.startsWith("Modified")) {
                mainWindow.webContents.send('Modified', d.substring(8));
            } else if (d.startsWith("Untracked")) {
                mainWindow.webContents.send('Untracked', d.substring(9));
            } else if (d.startsWith("Removed")) {
                mainWindow.webContents.send('Removed', d.substring(7));
            } else {
                mainWindow.webContents.send('Messages', d);
            }
        }
    }
    );
    vcs.stderr.on('data', (data) => {
        if (!closed) {
            mainWindow.webContents.send('Error', data.toString());
            console.log(data.toString());
        }
    });

    vcs.on('close', (code) => {
        // Handle Java process exit
        console.log(`Java process exited with code ${code}`);
        clearInterval(intervalID);
    });

    // and load the index.html of the app.
    mainWindow.loadFile(path.join(__dirname, 'index.html'))
    
    // Open the DevTools.
    mainWindow.webContents.openDevTools()
}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', createWindow)

const intervalID = setInterval(function() {
    callVCSFunction(null, String.fromCharCode(6) + "update");
}, 1000);

// Quit when all windows are closed, except on macOS. There, it's common
// for applications and their menu bar to stay active until the user quits
// explicitly with Cmd + Q.
app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        clearInterval(intervalID);
        closed = true;
        app.quit()
    }
})

app.on('activate', () => {
    // On OS X it's common to re-create a window in the app when the
    // dock icon is clicked and there are no other windows open.
    if (BrowserWindow.getAllWindows().length === 0) {
        createWindow()
    }
})