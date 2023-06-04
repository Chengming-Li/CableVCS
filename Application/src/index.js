const { app, BrowserWindow, ipcMain, dialog } = require('electron');
const { spawn } = require('child_process');
const prompt = require('electron-prompt');
const path = require('path')
var mainWindow = null
var vcs;
var closed = false;
const jarPath = "C:\\Users\\malic\\Downloads\\Project\\VersionControlSystem\\App\\out\\artifacts\\App_jar\\App.jar"
var currentDir = [];

// Handle creating/removing shortcuts on Windows when installing/uninstalling.
if (require('electron-squirrel-startup')) {
    app.quit()
}

function callVCSFunction(event, input) {
    vcs.stdin.write(input + "\n");
}

function generateConcatenation(input) {
    if (input.constructor === String) {
        return input.length + '#' + input;
    }
    let encodedString = '';
    for (let str of input) {
        if (Array.isArray(str)) {
            let temp = generateConcatenation(str);
            encodedString += temp.length + '#' + temp;
        } else {
            encodedString += str.length + '#' + str;
        }
    }
    return encodedString;
}

const createWindow = () => {
    vcs = spawn('java', ['-jar', jarPath]);
    // Create the browser window.
    mainWindow = new BrowserWindow({
        autoHideMenuBar: true,
        width: 1080,
        height: 645,
        minWidth: 1080,
        minHeight: 645,
        aspectRatio: 216/129,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
        },
    })

    ipcMain.handle('dialog:openDirectory', async () => {
        const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
            properties: ['openDirectory']
        })
        if (canceled) {
            return ["", ""]
        } else {
            currentDir = [path.basename(filePaths[0]), filePaths[0]]
            mainWindow.webContents.send('Dir', currentDir);
        }
    })

    ipcMain.handle('dialog:newBranch', async () => {
        prompt({
            title: 'New Branch',
            label: 'Branch Name:',
            value: '',
            inputAttrs: {
                type: 'text'
            },
            type: 'input',
            height: 180
        })
        .then((r) => {
            if(r !== null) {
                vcs.stdin.write(generateConcatenation(["branch", generateConcatenation(r)]) + "\n");
            }
        })
        .catch(console.error);
    })

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
            } else if (d.startsWith("Unstaged")) {
                mainWindow.webContents.send('Unstaged', d.substring(8));
            } else if (d.startsWith("Log")) {
                mainWindow.webContents.send('Log', d.substring(3));
            } else if (d.startsWith("Tasks")) { 
                mainWindow.webContents.send('Tasks', d.substring(5));
            } else if (d.startsWith("ERROR:")) {
                mainWindow.webContents.send('Error', d.substring(7));
            } else if (!d.startsWith("IGNORE")) {
                console.log(d);
            }
        }
    }
    );
    vcs.stderr.on('data', (data) => {
        if (!closed) {
            mainWindow.webContents.send('Error', data.toString());
        }
    });

    vcs.on('close', (code) => {
        // Handle Java process exit
        console.log(`Java process exited with code ${code}`);
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

// Quit when all windows are closed, except on macOS. There, it's common
// for applications and their menu bar to stay active until the user quits
// explicitly with Cmd + Q.
app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
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