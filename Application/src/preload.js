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

// gives render.js access to these two methods
contextBridge.exposeInMainWorld('electronAPI', {
    changeDir: (arg) => ipcRenderer.send("Messages", generateConcatenation("changeDir") + arg),
    init: (arg) => ipcRenderer.send("Messages", generateConcatenation("init") + arg),
    add: (arg) => ipcRenderer.send("Messages", generateConcatenation("add") + arg),
    commit: (arg) => ipcRenderer.send("Messages", generateConcatenation("commit") + arg),
    remove: (arg) => ipcRenderer.send("Messages", generateConcatenation("remove") + arg),
    log: (arg) => ipcRenderer.send("Messages", generateConcatenation("log") + arg),
    globalLog: (arg) => ipcRenderer.send("Messages", generateConcatenation("globalLog") + arg),
    status: (arg) => ipcRenderer.send("Messages", generateConcatenation("status") + arg),
    checkout: (arg) => ipcRenderer.send("Messages", generateConcatenation("checkout") + arg),
    branch: (arg) => ipcRenderer.send("Messages", generateConcatenation("branch") + arg),
    removeBranch: (arg) => ipcRenderer.send("Messages", generateConcatenation("removeBranch") + arg),
    reset: (arg) => ipcRenderer.send("Messages", generateConcatenation("reset") + arg),
    onGetMessage: (callback) => ipcRenderer.on("Messages", callback),  // starts listening for messages
    onGetError: (callback) => ipcRenderer.on("Error", callback),
    generateConcatenation: (strings) => generateConcatenation(strings),
})