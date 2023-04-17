const buttonOne = document.getElementById('buttonID');

// handles messages received by render.js
window.electronAPI.onGetMessage((event, value) => {
    console.log(value);
})
// handles errors received by render.js
window.electronAPI.onGetError((event, value) => {
    alert("ERROR: " + value);
})

// adds a listener to button that'll call the code inside
buttonOne.addEventListener('click', () => {
    window.electronAPI.init(window.electronAPI.generateConcatenation(["C:\\Users\\malic\\Downloads\\Test"]))
})