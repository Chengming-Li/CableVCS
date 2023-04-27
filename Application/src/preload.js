// is loaded before web content is rendered
// contextBridge is used to securely expose APIs to the renderer, and ipcRenderer is used to send messages
const { contextBridge, ipcRenderer } = require('electron')

function generateConcatenation(strings) {
    if (strings.constructor === String) {
        return String.fromCharCode(strings.length) + strings;
    }
    let output = "";
    for (let i = 0; i < strings.length; i++) {
        output += String.fromCharCode(strings[i].length) + strings[i];
    }
    return output
}

function decodeConcatenation(strings) {
    let list = []
    let i = 0;
    let num = 0;
    while (i < strings.length) {
        num = strings.charCodeAt(i);
        list.push(strings.substring(i + 1, i + num + 1));
        i += num + 1;
    }
    return list;
}

// gives render.js access to these two methods
contextBridge.exposeInMainWorld('electronAPI', {
    // VCS functions, take in a string or an array of strings as args
    changeDir: (arg) => ipcRenderer.send("Messages", generateConcatenation("changeDir") + generateConcatenation(arg)),
    init: (arg) => ipcRenderer.send("Messages", generateConcatenation("init") + generateConcatenation(arg)),
    add: (arg) => ipcRenderer.send("Messages", generateConcatenation("add") + generateConcatenation(arg)),
    commit: (arg) => ipcRenderer.send("Messages", generateConcatenation("commit") + generateConcatenation(arg)),
    remove: (arg) => ipcRenderer.send("Messages", generateConcatenation("remove") + generateConcatenation(arg)),
    log: (arg) => ipcRenderer.send("Messages", generateConcatenation("log") + generateConcatenation(arg)),
    globalLog: (arg) => ipcRenderer.send("Messages", generateConcatenation("globalLog") + generateConcatenation(arg)),
    status: (arg) => ipcRenderer.send("Messages", generateConcatenation("status") + generateConcatenation(arg)),
    checkout: (arg) => ipcRenderer.send("Messages", generateConcatenation("checkout") + generateConcatenation(arg)),
    branch: (arg) => ipcRenderer.send("Messages", generateConcatenation("branch") + generateConcatenation(arg)),
    removeBranch: (arg) => ipcRenderer.send("Messages", generateConcatenation("removeBranch") + generateConcatenation(arg)),
    reset: (arg) => ipcRenderer.send("Messages", generateConcatenation("reset") + generateConcatenation(arg)),
    // starts listening for messages
    onGetMessage: (callback) => ipcRenderer.on("Messages", callback), 
    onGetError: (callback) => ipcRenderer.on("Error", callback),
    updateBranch: (callback) => ipcRenderer.on("Branches", callback),
    updateStaged: (callback) => ipcRenderer.on("Staged", callback),
    updateModified: (callback) => ipcRenderer.on("Modified", callback),
    updateUntracked: (callback) => ipcRenderer.on("Untracked", callback),
    updateRemoved: (callback) => ipcRenderer.on("Removed", callback),
    generateConcatenation: (strings) => generateConcatenation(strings),
    decodeConcatenation: (strings) => decodeConcatenation(strings),
})