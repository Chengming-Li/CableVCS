// is loaded before web content is rendered
// contextBridge is used to securely expose APIs to the renderer, and ipcRenderer is used to send messages
const { contextBridge, ipcRenderer } = require('electron')

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

function decodeConcatenation(input) {
    let list = [];
    let i = 0;
    while (i < input.length) {
        let j = i;
        while (input.charAt(j) !== '#') j++;
    
        let length = parseInt(input.substring(i, j));
        i = j + 1 + length;
        list.push(input.substring(j + 1, i));
    }
    return list;
}

// gives render.js access to these two methods
contextBridge.exposeInMainWorld('electronAPI', {
    // VCS functions, take in a string or an array of strings as args
    changeDir: (arg) => ipcRenderer.send("Messages", generateConcatenation(["changeDir", generateConcatenation(arg)])),
    init: (arg) => ipcRenderer.send("Messages", generateConcatenation(["init", generateConcatenation(arg)])),
    add: (arg) => ipcRenderer.send("Messages", generateConcatenation(["add", generateConcatenation(arg)])),
    commit: (arg) => ipcRenderer.send("Messages", generateConcatenation(["commit", generateConcatenation(arg)])),
    remove: (arg) => ipcRenderer.send("Messages", generateConcatenation(["remove", generateConcatenation(arg)])),
    log: () => ipcRenderer.send("Messages", generateConcatenation(["log", generateConcatenation("arg")])),
    globalLog: (arg) => ipcRenderer.send("Messages", generateConcatenation(["globalLog", generateConcatenation(arg)])),
    status: (arg) => ipcRenderer.send("Messages", generateConcatenation(["status", generateConcatenation(arg)])),
    checkout: (arg) => ipcRenderer.send("Messages", generateConcatenation(["checkout", generateConcatenation(arg)])),
    branch: () => ipcRenderer.invoke('dialog:newBranch'),
    removeBranch: (arg) => ipcRenderer.send("Messages", generateConcatenation(["removeBranch", generateConcatenation(arg)])),
    reset: (arg) => ipcRenderer.send("Messages", generateConcatenation(["reset", generateConcatenation(arg)])),
    updateStatus: () => ipcRenderer.send("Messages", generateConcatenation(["updateStatus", generateConcatenation(["arg"])])),
    selectFolder: () => ipcRenderer.invoke('dialog:openDirectory'),
    // starts listening for messages
    onGetMessage: (callback) => ipcRenderer.on("Messages", callback), 
    onGetError: (callback) => ipcRenderer.on("Error", callback),
    updateBranch: (callback) => ipcRenderer.on("Branches", callback),
    updateStaged: (callback) => ipcRenderer.on("Staged", callback),
    updateUnstaged: (callback) => ipcRenderer.on("Unstaged", callback),
    updateLog: (callback) => ipcRenderer.on("Log", callback),
    updateTasks: (callback) => ipcRenderer.on("Tasks", callback),
    generateConcatenation: (strings) => generateConcatenation(strings),
    decodeConcatenation: (strings) => decodeConcatenation(strings.slice(0, -2)),
})