// is loaded before web content is rendered
// contextBridge is used to securely expose APIs to the renderer, and ipcRenderer is used to send messages
const { contextBridge, ipcRenderer } = require('electron')

// exposes the window.electronAPI variable, and creating a method called setTitle(title) that sends a message
// this method can be called with window.electronAPI.setTitle(title)
// also creates onUpdateCounter, which opens a listening channel "update-counter"
// Didn't expose the entire ipcRenderer.send() method for security reasons, limiting what messages we can send
contextBridge.exposeInMainWorld('electronAPI', {
    testDialogue: (arg) => ipcRenderer.send("Test", arg),
    onGetMessage: (callback) => ipcRenderer.on('Test', callback)
})